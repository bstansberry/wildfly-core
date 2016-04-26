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

import static org.jboss.as.repository.RepositoryUtil.CHILDREN;
import static org.jboss.as.repository.RepositoryUtil.CONTENT;
import static org.jboss.as.repository.RepositoryUtil.CONTENT_TYPES;
import static org.jboss.as.repository.RepositoryUtil.EMPTY_DIR;
import static org.jboss.as.repository.RepositoryUtil.createMessageDigest;
import static org.jboss.as.repository.RepositoryUtil.getContentFile;
import static org.jboss.as.repository.RepositoryUtil.getContentHashDir;
import static org.jboss.as.repository.RepositoryUtil.moveTempToPermanent;
import static org.jboss.as.repository.RepositoryUtil.safeClose;
import static org.jboss.as.repository.RepositoryUtil.safeDelete;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.jboss.as.repository.logging.DeploymentRepositoryLogger;
import org.jboss.vfs.util.PathTokenizer;

/**
 * A proper piece of content controlled by the content repository. Proper in the sense that it is meant to formally
 * be under content repo control, i.e. hashed and stored in the proper persisted form. So this excludes random files that
 * may end up in the repo's portion of the filesystem.
 * <p>
 * A {@code ContentRepoItem} has not necessarily been persisted; this class can be used for data manipulation that
 * results in persistence.
 *
 * @author Brian Stansberry
 */
class ContentRepoItem implements Closeable {
    private final ContentRepositoryImpl contentRepository;
    private final Path repoRoot;
    // For normal persisted content
    private byte[] hash;
    private String hexHash;
    //  For non-directory content not yet integrated into the normal repo structure
    private InputStream inputStream;
    private Path tmpFile;
    //  For directory content not yet integrated into the normal repo structure
    private SortedMap<String, UnpersistedChild> unpersistedChildren;

    ContentRepoItem(ContentRepositoryImpl contentRepository, Path repoRoot, byte[] hash) {
        this.contentRepository = contentRepository;
        this.repoRoot = repoRoot;
        this.hash = hash;
    }

    ContentRepoItem(ContentRepositoryImpl contentRepository, Path repoRoot, String hexHash) {
        this.contentRepository = contentRepository;
        this.repoRoot = repoRoot;
        this.hexHash = hexHash;
    }

    private ContentRepoItem(ContentRepositoryImpl contentRepository, Path repoRoot, SortedMap<String, UnpersistedChild> unpersistedChildren) {
        this(contentRepository, repoRoot, (byte[]) null);
        this.unpersistedChildren = unpersistedChildren;
    }

    ContentRepoItem(ContentRepositoryImpl contentRepository, Path repoRoot, InputStream source) throws IOException {
        this(contentRepository, repoRoot, (byte[]) null);
        File file = File.createTempFile(CONTENT, ".tmp", repoRoot.toFile());
        this.tmpFile = file.toPath();
        this.inputStream = new ReadToTempInputStream(source, file);
    }

    @Override
    public void close() throws IOException {
        safeClose(inputStream);
        inputStream = null;
        safeDelete(tmpFile);
        tmpFile = null;
        if (unpersistedChildren != null) {
            for (UnpersistedChild child : unpersistedChildren.values()) {
                safeClose(child.repoItem);
                safeClose(child.digestStream);
            }
            unpersistedChildren = null;
        }
    }

