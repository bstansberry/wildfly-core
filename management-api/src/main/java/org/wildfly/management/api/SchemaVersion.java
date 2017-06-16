/*
Copyright 2017 Red Hat, Inc.

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

package org.wildfly.management.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Encapsulates information about a particular version of a management schema. A unique management schema version
 * is determined by a combination of the major, minor and micro versions of its remote API and the string
 * representation of the URI of the XML namespace used by its XSD. More than one remote API can be associated
 * with a given XSD (i.e. if changes were made to the remote API that didn't involve the persistent
 * form of the management resources), in which case there will be more than one {@code SchemaVersion} that uses
 * that remote API version. Conversely, but less commonly, more than one XSD can be associated
 * with a given remote API version (i.e. if changes were made to persistent format that were not the result of any
 * new resources, attributes or operations available remotely.)
 * <p>
 * Except for the first version for a given schema, all versions have a parent, which represents the version
 * from which they were derived. A parent version can have more than one child, with each child representing
 * a branch point from which divergent variants of the schema were derived.
 * <p>
 * Within a SchemaVersion tree, one node is the "current" version. This is defined as the node with the greatest
 * API version that does not have any children.
 * <p>
 * A SchemaVersion also has a set of "predecessors" which are the set of leaf nodes in the logical tree of versions
 * that were in existence at the historical moment when the version came into existence. The authors of a
 * version can be expected to have had knowledge of its predecessors or their ancestors. They cannot be
 * expected to have had knowledge of any of the children of the version's predecessors, other than the
 * version itself. The parent of a SchemaVersion will always be included in its set of predecessors.
 *
 * @author Brian Stansberry
 */
public final class SchemaVersion {

    /** Builder class for creating a {@link SchemaVersion} */
    public static final class Builder {

        /**
         * Create a schema version builder for a given remote management API version and XSD namespace.
         * @param major the remote management API major version
         * @param minor the remote management API minor version
         * @param micro the remote management API micro version
         * @param namespaceURI the XSD namespace
         * @return the builder
         */
        public static Builder of(int major, int minor, int micro, String namespaceURI) {
            return new Builder(major, minor, micro, namespaceURI);
        }

        private final int major;
        private final int minor;
        private final int micro;
        private final String namespaceURI;
        private Supplier<XMLElementReader<List<ModelNode>>> parserSupplier;
        private boolean nonParseable;

        private Builder(int major, int minor, int micro, String namespaceURI) {
            this.major = major;
            this.minor = minor;
            this.micro = micro;
            this.namespaceURI = namespaceURI;
        }

        /**
         * Configures a {@link Supplier} of a custom parser for XML content that uses this schema's namespace.
         * If no custom parser supplier is configured and the schema isn't {@link #setNonParseable() non-parseable},
         * a standard parser will be generated.
         *
         * @param parserSupplier the supplier
         * @return this builder
         */
        public Builder setCustomParserSupplier(Supplier<XMLElementReader<List<ModelNode>>> parserSupplier) {
            this.parserSupplier = parserSupplier;
            return this;
        }

        /**
         * Marks the schema version as not being parseable. Used for old versions where parsing of
         * xml documents using this version's XSD is no longer supported.
         *
         * @return this builder
         */
        public Builder setNonParseable() {
            this.nonParseable = true;
            return this;
        }

        /**
         * Builds the {@link SchemaVersion}.
         * @return the {@link SchemaVersion}
         */
        public SchemaVersion build() {
            return new SchemaVersion(new ApiVersion(major, minor, micro), namespaceURI, parserSupplier, nonParseable);
        }
    }

    /**
     * Converts a time-ordered sequence of schema versions into a logical tree that includes the
     * parent/child and predecessor/non-predecessor relationships between the versions.
     * @param linearHistory a list of versions ordered according to the sequence in time when the management
     *                      schema the version represents was released in final form
     * @return a SchemaVersion that represents the {@link #isCurrent() current} version in the tree of schemas created
     *         from the linear history
     */
    public static SchemaVersion linearToTree(SchemaVersion... linearHistory) {
        assert linearHistory != null && linearHistory.length > 0;
        SchemaVersion current = getUnconnectedVersion(linearHistory[0]);
        if (linearHistory.length > 1) {
            Set<SchemaVersion> leaves = new LinkedHashSet<>();
            leaves.add(current);
            for (int i = 1; i < linearHistory.length; i++) {
                SchemaVersion version = getUnconnectedVersion(linearHistory[i]);
                SchemaVersion parent = version.setPredecessors(leaves);
                leaves.remove(parent);
                leaves.add(version);
                if (version.isCurrent()) {
                    current = version;
                }
            }
        }
        return current;
    }

    private static SchemaVersion getUnconnectedVersion(SchemaVersion version) {
        return version.connected ? new SchemaVersion(version) : version;
    }

