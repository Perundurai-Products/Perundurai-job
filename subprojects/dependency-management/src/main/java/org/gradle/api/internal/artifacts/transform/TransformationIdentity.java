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

package org.gradle.api.internal.artifacts.transform;

import org.gradle.api.internal.project.WorkIdentity;

import java.util.concurrent.atomic.AtomicLong;

public final class TransformationIdentity implements WorkIdentity {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final long id;

    private TransformationIdentity(long id) {
        this.id = id;
    }

    public static TransformationIdentity create() {
        return new TransformationIdentity(SEQUENCE.getAndIncrement());
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransformationIdentity that = (TransformationIdentity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "TransformationIdentity{id=" + id + '}';
    }

}
