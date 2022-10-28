/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.dsl;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.internal.artifacts.Module;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.publish.DefaultConfigurablePublishArtifact;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.internal.tasks.TaskResolver;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.internal.Factory;
import org.gradle.internal.exceptions.DiagnosticsVisitor;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.typeconversion.MapKey;
import org.gradle.internal.typeconversion.MapNotationConverter;
import org.gradle.internal.typeconversion.NotationParser;
import org.gradle.internal.typeconversion.NotationParserBuilder;
import org.gradle.internal.typeconversion.TypedNotationConverter;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.concurrent.Callable;

public class PublishArtifactNotationParserFactory implements Factory<NotationParser<Object, ConfigurablePublishArtifact>> {
    private final Instantiator instantiator;
    private final DependencyMetaDataProvider metaDataProvider;
    private final TaskResolver taskResolver;
    private final ObjectFactory objectFactory;
    private final ProviderFactory providerFactory;

    public PublishArtifactNotationParserFactory(Instantiator instantiator, DependencyMetaDataProvider metaDataProvider, TaskResolver taskResolver, ObjectFactory objectFactory, ProviderFactory providerFactory) {
        this.instantiator = instantiator;
        this.metaDataProvider = metaDataProvider;
        this.taskResolver = taskResolver;
        this.objectFactory = objectFactory;
        this.providerFactory = providerFactory;
    }

    public NotationParser<Object, ConfigurablePublishArtifact> create() {
        FileNotationConverter fileConverter = new FileNotationConverter();
        return NotationParserBuilder
                .toType(ConfigurablePublishArtifact.class)
                .converter(new DecoratingConverter())
                .converter(new ArchiveTaskNotationConverter())
                .converter(new FileProviderNotationConverter())
                .converter(new FileSystemLocationNotationConverter())
                .converter(fileConverter)
                .converter(new FileMapNotationConverter(fileConverter))
                .toComposite();
    }

    private class DecoratingConverter extends TypedNotationConverter<PublishArtifact, ConfigurablePublishArtifact> {
        private DecoratingConverter() {
            super(PublishArtifact.class);
        }

