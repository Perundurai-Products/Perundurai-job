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

package org.gradle.api.internal.notations;

import com.google.common.collect.Interner;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyConstraint;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.typeconversion.NotationParser;
import org.gradle.internal.typeconversion.NotationParserBuilder;

public class DependencyConstraintNotationParser {
    public static NotationParser<Object, DependencyConstraint> parser(Instantiator instantiator, Interner<String> stringInterner) {
        return NotationParserBuilder
            .toType(DependencyConstraint.class)
            .fromCharSequence(new DependencyStringNotationConverter<DefaultDependencyConstraint>(instantiator, DefaultDependencyConstraint.class, stringInterner))
            .converter(new DependencyMapNotationConverter<DefaultDependencyConstraint>(instantiator, DefaultDependencyConstraint.class))
            .invalidNotationMessage("Comprehensive documentation on dependency notations is available in DSL reference for DependencyHandler type.")
            .toComposite();
    }
}