    boolean exists() {
        String hexHash = getHexHash();
        Path dir = getContentHashDir(repoRoot, hexHash, true);
        for (String type : CONTENT_TYPES) {
            if (dir.resolve(type).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    byte[] getHash() {
        if (hash == null) {
            if (hexHash == null) {
                throw new IllegalStateException();
            } else {
                hash = HashUtil.hexStringToByteArray(hexHash);
            }
        }
        return hash;
    }

    ContentRepoItem explode() throws IOException, ExplodedContentException {

        final Path existing = getContentFile(repoRoot, getHexHash(), true);

        Map<List<String>, ContentRepoItem> createdDirs = new HashMap<>();

        ZipFile zf;
        try {
            zf = new ZipFile(existing.toFile());
        } catch (ZipException ze) {
            // This content isn't a zip, so it can't be exploded
            throw new ExplodedContentException(ze);
        }

        try (ZipFile zipFile = zf; ContentRepoItem rootDirItem = new ContentRepoItem(contentRepository, repoRoot, new TreeMap<String, UnpersistedChild>())) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            FILES_LOOP:
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                final String name = zipEntry.getName();
                final List<String> tokens = PathTokenizer.getTokens(name);
                int tknCount = tokens.size();
                for (int i = 0; i < tknCount; i++) {
                    String token = tokens.get(i);
                    if (PathTokenizer.isCurrentToken(token) || PathTokenizer.isReverseToken(token)) {
                        // invalid file; skip it!
                        continue FILES_LOOP;
                    }
                    boolean last = i == tknCount - 1;
                    List<String> current = last ? tokens : tokens.subList(0, i + 1);
                    if ((!last || zipEntry.isDirectory()) && !createdDirs.containsKey(current)) {
                        // Newly discovered directory
                        ContentRepoItem dirItem = new ContentRepoItem(contentRepository, repoRoot, new TreeMap<String, UnpersistedChild>());
                        ContentRepoItem parentDirItem;
                        if (i == 0) {
                            parentDirItem = rootDirItem;
                        } else {
                            List<String> parent = current.subList(0, i);
                            parentDirItem = createdDirs.get(parent);
                        }
                        parentDirItem.unpersistedChildren.put(token, new UnpersistedChild(dirItem));
                        createdDirs.put(current, dirItem);
                    }
                }

                if (!zipEntry.isDirectory()) {
                    ContentRepoItem nonDirItem = new ContentRepoItem(contentRepository, repoRoot, zipFile.getInputStream(zipEntry));
                    ContentRepoItem parentDirItem;
                    if (tknCount == 1) {
                        parentDirItem = rootDirItem;
                    } else {
                        List<String> parent = tokens.subList(0, tknCount - 1);
                        parentDirItem = createdDirs.get(parent);
                    }
                    parentDirItem.unpersistedChildren.put(tokens.get(tknCount - 1), new UnpersistedChild(nonDirItem));
                }
            }

            try (DigestInputStream dis = new DigestInputStream(rootDirItem.getInputStream(), createMessageDigest())) {

                byte[] read = new byte[1024];
                //noinspection StatementWithEmptyBody
                while (dis.read(read, 0, 1024) != -1) {

                }

                rootDirItem.persist(dis.getMessageDigest().digest(), null);
            }

            return rootDirItem;
        }
    }

    void persist(byte[] hash, String path) throws IOException {
        if (hasHash()) {
            throw new IllegalStateException();
        }
        path = path == null ? "" : path;

        final Path persistedPath;
        if (contentRepository.hasContent(hash)) {

            // we've already got this content
            persistedPath = null;

            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException ioex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteTempFile(ioex, tmpFile.toAbsolutePath().toString());
                tmpFile.toFile().deleteOnExit();
            }
            if (DeploymentRepositoryLogger.ROOT_LOGGER.isDebugEnabled()) {
                Path existing = getContentFile(repoRoot, hash, false);
                DeploymentRepositoryLogger.ROOT_LOGGER.debugf("Content was already present in repository at location %s",
                        existing.toAbsolutePath().toString());
            }
        } else if (tmpFile != null) {
            persistedPath = getContentFile(repoRoot, hash, true);
            moveTempToPermanent(tmpFile, persistedPath);
        } else if (unpersistedChildren.isEmpty()) {
            persistedPath = getContentHashDir(repoRoot, hash, true).resolve(EMPTY_DIR);
            Files.createFile(persistedPath);
        } else {
            Properties props = new Properties();
            for (Map.Entry<String, UnpersistedChild> entry : unpersistedChildren.entrySet()) {
                String childName = entry.getKey();
                String childPath = path + "/" + childName;
                UnpersistedChild up = entry.getValue();
                up.hash = up.digestStream.getMessageDigest().digest();
                up.repoItem.persist(up.hash, childPath);
                props.put(childName, HashUtil.bytesToHexString(up.hash));
                contentRepository.addContentReference(new ContentReference(path, up.hash));
            }
            persistedPath = getContentHashDir(repoRoot, hash, true).resolve(CHILDREN);
            props.store(new FileOutputStream(persistedPath.toFile()), null);
        }

        if (persistedPath != null) {
            DeploymentRepositoryLogger.ROOT_LOGGER.contentAdded(persistedPath.toAbsolutePath().toString());
        }

        this.hash = hash;
    }

