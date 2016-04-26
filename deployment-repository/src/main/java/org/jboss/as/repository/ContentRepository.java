/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.repository;

import static java.lang.Long.getLong;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;

/**
 * Repository for deployment content and other managed content.
 *
 * @author John Bailey
 */
public interface ContentRepository {

    /**
     * Standard ServiceName under which a service controller for an instance of
     * @code Service<ContentRepository> would be registered.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("content-repository");

    /**
     * Time after which a marked obsolete content will be removed.
     * Currently 5 minutes.
     */
    long OBSOLETE_CONTENT_TIMEOUT = getSecurityManager() == null ? getLong(Factory.UNSUPPORTED_PROPERTY, 300000L) : doPrivileged((PrivilegedAction<Long>) () -> getLong(Factory.UNSUPPORTED_PROPERTY, 300000L));

    String DELETED_CONTENT = "deleted-contents";
    String MARKED_CONTENT = "marked-contents";

    /**
     * Add the given content to the repository along with a reference tracked by {@code name}.
     *
     * @param stream stream from which the content can be read. Cannot be <code>null</code>
     * @return the hash of the content that will be used as an internal identifier for the content. Will not be
     * <code>null</code>
     * @throws IOException if there is a problem reading the stream
     */
    byte[] addContent(InputStream stream) throws IOException;

    /**
     * Adds a reference to the content.
     *
     * @param reference a reference to the content to be referenced. This is also used in
     * {@link #removeContent(ContentReference reference)}
     */
    void addContentReference(ContentReference reference);

    /**
     * Get the content as a virtual file.
     *
     * @param hash the hash. Cannot be {@code null}
     *
     * @return the content as a virtual file
     */
    VirtualFile getContent(byte[] hash);

    /**
     * Gets whether content with the given hash is stored in the repository.
     *
     * @param hash the hash. Cannot be {@code null}
     *
     * @return {@code true} if the repository has content with the given hash.
     */
    boolean hasContent(byte[] hash);

    /**
     * Synchronize content with the given reference. This may be used in favor of {@linkplain #hasContent(byte[])} to
     * explicitly allow additional operations to synchronize the local content with some external repository.
     *
     * @param reference the reference to be synchronized. Cannot be {@code null}
     *
     * @return {@code true} if the repository has content with the given reference
     */
    boolean syncContent(ContentReference reference);

    /**
     * Remove the given content from the repository.
     *
     * Remove the given content from the repository. The reference will be removed, and if there are no references left
     * the content will be totally removed.
     *
     * @param reference a reference to the content to be unreferenced. This is also used in
     * {@link #addContentReference(ContentReference reference)}
     */
    void removeContent(ContentReference reference);

    /**
     * Clean content that is not referenced from the repository.
     *
     * Remove the contents that are no longer referenced from the repository.
     *
     * @return the list of obsolete contents that were removed and the list of obsolete contents that were marked to
     * be removed.
     */
    Map<String, Set<String>> cleanObsoleteContent();

    /**
     * Takes the content found at hash {@code unexploded} and explodes it.
     *
     * @param unexploded the hash of the unexploded content
     * @return the hash of the exploded content.
     *
     * @throws IllegalStateException if there is no content identified by {@code unexploded} or if that content is not an archive
     */
    byte[] explodeContent(byte[] unexploded) throws IOException, ExplodedContentException;

    /**
     * Copies the managed exploded content to the given path.
     * @param hash   the hash of the exploded content
     * @param target path to which the content should be copied
     *
     * @throws IllegalStateException if there is no content identified by {@code exploded} or if that content is not a directory
     */
    void copyExplodedContent(byte[] hash, Path target) throws IOException;

    class Factory {
        /**
         * For testing purpose only.
         * @deprecated DON'T USE IT.
         */
        @Deprecated
        private static final String UNSUPPORTED_PROPERTY = "org.wildfly.unsupported.content.repository.obsolescence";

        public static void addService(final ServiceTarget serviceTarget, final File repoRoot) {
            ContentRepositoryImpl contentRepository = new ContentRepositoryImpl(repoRoot, OBSOLETE_CONTENT_TIMEOUT);
            serviceTarget.addService(SERVICE_NAME, contentRepository).install();
        }

        public static ContentRepository create(final File repoRoot) {
            return create(repoRoot, OBSOLETE_CONTENT_TIMEOUT);
        }

        static ContentRepository create(final File repoRoot, long timeout) {
            return new ContentRepositoryImpl(repoRoot, timeout);
        }

    }

}
