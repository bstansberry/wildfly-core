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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * Prototype utility code for experiments with rehashing exploded content as indivdual pieces are added, updated
 * and removed.
 *
 * TODO remove this once the experimental stage is done and something real has been developed.
 *
 * @author Brian Stansberry
 */
public class RecursiveHasher {

    public static void main(String[] args) {
        File f = new File("/Users/bstansberry/tmp/helloWorld1.war");
        File newContent = new File(f, "WEB-INF/web.xml");
        test(UpdateType.ADD, f, newContent);
        test(UpdateType.UPDATE, f, newContent);
        test(UpdateType.REMOVE, f, null);
    }

    private static void test(UpdateType updateType, File root, File child) {
        try {
            FileBasedHashableItem fbhs = new FileBasedHashableItem(root);
            InputStream newContent = updateType == UpdateType.REMOVE ? null : new FileInputStream(child);
            List<UpdatedPath> path = UpdatedPath.create(updateType, newContent, "WEB-INF", "web.xml");
            InputStream is = fbhs.getInputStream(path, 0);
            DigestInputStream dis = new DigestInputStream(is, createMessageDigest());
            byte[] bytes = new byte[1024];
            //noinspection StatementWithEmptyBody
            while (dis.read(bytes, 0, 1024) != -1) {

            }
            System.out.println(updateType);
            System.out.println("Root: " + HashUtil.bytesToHexString(dis.getMessageDigest().digest()));
            for (UpdatedPath up : path) {
                String hex = up.dis == null ? "N/A" : HashUtil.bytesToHexString(up.dis.getMessageDigest().digest());
                System.out.println(up.elementName + ": " + hex);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }
    interface HashableItem {
        boolean isDirectory();
        InputStream getInputStream() throws IOException;
        InputStream getInputStream(List<UpdatedPath> path, int yourIndex) throws IOException;
    }

    private enum UpdateType {
        ADD,
        REMOVE,
        UPDATE
    }

    private static class UpdatedPath {
        private final UpdateType updateType;
        private final String elementName;
        private final InputStream newContent;
        private DigestInputStream dis;
        private String replacedHash;

        private static List<UpdatedPath> create(UpdateType updateType, InputStream newContent, String... elements) {
            List<UpdatedPath> result = new ArrayList<>();
            for (int i = 0; i < elements.length; i++) {
                if (i < elements.length - 1) {
                    result.add(new UpdatedPath(null, null, elements[i]));
                } else {
                    result.add(new UpdatedPath(updateType, newContent, elements[i]));
                }
            }
            return Collections.unmodifiableList(result);
        }

        private UpdatedPath(UpdateType updateType, InputStream newContent, String elementName) {
            this.updateType = updateType;
            this.newContent = newContent;
            this.elementName = elementName;
        }
    }

    private static class FileBasedHashableItem implements HashableItem {
        private final File file;

        FileBasedHashableItem(File file) {
            this.file = file;
        }

        @Override
        public boolean isDirectory() {
            return file.isDirectory();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return getInputStream(null, -1);
        }

        @Override
        public InputStream getInputStream(List<UpdatedPath> path, int oneBasedIndex) throws IOException {
            if (file.isDirectory()) {
                MessageDigest messageDigest;
                String targetChild;
                if (oneBasedIndex > -1 && oneBasedIndex < path.size()) {
                    targetChild = path.get(oneBasedIndex).elementName;
                    messageDigest = createMessageDigest();
                } else {
                    targetChild = null;
                    messageDigest = null;
                }
                File[] children = file.listFiles();
                assert  children != null;
                Arrays.sort(children, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                Vector<InputStream> vector = new Vector<>();
                for (File f : children) {
                    FileBasedHashableItem fbhs = new FileBasedHashableItem(f);
                    InputStream nameStream = new ByteArrayInputStream(f.getName().getBytes("UTF-8"));
                    InputStream contentStream;
                    if (f.getName().equals(targetChild)) {
                        if (oneBasedIndex == path.size() - 1 && path.get(path.size() - 1).updateType == UpdateType.REMOVE) {
                            // The last element of the path is being removed; ignore it
                            contentStream = null;
                        } else {
                            InputStream childStream = fbhs.getInputStream(path, oneBasedIndex + 1);
                            DigestInputStream dis = new DigestInputStream(childStream, messageDigest);
                            path.get(oneBasedIndex).dis = dis;
                            contentStream = dis;
                        }
                    } else {
                        contentStream = fbhs.getInputStream();
                    }
                    if (contentStream != null) {
                        vector.add(new SequenceInputStream(nameStream, contentStream));
                    }
                }
                return new SequenceInputStream(vector.elements());
            } else {
                InputStream newContent = null;
                if (path != null && oneBasedIndex == path.size()) {
                    newContent = path.get(oneBasedIndex -1).newContent;
                }
                return newContent != null ? newContent : new FileInputStream(file);
            }
        }
    }

    private static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
