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

import static org.jboss.as.repository.RepositoryUtil.getContentFile;
import static org.jboss.as.repository.RepositoryUtil.moveTempToPermanent;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.as.repository.logging.DeploymentRepositoryLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Default implementation of {@link ContentRepository}.
 *
 * @author John Bailey
 */
class ContentRepositoryImpl implements ContentRepository, Service<ContentRepository> {

    protected static final String CONTENT = RepositoryUtil.CONTENT;
    private final File repoRoot;
    protected final MessageDigest messageDigest;
    private final Map<String, Set<ContentReference>> contentHashReferences = new HashMap<String, Set<ContentReference>>();
    private final Map<String, Long> obsoleteContents = new HashMap<String, Long>();
    private final long obsolescenceTimeout;

    ContentRepositoryImpl(final File repoRoot, long obsolescenceTimeout) {
        if (repoRoot == null) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.nullVar("repoRoot");
        }
        if (repoRoot.exists()) {
            if (!repoRoot.isDirectory()) {
                throw DeploymentRepositoryLogger.ROOT_LOGGER.notADirectory(repoRoot.getAbsolutePath());
            } else if (!repoRoot.canWrite()) {
                throw DeploymentRepositoryLogger.ROOT_LOGGER.directoryNotWritable(repoRoot.getAbsolutePath());
            }
        } else if (!repoRoot.mkdirs()) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.cannotCreateDirectory(null, repoRoot.getAbsolutePath());
        }
        this.repoRoot = repoRoot;
        this.obsolescenceTimeout = obsolescenceTimeout;
        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.cannotObtainSha1(e, MessageDigest.class.getSimpleName());
        }
    }

    @Override
    public byte[] addContent(InputStream stream) throws IOException {
        byte[] sha1Bytes;
        Path tmp = File.createTempFile(CONTENT, ".tmp", repoRoot).toPath();
        OutputStream fos = Files.newOutputStream(tmp);
        synchronized (messageDigest) {
            messageDigest.reset();
            try {
                DigestOutputStream dos = new DigestOutputStream(fos, messageDigest);
                BufferedInputStream bis = new BufferedInputStream(stream);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    dos.write(bytes, 0, read);
                }
                fos.flush();
                fos.close();
                fos = null;
            } finally {
                safeClose(fos);
            }
            sha1Bytes = messageDigest.digest();
        }
        final Path realFile = getContentFile(getRepoRoot(), sha1Bytes, true);
        if (hasContent(sha1Bytes)) {
            // we've already got this content
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ioex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteTempFile(ioex, tmp.toAbsolutePath().toString());
                tmp.toFile().deleteOnExit();
            }
            DeploymentRepositoryLogger.ROOT_LOGGER.debugf("Content was already present in repository at location %s", realFile.toAbsolutePath().toString());
        } else {
            moveTempToPermanent(tmp, realFile);
            DeploymentRepositoryLogger.ROOT_LOGGER.contentAdded(realFile.toAbsolutePath().toString());
        }

        return sha1Bytes;
    }

    @Override
    public void addContentReference(ContentReference reference) {
        synchronized (contentHashReferences) {
            Set<ContentReference> references = contentHashReferences.get(reference.getHexHash());
            if (references == null) {
                references = new HashSet<ContentReference>();
                contentHashReferences.put(reference.getHexHash(), references);
            }
            references.add(reference);
        }
    }

    @Override
    public VirtualFile getContent(byte[] hash) {
        if (hash == null) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.nullVar("hash");
        }
        return VFS.getChild(getContentFile(getRepoRoot(), hash, true).toUri());
    }

    @Override
    public boolean syncContent(ContentReference reference) {
        return hasContent(reference.getHash());
    }

    @Override
    public boolean hasContent(byte[] hash) {
        return new ContentRepoItem(this, getRepoRoot(), hash).exists();
    }

    private Path getRepoRoot() {
        return repoRoot.toPath();
    }

    @Override
    public void removeContent(ContentReference reference) {
        synchronized (contentHashReferences) {
            final Set<ContentReference> references = contentHashReferences.get(reference.getHexHash());
            if (references != null) {
                references.remove(reference);
                if (!references.isEmpty()) {
                    return;
                }
                contentHashReferences.remove(reference.getHexHash());
            }
        }

        Path file;
        if (HashUtil.isEachHexHashInTable(reference.getHexHash())) {
            removeContent(reference, null);
        } else {
            // TODO the HashUtil.isEachHexHashInTable stuff and then this block is because
            // listLocalContents will create a ContentReference for misc junk files
            // (e.g. .DS_store on OSX) that may end up in the repo. We should find a simpler
            // way to deal with those; e.g. just delete them directly when we first find them.
            String identifier = reference.getContentIdentifier();
            file = Paths.get(identifier);

            try {
                Files.deleteIfExists(file);
            } catch (IOException ex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, file.toString());
            }
            Path parent = file.getParent();
            try {
                Files.deleteIfExists(parent);
            } catch (IOException ex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, parent.toString());
            }
            Path grandParent = parent.getParent();
            if (!grandParent.equals(getRepoRoot())) {
                try (Stream<Path> files = Files.list(grandParent)) {
                    if (!files.findAny().isPresent()) {
                        Files.deleteIfExists(grandParent);
                    }
                } catch (IOException ex) {
                    DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, grandParent.toString());
                }
            }
        }
    }

    void removeContent(ContentReference reference, String path) {
        ContentRepoItem repoItem = new ContentRepoItem(this, getRepoRoot(), reference.getHexHash());
        repoItem.remove(path);

    }

    /**
     * Clean obsolete contents from the content repository.
     * It will first mark contents as obsolete then after some time if these contents are still obsolete they
     * will be removed.
     *
     * @return a map containing the list of marked contents and the list of deleted contents.
     */
    @Override
    public Map<String, Set<String>> cleanObsoleteContent() {
        Map<String, Set<String>> cleanedContents = new HashMap<String, Set<String>>(2);
        cleanedContents.put(MARKED_CONTENT, new HashSet<String>());
        cleanedContents.put(DELETED_CONTENT, new HashSet<String>());
        synchronized (contentHashReferences) {
            for (ContentReference fsContent : listLocalContents()) {
                if (!contentHashReferences.containsKey(fsContent.getHexHash())) { //We have no refrence to this content
                    if(markAsObsolete(fsContent)) {
                        cleanedContents.get(DELETED_CONTENT).add(fsContent.getContentIdentifier());
                    } else {
                        cleanedContents.get(MARKED_CONTENT).add(fsContent.getContentIdentifier());
                    }
                } else {
                    obsoleteContents.remove(fsContent.getHexHash()); //Remove existing references from obsoleteContents
                }
            }
        }
        return cleanedContents;
    }

    @Override
    public byte[] explodeContent(byte[] unexploded) throws IOException, ExplodedContentException {
        assert unexploded != null;
        ContentRepoItem current = new ContentRepoItem(this, getRepoRoot(), unexploded);
        ContentRepoItem result = current.explode();
        return result.getHash();
    }

    @Override
    public void copyExplodedContent(byte[] hash, Path target) throws IOException {
        ContentRepoItem item = new ContentRepoItem(this, getRepoRoot(), hash);
        item.copy(target);
    }

    /**
     * Mark content as obsolete. If content was already marked for obsolescenceTimeout ms then it is removed.
     *
     * @param ref the content refrence to be marked as obsolete.
     *
     * @return true if the content refrence is removed, fale otherwise.
     */
    private boolean markAsObsolete(ContentReference ref) {
        if (obsoleteContents.containsKey(ref.getHexHash())) { //This content is already marked as obsolete
            if (obsoleteContents.get(ref.getHexHash()) + obsolescenceTimeout < System.currentTimeMillis()) {
                DeploymentRepositoryLogger.ROOT_LOGGER.obsoleteContentCleaned(ref.getContentIdentifier());
                removeContent(ref);
                return true;
            }
        } else {
            obsoleteContents.put(ref.getHexHash(), System.currentTimeMillis()); //Mark content as obsolete
        }
        return false;
    }

    private Set<ContentReference> listLocalContents() {
        Set<ContentReference> localReferences = new HashSet<>();
        File[] rootHashes = repoRoot.listFiles();
        if (rootHashes != null) {
            for (File rootHash : rootHashes) {
                if (rootHash.isDirectory()) {
                    File[] complementaryHashes = rootHash.listFiles();
                    if (complementaryHashes == null || complementaryHashes.length == 0) {
                        ContentReference reference = new ContentReference(rootHash.getAbsolutePath(), rootHash.getName());
                        localReferences.add(reference);
                    } else {
                        for (File complementaryHash : complementaryHashes) {
                            String hash = rootHash.getName() + complementaryHash.getName();
                            ContentReference reference = new ContentReference(complementaryHash.getAbsolutePath(), hash);
                            localReferences.add(reference);
                        }
                    }
                }
            }
        } else {
            DeploymentRepositoryLogger.ROOT_LOGGER.localContentListError(repoRoot.getAbsolutePath());
        }
        return localReferences;
    }

    protected static void safeClose(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                //
            }
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        DeploymentRepositoryLogger.ROOT_LOGGER.debugf("%s started", ContentRepository.class.getSimpleName());
    }

    @Override
    public void stop(StopContext context) {
        DeploymentRepositoryLogger.ROOT_LOGGER.debugf("%s stopped", ContentRepository.class.getSimpleName());
    }

    @Override
    public ContentRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
