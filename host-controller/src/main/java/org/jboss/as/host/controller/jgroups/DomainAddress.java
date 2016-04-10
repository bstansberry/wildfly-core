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

package org.jboss.as.host.controller.jgroups;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.jboss.as.host.controller.discovery.RemoteDomainControllerConnectionConfiguration;
import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.PhysicalAddress;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.IpAddress;

/**
 * JGroups {@link PhysicalAddress} that encapsulates the protocol, address and port info used for
 * the JBoss Remoting based connections used for intra-domain communications.
 *
 * @author Brian Stansberry
 */
public final class DomainAddress implements PhysicalAddress {

    private static final long serialVersionUID = -1L;

    static {
        ClassConfigurator.add((short)9990, DomainAddress.class);
    }

    private String protocol;
    private IpAddress ipAddress;

    // Used only by Externalization
    public DomainAddress() {
    }

    /** Standard constructor */
    DomainAddress(String protocol, InetAddress address, int port) {
        this.protocol = protocol;
        this.ipAddress = new IpAddress(address, port);
    }

    /** Constructor for converting {@link org.jboss.as.host.controller.discovery.DiscoveryOption} results.*/
    DomainAddress(RemoteDomainControllerConnectionConfiguration rdccc) throws UnknownHostException {
        this.protocol = rdccc.getProtocol();
        this.ipAddress = new IpAddress(rdccc.getHost(), rdccc.getPort());
    }

    String getProtocol() {
        return protocol;
    }

    InetAddress getHost() {
        return ipAddress.getIpAddress();
    }

    int getPort() {
        return ipAddress.getPort();
    }

    URI toURI() {
        try {
            return new URI(protocol, null, getHost().getHostAddress(), getPort(), null, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException();
        }

    }

    @Override
    public int size() {
        // length (1 bytes) + number of bytes in protocol + number of bytes in IpAddress
        return Global.BYTE_SIZE + protocol.getBytes().length + ipAddress.size() ;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            writeTo(out);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            readFrom(in);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int compareTo(Address o) {
        if(this == o) return 0;
        DomainAddress other = (DomainAddress) o;
        int result = protocol.compareTo(other.protocol);
        return result == 0 ? ipAddress.compareTo(other.ipAddress) : result;
    }

    @Override
    public void writeTo(DataOutput out) throws Exception {
        byte[] prot = protocol.getBytes();
        out.write(prot.length);
        out.write(prot);
        ipAddress.writeTo(out);
    }

    @Override
    public void readFrom(DataInput in) throws Exception {
        int len = in.readByte();
        byte[] a = new byte[len]; // 4 bytes (IPv4) or 16 bytes (IPv6)
        in.readFully(a);
        protocol = new String(a);
        ipAddress = new IpAddress();
        ipAddress.readFrom(in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainAddress that = (DomainAddress) o;

        return protocol.equals(that.protocol) && ipAddress.equals(that.ipAddress);
    }

    @Override
    public int hashCode() {
        int result = protocol.hashCode();
        result = 31 * result + ipAddress.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DomainAddress{" +
                "protocol=" + protocol +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