        @Override
        protected ConfigurablePublishArtifact parseType(PublishArtifact notation) {
            // TODO: Introduce providers in PublishArtifact
            ConfigurablePublishArtifact configurablePublishArtifact = instantiator.newInstance(DefaultConfigurablePublishArtifact.class, objectFactory, taskResolver, Providers.of(new FileSystemLocation() {
                @Override
                public File getAsFile() {
                    return notation.getFile();
                }
            }));

            configurablePublishArtifact.getArtifactName().set(providerFactory.provider(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return notation.getName();
                }
            }));
            configurablePublishArtifact.getArtifactClassifier().set(providerFactory.provider(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return notation.getClassifier();
                }
            }));
            configurablePublishArtifact.getArtifactType().set(providerFactory.provider(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return notation.getType();
                }
            }));
            configurablePublishArtifact.getArtifactExtension().set(providerFactory.provider(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return notation.getExtension();
                }
            }));
            configurablePublishArtifact.builtBy(notation.getBuildDependencies());

            return configurablePublishArtifact;
        }
    }

    private class ArchiveTaskNotationConverter extends TypedNotationConverter<AbstractArchiveTask, ConfigurablePublishArtifact> {
        private ArchiveTaskNotationConverter() {
            super(AbstractArchiveTask.class);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of AbstractArchiveTask").example("jar");
        }

        @Override
        protected ConfigurablePublishArtifact parseType(AbstractArchiveTask archiveTask) {
            DefaultConfigurablePublishArtifact configurablePublishArtifact = objectFactory.newInstance(DefaultConfigurablePublishArtifact.class, objectFactory, taskResolver, archiveTask.getArchiveFile());
            configurablePublishArtifact.configureFor(Providers.of(archiveTask));
            return configurablePublishArtifact;
        }
    }

    private static class FileMapNotationConverter extends MapNotationConverter<ConfigurablePublishArtifact> {
        private final FileNotationConverter fileConverter;

        private FileMapNotationConverter(FileNotationConverter fileConverter) {
            this.fileConverter = fileConverter;
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Maps with 'file' key");
        }

        protected PublishArtifact parseMap(@MapKey("file") File file) {
            return fileConverter.parseType(file);
        }
    }

    private class FileProviderNotationConverter extends TypedNotationConverter<Provider, ConfigurablePublishArtifact> {
        FileProviderNotationConverter() {
            super(Provider.class);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of Provider<RegularFile>.");
            visitor.candidate("Instances of Provider<Directory>.");
            visitor.candidate("Instances of Provider<File>.");
        }

        @Override
        protected ConfigurablePublishArtifact parseType(Provider notation) {
            Module module = metaDataProvider.getModule();
            // Don't know what kind of Provider this is
            // Try to convert it into a FileSystemLocation
            final Provider<FileSystemLocation> file = notation.map(new Transformer<FileSystemLocation, Object>() {
                @Override
                public FileSystemLocation transform(Object value) {
                    if (value instanceof FileSystemLocation) {
                        return (FileSystemLocation) value;
                    } else if (value instanceof File) {
                        return new FileSystemLocation() {
                            @Override
                            public File getAsFile() {
                                return (File) value;
                            }
                        };
                    } else if (value instanceof AbstractArchiveTask) {
                        // TODO: Deprecate this behavior?
                        // This used to work for some builds (e.g., Android), even though it wasn't documented as something we supported.
                        return ((AbstractArchiveTask)value).getArchiveFile().get();
                    } else {
                        throw new InvalidUserDataException(String.format("Cannot convert provided value (%s) to a file.", value));
                    }
                }
            });
            
            Provider<ArtifactFile> artifactFile = file.map(new Transformer<ArtifactFile, FileSystemLocation>() {
                @Override
                public ArtifactFile transform(FileSystemLocation value) {
                    return new ArtifactFile(value.getAsFile(), module.getVersion());
                }
            });
            DefaultConfigurablePublishArtifact configurablePublishArtifact = objectFactory.newInstance(DefaultConfigurablePublishArtifact.class, objectFactory, taskResolver, file);
            configurablePublishArtifact.getArtifactName().set(artifactFile.map(new Transformer<String, ArtifactFile>() {
                @Override
                public String transform(ArtifactFile artifactFile) {
                    return artifactFile.getName();
                }
            }));
            configurablePublishArtifact.getArtifactExtension().set(artifactFile.map(new Transformer<String, ArtifactFile>() {
                @Override
                public String transform(ArtifactFile artifactFile) {
                    return artifactFile.getExtension();
                }
            }));
            configurablePublishArtifact.getArtifactClassifier().set(artifactFile.map(new Transformer<String, ArtifactFile>() {
                @Override
                public String transform(ArtifactFile artifactFile) {
                    return GUtil.elvis(artifactFile.getClassifier(), "");
                }
            }));
            configurablePublishArtifact.getArtifactType().set(configurablePublishArtifact.getArtifactExtension());

            configurablePublishArtifact.builtBy(notation);
            return configurablePublishArtifact;
        }
    }

    private class FileSystemLocationNotationConverter extends TypedNotationConverter<FileSystemLocation, ConfigurablePublishArtifact> {
        FileSystemLocationNotationConverter() {
            super(FileSystemLocation.class);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of RegularFile.");
            visitor.candidate("Instances of Directory.");
        }

        @Override
        protected ConfigurablePublishArtifact parseType(FileSystemLocation notation) {
            Module module = metaDataProvider.getModule();
            ArtifactFile artifactFile = new ArtifactFile(notation.getAsFile(), module.getVersion());

            DefaultConfigurablePublishArtifact configurablePublishArtifact = objectFactory.newInstance(DefaultConfigurablePublishArtifact.class, objectFactory, taskResolver, Providers.of(notation));
            configurablePublishArtifact.getArtifactName().set(artifactFile.getName());
            configurablePublishArtifact.getArtifactExtension().set(artifactFile.getExtension());
            configurablePublishArtifact.getArtifactType().set(configurablePublishArtifact.getArtifactExtension());
            configurablePublishArtifact.getArtifactClassifier().set(artifactFile.getClassifier());
            return configurablePublishArtifact;

        }
    }

    private class FileNotationConverter extends TypedNotationConverter<File, ConfigurablePublishArtifact> {
        private FileNotationConverter() {
            super(File.class);
        }

        @Override
        protected ConfigurablePublishArtifact parseType(File notation) {
            Module module = metaDataProvider.getModule();

            ArtifactFile artifactFile = new ArtifactFile(notation, module.getVersion());
            DefaultConfigurablePublishArtifact configurablePublishArtifact = objectFactory.newInstance(DefaultConfigurablePublishArtifact.class, objectFactory, taskResolver, Providers.of(new FileSystemLocation() {
                @Override
                public File getAsFile() {
                    return notation;
                }
            }));
            configurablePublishArtifact.getArtifactName().set(artifactFile.getName());
            configurablePublishArtifact.getArtifactExtension().set(artifactFile.getExtension());
            configurablePublishArtifact.getArtifactType().set(artifactFile.getExtension());
            configurablePublishArtifact.getArtifactClassifier().set(artifactFile.getClassifier());

            return configurablePublishArtifact;
        }
    }
}
