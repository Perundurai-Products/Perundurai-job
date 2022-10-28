/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.changedetection.changes;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RebuildIncrementalTaskInputs extends StatefulIncrementalTaskInputs {
    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildIncrementalTaskInputs.class);

    private final Iterable<? extends FileCollectionFingerprint> fingerprints;

    public RebuildIncrementalTaskInputs(Task task, Iterable<? extends FileCollectionFingerprint> fingerprints) {
        LOGGER.info("All input files are considered out-of-date for incremental {}.", task);
        this.fingerprints = fingerprints;
    }

    public boolean isIncremental() {
        return false;
    }

    public void doOutOfDate(final Action<? super InputFileDetails> outOfDateAction) {
        for (FileCollectionFingerprint fingerprint : fingerprints) {
            for (String path : fingerprint.getFingerprints().keySet()) {
                outOfDateAction.execute(new RebuildInputFile(new File(path)));
            }
        }
    }

    public void doRemoved(Action<? super InputFileDetails> removedAction) {
    }

    private static class RebuildInputFile implements InputFileDetails {
        private final File file;

        private RebuildInputFile(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public boolean isAdded() {
            return false;
        }

        public boolean isModified() {
            return false;
        }

        public boolean isRemoved() {
            return false;
        }
    }
}
