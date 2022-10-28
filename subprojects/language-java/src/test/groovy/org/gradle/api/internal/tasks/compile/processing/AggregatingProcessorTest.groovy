/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.internal.tasks.compile.processing

import org.gradle.api.internal.tasks.compile.incremental.processing.AnnotationProcessingResult
import spock.lang.Specification

import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class AggregatingProcessorTest extends Specification {

    Set<TypeElement> annotationTypes = [
        annotation("Helper"),
        annotation("Service")
    ] as Set

    RoundEnvironment roundEnvironment = Stub(RoundEnvironment) {
        getRootElements() >> ([type("A"), type("B"), type("C")] as Set)
        getElementsAnnotatedWith(_ as TypeElement) >> { TypeElement annotationType ->
            if (annotationType.is(annotationTypes[0])) {
                [type("A")] as Set
            } else if (annotationType.is(annotationTypes[1])) {
                [type("B")] as Set
            }
        }
    }

    AnnotationProcessingResult result = new AnnotationProcessingResult()
    Processor delegate = Stub(Processor)
    Messager messager = Mock(Messager)
    AggregatingProcessor processor = new AggregatingProcessor(delegate, result)

    def setup() {
        processor.init(Stub(ProcessingEnvironment) {
            getMessager() >> messager
        })
    }

    def "when delegate reacts to any class, all root elements are aggregated"() {
        given:
        delegate.getSupportedAnnotationTypes() >> ["*"]

        when:
        processor.process(annotationTypes, roundEnvironment)

        then:
        result.getAggregatedTypes() == ["A", "B", "C"] as Set
    }

    def "when delegate reacts to specific annotations, only types annotated with those are aggregated"() {
        given:
        delegate.getSupportedAnnotationTypes() >> annotationTypes.collect { it.getQualifiedName().toString() }

        when:
        processor.process(annotationTypes, roundEnvironment)

        then:
        result.getAggregatedTypes() == ["A", "B"] as Set
    }

    def "aggregating processors do not work with source retention annotations"() {
        given:
        def sourceRetentionAnnotation = annotation("Broken", RetentionPolicy.SOURCE)

        when:
        processor.process([sourceRetentionAnnotation] as Set, roundEnvironment)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { CharSequence message -> message.contains("'@Broken' has source retention.") })
    }


    TypeElement annotation(String name, RetentionPolicy retentionPolicy = RetentionPolicy.CLASS) {
        Stub(TypeElement) {
            getEnclosingElement() >> null
            getQualifiedName() >> Stub(Name) {
                toString() >> name
            }
            getSimpleName() >> Stub(Name) {
                toString() >> name
            }
            getAnnotation(Retention) >> Stub(Retention) {
                value() >> retentionPolicy
            }
        }
    }

    TypeElement type(String name) {
        Stub(TypeElement) {
            getEnclosingElement() >> null
            getQualifiedName() >> Stub(Name) {
                toString() >> name
            }
        }
    }

}
