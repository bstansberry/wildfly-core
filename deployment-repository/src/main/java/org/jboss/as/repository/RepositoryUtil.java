/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.repository;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.as.repository.logging.DeploymentRepositoryLogger;

/**
 * Utilities related to dealing with content in a content repository.
 *
 * @author Brian Stansberry
 */
final class RepositoryUtil {

    static final String CONTENT = "content";
    static final String CHILDREN = "children";
    static final String EMPTY_DIR = "children";

    static final List<String> CONTENT_TYPES = Arrays.asList(CONTENT, CHILDREN, EMPTY_DIR);

    static final InputStream NULL_STREAM = new InputStream() {

        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    static Path getContentFile(Path repoRoot, byte[] deploymentHash, boolean validate) {
        return getContentHashDir(repoRoot, deploymentHash, validate).resolve(CONTENT);
    }

    static Path getContentFile(Path repoRoot, String deploymentHexHash, boolean validate) {
        return getContentHashDir(repoRoot, deploymentHexHash, validate).resolve(CONTENT);
    }

    static Path getContentFile(Path repoRoot, String deploymentHexHash, String contentFile) {
        return getContentHashDir(repoRoot, deploymentHexHash, true).resolve(contentFile);
    }

    static Path getContentHashDir(Path repoRoot, final byte[] deploymentHash, final boolean validate) {
        return getContentHashDir(repoRoot, HashUtil.bytesToHexString(deploymentHash), validate);
    }

    static Path getContentHashDir(Path repoRoot, final String sha1, final boolean validate) {
        final String partA = sha1.substring(0, 2);
        final String partB = sha1.substring(2);
        final Path base = repoRoot.resolve(partA);
        if (validate) {
            validateDir(base);
        }
        final Path hashDir = base.resolve(partB);
        if (validate && !Files.exists(hashDir)) {
            try {
                Files.createDirectories(hashDir);
            } catch (IOException ioex) {
                throw DeploymentRepositoryLogger.ROOT_LOGGER.cannotCreateDirectory(ioex, hashDir.toAbsolutePath().toString());
            }
        }
        return hashDir;
    }

    private static void validateDir(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ioex) {
                throw DeploymentRepositoryLogger.ROOT_LOGGER.cannotCreateDirectory(ioex, dir.toAbsolutePath().toString());
            }
        } else if (!Files.isDirectory(dir)) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.notADirectory(dir.toAbsolutePath().toString());
        } else if (!dir.toFile().canWrite()) { //WFCORE-799 workaround for broken Files.isWritable() on Windows in JDK8
            throw DeploymentRepositoryLogger.ROOT_LOGGER.directoryNotWritable(dir.toAbsolutePath().toString());
        }
    }

    static void moveTempToPermanent(Path tmpFile, Path permanentFile) throws IOException {
        Path localTmp = permanentFile.resolveSibling("tmp");
        try {
            Files.move(tmpFile, permanentFile);
        } catch (IOException ioex) {
            // AS7-3574. Try to avoid writing the permanent file bit by bit in we crash in the middle.
            // Copy tmpFile to another tmpfile in the same dir as the permanent file (and thus same filesystem)
            // and see then if we can rename it.
            Files.copy(tmpFile, localTmp);
            try {
                Files.move(localTmp, permanentFile);
            } catch (IOException ex) {
                // No luck; need to copy
                try {
                    Files.copy(localTmp, permanentFile);
                } catch (IOException e) {
                    Files.deleteIfExists(permanentFile);
                    throw e;
                }
            }
        } finally {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException ioex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteTempFile(ioex, tmpFile.toString());
                tmpFile.toFile().deleteOnExit();
            }
            try {
                Files.deleteIfExists(localTmp);
            } catch (IOException ioex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteTempFile(ioex, localTmp.toString());
                localTmp.toFile().deleteOnExit();
            }
        }
    }

    static boolean removeContent(Path repoRoot, String hexHash, String contentFile) {

        Path file = getContentFile(repoRoot, hexHash, contentFile);
        try {
            if (Files.deleteIfExists(file)) {
                DeploymentRepositoryLogger.ROOT_LOGGER.contentRemoved(file.toAbsolutePath().toString());
            }
        } catch (IOException ex) {
            DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, file.toString());
        }

        // If the content file is gone now or never existed, the content item is removed; the rest is
        // housekeeping
        boolean result = !file.toFile().exists();

        if (result) {

            // Try and remove parent dir and grandparent

            Path parent = file.getParent();

            // First, if the content file had some misc siblings (e.g. .DS_store on OSX)
            // then try and remove those so we can remove the parent
            // TODO ContentRepositoryTest.testNotEmptyDir() seems to be explicitly
            // rejecting this behavior, for no good reason that I'm aware of but
            // maybe I've forgotten something. But disabled for now
//            try (Stream<Path> files = Files.list(parent)) {
//                if (!files.allMatch(path -> path.toFile().delete())) {
//                    DeploymentRepositoryLogger.ROOT_LOGGER.debugf("Cannot delete some children of %s", parent);
//                }
//            } catch (IOException ex) {
//                DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, parent.toString());
//            }

            try {
                Files.deleteIfExists(parent);
            } catch (IOException ex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, parent.toString());
            }

            Path grandParent = parent.getParent();
            try (Stream<Path> files = Files.list(grandParent)) {
                if (!files.findAny().isPresent()) {
                    Files.deleteIfExists(grandParent);
                }
            } catch (IOException ex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, grandParent.toString());
            }
        }
        return result;
    }

    static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void safeClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (RuntimeException | IOException e) {
            // ignored
        }
    }

    static void safeDelete(Path path) {
        try {
            Files.delete(path);
        } catch (RuntimeException | IOException e) {
            // ignored
        }
    }

    private RepositoryUtil() {
    }
}