    private final ApiVersion apiVersion;
    private final String namespaceURI;
    private final Supplier<XMLElementReader<List<ModelNode>>> parserSupplier;
    private final boolean nonParseable;
    private boolean connected;
    private SchemaVersion parent;
    private ApiVersion currentVersion;
    private Set<SchemaVersion> predecessors = Collections.emptySet();
    private Set<SchemaVersion> children = Collections.emptySet();

    private SchemaVersion(ApiVersion apiVersion, String namespaceURI,
                          Supplier<XMLElementReader<List<ModelNode>>>  parserSupplier,
                          boolean nonParseable) {
        this.apiVersion = apiVersion;
        this.namespaceURI = namespaceURI;
        this.parserSupplier = nonParseable ? null : parserSupplier;
        this.nonParseable = nonParseable;
    }

    private SchemaVersion(SchemaVersion toCopy) {
        this (toCopy.apiVersion, toCopy.namespaceURI, toCopy.parserSupplier, toCopy.nonParseable);
    }

    public int getMajorApiVersion() {
        return apiVersion.major;
    }

    public int getMinorApiVersion() {
        return apiVersion.minor;
    }

    public int getMicroApiVersion() {
        return apiVersion.micro;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public Supplier<XMLElementReader<List<ModelNode>>>  getCustomParserSupplier() {
        return parserSupplier;
    }

    public boolean isNonParseable() {
        return nonParseable;
    }

    public boolean isCurrent() {
        return children.size() == 0 && apiVersion.equals(currentVersion);
    }

    public SchemaVersion getParent() {
        return parent;
    }

    public Set<SchemaVersion> getChildren() {
        return children.size() <= 1 ? children : Collections.unmodifiableSet(children);
    }

    public Set<SchemaVersion> getPredecessors() {
        return predecessors.size() <= 1 ? predecessors : Collections.unmodifiableSet(predecessors);
    }

    private SchemaVersion setPredecessors(Set<SchemaVersion> leaves) {
        assert parent == null && predecessors == null;
        assert leaves.size() > 0;
        for (SchemaVersion candidate : leaves) {
            addPredecessor(candidate);
            // If we are higher or same API as the candidate and we have no parent or
            // the candidate is later than the current parent, the candidate is our parent
            if ((this.apiVersion.compareTo(candidate.apiVersion) >= 0)
                && (parent == null || (candidate.apiVersion.compareTo(parent.apiVersion) > 0))) {
                parent = candidate;
            }
        }
        if (parent == null) {
            // Original history must have been incorrectly ordered
            throw new IllegalStateException();
        }
        parent.addChild(this);
        connected = true;
        return parent;
    }

    private void addPredecessor(SchemaVersion predecessor) {
        if (predecessors.size() == 0) {
            predecessors = Collections.singleton(predecessor);
        } else {
            if (predecessors.size() == 1) {
                predecessors = new LinkedHashSet<>(predecessors);
            }
            predecessors.add(predecessor);
        }
    }

    private void addChild(SchemaVersion child) {
        int size = children.size();
        if (size == 0) {
            children = Collections.singleton(child);
        } else {
            if (size == 1) {
                children = new LinkedHashSet<>(children);
            }
            children.add(child);
        }
        connected = true;
        // Inform the rest of the tree of this possibly new 'current' version.
        // This will update our own currentVersion if 'child' is latest
        addApiVersion(child.apiVersion);
        // Inform the child of the currentVersion. The addApiVersion call will have
        // already done this if the child is current, but checking for that costs more
        // than just doing it
        child.setCurrentVersion(currentVersion);
    }

    private void setCurrentVersion(ApiVersion currentVersion) {
        this.currentVersion = currentVersion;
        for (SchemaVersion child : children) {
            child.setCurrentVersion(currentVersion);
        }
    }

    private void addApiVersion(ApiVersion added) {
        if (parent != null) {
            parent.addApiVersion(added);
        } else if (currentVersion == null || currentVersion.compareTo(added) < 0) {
            setCurrentVersion(added);
        }
    }

    private static class ApiVersion implements Comparable<ApiVersion> {
        private final int major;
        private final int minor;
        private final int micro;

        private ApiVersion(int major, int minor, int micro) {
            this.major = major;
            this.minor = minor;
            this.micro = micro;
        }

        @Override
        public int compareTo(ApiVersion other) {
            int result = major - other.major;
            if (result == 0) {
                result = minor - other.minor;
                if (result == 0) {
                    result = micro - other.micro;
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApiVersion that = (ApiVersion) o;

            return major == that.major && minor == that.minor && micro == that.micro;
        }

        @Override
        public int hashCode() {
            int result = major;
            result = 31 * result + minor;
            result = 31 * result + micro;
            return result;
        }
    }
}
