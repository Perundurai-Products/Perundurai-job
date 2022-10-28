/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.language.swift.internal;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.language.swift.SwiftSharedLibrary;

import javax.inject.Inject;

public class DefaultSwiftSharedLibrary extends DefaultSwiftBinary implements SwiftSharedLibrary {
    @Inject
    public DefaultSwiftSharedLibrary(String name, ObjectFactory objectFactory, Provider<String> module, boolean debuggable, FileCollection source, ConfigurationContainer configurations, Configuration implementation) {
        super(name, objectFactory, module, debuggable, source, configurations, implementation);
    }
}