    void copy(Path to) throws IOException {
        if (!hasHash()) {
            throw new IllegalStateException();
        }
        if (isDirectory()) {
            Files.createDirectory(to);
            Path dir = getContentHashDir(repoRoot, getHexHash(), true);

            File file = dir.resolve(CHILDREN).toFile();
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                ContentRepoItem childItem = new ContentRepoItem(contentRepository, repoRoot, (String) entry.getValue());
                childItem.copy(to.resolve((String) entry.getKey()));
            }
        } else {
            Path content = getContentFile(repoRoot, getHexHash(), true);
            Files.copy(content, to);
        }
    }

    boolean remove(String path) {
        // Start by assuming our content file is "children" as that is
        // the case that may trigger an IOException that we have to report
        String contentFile = CHILDREN;
        try {
            if (isDirectory()) {
                Properties props = getChildrenHashes();
                if (props == null) {
                    contentFile = EMPTY_DIR;
                } else {
                    path = path == null ? "" : path;
                    for (Map.Entry<Object, Object> entry : props.entrySet()) {
                        String childPath = path + "/" + entry.getKey();
                        String hexHash = (String) entry.getValue();
                        contentRepository.removeContent(new ContentReference(childPath, hexHash), childPath);
                    }
                }
            } else {
                contentFile = CONTENT;
            }

            return RepositoryUtil.removeContent(repoRoot, getHexHash(), contentFile);
        } catch (IOException ex) {
            File file = getContentFile(repoRoot, getHexHash(), contentFile).toFile();
            DeploymentRepositoryLogger.ROOT_LOGGER.contentDeletionError(ex, file.toString());
        }
        return false;
    }

    private Properties getChildrenHashes() throws IOException {

        Properties result = null;
        Path dir = getContentHashDir(repoRoot, getHexHash(), true);

        File file = dir.resolve(CHILDREN).toFile();
        if (file.exists()) {
            result = new Properties();
            result.load(new FileInputStream(file));
        }

        return result;
    }

    private String getHexHash() {
        if (hexHash == null) {
            if (hash == null) {
                throw new IllegalStateException();
            } else {
                hexHash = HashUtil.bytesToHexString(hash);
            }
        }
        return hexHash;
    }

    private boolean hasHash() {
        return hash != null || hexHash != null;
    }

    private boolean isDirectory() {
        if (unpersistedChildren != null) {
            return true;
        } else if (tmpFile != null) {
            return false;
        } else {
            Path path = getContentHashDir(repoRoot, getHexHash(), false);
            return path.resolve(CHILDREN).toFile().exists() || path.resolve(EMPTY_DIR).toFile().exists();
        }
    }

    private InputStream getInputStream() throws IOException {
        if (inputStream != null) {
            return inputStream;
        } else if (hasHash()) {
            Path dir = getContentHashDir(repoRoot, getHexHash(), true);

            File file = dir.resolve(CONTENT).toFile();
            if (file.exists()) {
                return new FileInputStream(file);
            }
            file = dir.resolve(EMPTY_DIR).toFile();
            if (file.exists()) {
                return RepositoryUtil.NULL_STREAM;
            }

            file = dir.resolve(CHILDREN).toFile();
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            if (properties.size() == 0) {
                return RepositoryUtil.NULL_STREAM;
            }

            TreeMap<String, ContentRepoItem> children = new TreeMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String name = (String) entry.getKey();
                String childHash = (String) entry.getValue();
                children.put(name, new ContentRepoItem(contentRepository, repoRoot, childHash));
            }
            Vector<InputStream> vector = new Vector<>();
            for (Map.Entry<String, ContentRepoItem> entry : children.entrySet()) {
                InputStream nameStream = new ByteArrayInputStream(entry.getKey().getBytes("UTF-8"));
                InputStream contentStream = entry.getValue().getInputStream();
                vector.add(new SequenceInputStream(nameStream, contentStream));
            }
            return new SequenceInputStream(vector.elements());
        } else if (unpersistedChildren != null) {
            if (unpersistedChildren.size() == 0) {
                return RepositoryUtil.NULL_STREAM;
            }
            Vector<InputStream> vector = new Vector<>();
            for (Map.Entry<String, UnpersistedChild> entry : unpersistedChildren.entrySet()) {
                InputStream nameStream = new ByteArrayInputStream(entry.getKey().getBytes("UTF-8"));
                UnpersistedChild up = entry.getValue();
                up.digestStream = new DigestInputStream(up.repoItem.getInputStream(), createMessageDigest());
                vector.add(new SequenceInputStream(nameStream, up.digestStream));
            }
            return new SequenceInputStream(vector.elements());
        }
        throw new IllegalStateException();
    }

    private static class UnpersistedChild {
        private final ContentRepoItem repoItem;
        private DigestInputStream digestStream;
        private byte[] hash;

        private UnpersistedChild(ContentRepoItem repoItem) {
            this.repoItem = repoItem;
        }
    }

    private static class ReadToTempInputStream extends FilterInputStream {

        private final InputStream in;
        private final OutputStream out;

        private ReadToTempInputStream(InputStream in, File tmpFile) throws FileNotFoundException {
            super(in);
            this.in = in;
            this.out = new BufferedOutputStream(new FileOutputStream(tmpFile), 1024);
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read != -1) {
                out.write(read);
            }
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = super.read(b);
            if (read != -1) {
                out.write(b, 0, read);
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read != -1) {
                out.write(b, off, read);
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            safeClose(out);
            safeClose(in);
        }
    }
}
