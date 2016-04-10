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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.discovery.RemoteDomainControllerConnectionConfiguration;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.ManagedOperation;
import org.jgroups.annotations.Property;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.PingHeader;
import org.jgroups.util.BoundedList;
import org.jgroups.util.Responses;
import org.jgroups.util.Tuple;

/**
 * JGroups {@link Discovery} implementation that uses the domain controller {@link DiscoveryOption}
 * settings as a source of data.
 *
 * @author Brian Stansberry
 */
public class DomainDiscovery extends Discovery {

    private final LocalHostControllerInfo hostControllerInfo;

    /** https://jira.jboss.org/jira/browse/JGRP-989 */
    private BoundedList<PhysicalAddress> dynamic_hosts;
    private volatile List<PhysicalAddress> domainHosts = Collections.emptyList();

    @Property(description="max number of hosts to keep beyond the ones discovered via domain controller discovery")
    protected int max_dynamic_hosts=2000;

    DomainDiscovery(LocalHostControllerInfo localHostInfo) {
        this.hostControllerInfo = localHostInfo;
        this.id = 9090;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @ManagedAttribute
    public String getDynamicHostList() {
        return dynamic_hosts.toString();
    }

    @ManagedOperation
    public void clearDynamicHostList() {
        dynamic_hosts.clear();
    }

    @ManagedAttribute
    public String getDomainHostsList() {
        return domainHosts.toString();
    }

    public void init() throws Exception {
        super.init();
        dynamic_hosts=new BoundedList<>(max_dynamic_hosts);
    }

    public Object down(Event evt) {
        Object retval=super.down(evt);
        switch(evt.getType()) {
            case Event.VIEW_CHANGE:
                for(Address logical_addr: members) {
                    PhysicalAddress physical_addr=(PhysicalAddress)down_prot.down(new Event(Event.GET_PHYSICAL_ADDRESS, logical_addr));
                    if(physical_addr != null && !domainHosts.contains(physical_addr)) {
                        dynamic_hosts.addIfAbsent(physical_addr);
                    }
                }
                break;
            case Event.SET_PHYSICAL_ADDRESS:
                Tuple<Address,PhysicalAddress> tuple=(Tuple<Address,PhysicalAddress>)evt.getArg();
                PhysicalAddress physical_addr=tuple.getVal2();
                if(physical_addr != null && !domainHosts.contains(physical_addr))
                    dynamic_hosts.addIfAbsent(physical_addr);
                break;
        }
        return retval;
    }

    public void discoveryRequestReceived(Address sender, String logical_name, PhysicalAddress physical_addr) {
        super.discoveryRequestReceived(sender, logical_name, physical_addr);
        if(physical_addr != null) {
            if(!domainHosts.contains(physical_addr))
                dynamic_hosts.addIfAbsent(physical_addr);
        }
    }

    @Override
    public void findMembers(List<Address> members, boolean initial_discovery, Responses responses) {
        List<DiscoveryOption> discoveryOptions = hostControllerInfo.getRemoteDomainControllerDiscoveryOptions();
        Set<RemoteDomainControllerConnectionConfiguration> rdcccs = new LinkedHashSet<>();
        for (DiscoveryOption dopt : discoveryOptions) {
            List<RemoteDomainControllerConnectionConfiguration> opts = dopt.discover();
            log.trace("%s discovered %s", dopt, opts);
            rdcccs.addAll(opts);
        }
        List<PhysicalAddress> hosts = new ArrayList<>(rdcccs.size());
        for (RemoteDomainControllerConnectionConfiguration rdccc : rdcccs) {
            try {
                hosts.add(new DomainAddress(rdccc));
            } catch (UnknownHostException e) {
                // ignore; we can't discover this one
            }
        }
        this.domainHosts = hosts;

        PhysicalAddress physical_addr=(PhysicalAddress)down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));

        // https://issues.jboss.org/browse/JGRP-1670
        PingData data=new PingData(local_addr, false, org.jgroups.util.UUID.get(local_addr), physical_addr);
        PingHeader hdr=new PingHeader(PingHeader.GET_MBRS_REQ).clusterName(cluster_name);

        List<PhysicalAddress> cluster_members=new ArrayList<>(domainHosts.size() + (dynamic_hosts != null? dynamic_hosts.size() : 0) + 5);
        for(PhysicalAddress phys_addr: domainHosts)
            if(!cluster_members.contains(phys_addr))
                cluster_members.add(phys_addr);
        if(dynamic_hosts != null) {
            for(PhysicalAddress phys_addr : dynamic_hosts)
                if(!cluster_members.contains(phys_addr))
                    cluster_members.add(phys_addr);
        }

        if(use_disk_cache) {
            // this only makes sense if we have PDC below us
            Collection<PhysicalAddress> list=(Collection<PhysicalAddress>)down_prot.down(new Event(Event.GET_PHYSICAL_ADDRESSES));
            if(list != null)
                for(PhysicalAddress phys_addr: list)
                    if(!cluster_members.contains(phys_addr))
                        cluster_members.add(phys_addr);
        }

        for(final PhysicalAddress addr: cluster_members) {
            if(physical_addr != null && addr.equals(physical_addr)) // no need to send the request to myself
                continue;

            // the message needs to be DONT_BUNDLE, see explanation above
            final Message msg=new Message(addr).setFlag(Message.Flag.INTERNAL, Message.Flag.DONT_BUNDLE, Message.Flag.OOB)
                    .putHeader(this.id,hdr).setBuffer(marshal(data));

            if(async_discovery_use_separate_thread_per_request) {
                timer.execute(new Runnable() {
                    public void run() {
                        log.trace("%s: sending discovery request to %s", local_addr, msg.getDest());
                        down_prot.down(new Event(Event.MSG, msg));
                    }
                });
            }
            else {
                log.trace("%s: sending discovery request to %s", local_addr, msg.getDest());
                down_prot.down(new Event(Event.MSG, msg));
            }
        }
    }
}
