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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostControllerService;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

/**
 * Proof of concept of a service that integrates JGroups based group communication
 * with the intra-domain communication layer. This service creates a {@link Channel},
 * which is done in a fairly realistic manner, and then uses it to send string messages
 * around the group, writing them to the log on receipt.
 *
 * @author Brian Stansberry
 */
public class JGroupsExperimentService implements Service<Void> {

    private static final Logger log = Logger.getLogger(JGroupsExperimentService.class.getCanonicalName());

    private static final ServiceName SERVICE_NAME = HostControllerService.HC_SERVICE_NAME.append("jgroups-experiment");

    private final InjectedValue<DomainServer> domainServerInjector = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorServiceInjector = new InjectedValue<>();

    private final LocalHostControllerInfo localHostInfo;
    private Channel channel;
    private ScheduledFuture<?> future;

    public static void install(ServiceTarget target,
                               LocalHostControllerInfo localHostInfo) {

        JGroupsExperimentService service = new JGroupsExperimentService(localHostInfo);
        ServiceBuilder<Void> builder = target.addService(SERVICE_NAME, service)
                .addDependency(DomainServerService.SERVICE_NAME, DomainServer.class, service.domainServerInjector)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, service.executorServiceInjector)
                .addDependency(HostControllerService.HC_SCHEDULED_EXECUTOR_SERVICE_NAME, ScheduledExecutorService.class, service.scheduledExecutorServiceInjector);
        builder.install();
    }

    private JGroupsExperimentService(LocalHostControllerInfo localHostInfo) {
        this.localHostInfo = localHostInfo;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        final Runnable asyncTask;
        try {
            DomainServer server = domainServerInjector.getValue();
            // Build a JChannel to use the domain-stack.xml config in this package
            String path = getStackConfigPath();
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            ProtocolStackConfigurator psc = XmlConfigurator.getInstance(is);
            // Create the protocols, but then swap out discovery and transport with our own
            // This funky approach removes the need for JGroups module to be able to see
            // the DomainDiscovery and DomainTP classes.
            List<Protocol> list = Configurator.createProtocols(psc.getProtocolStack(), new ProtocolStack());
            for (int i = 0; i < list.size(); i++) {
                Protocol protocol = list.get(i);
                if (protocol instanceof TP) {
                    list.set(i, new DomainTP(server));
                } else if (protocol instanceof Discovery) {
                    list.set(i, new DomainDiscovery(localHostInfo));
                }
            }
            Channel jChannel = new JChannel(list);

            jChannel.setReceiver(new Receiver() {
                @Override
                public void viewAccepted(View new_view) {
                    log.infof("New view %s", new_view);
                }

                @Override
                public void suspect(Address suspected_mbr) {
                    log.infof("%s suspected", suspected_mbr);
                }

                @Override
                public void block() {
                }

                @Override
                public void unblock() {
                }

                @Override
                public void receive(Message msg) {
                    log.infof("Received: %s", msg.toStringAsObject());
                }

                @Override
                public void getState(OutputStream output) throws Exception {
                }

                @Override
                public void setState(InputStream input) throws Exception {
                }
            });

            asyncTask = createStartTask(context, jChannel);
        } catch (Exception e) {
            throw new StartException(e);
        }

        context.asynchronous();
        try {
            executorServiceInjector.getValue().execute(asyncTask);
        } catch (RejectedExecutionException r) {
            asyncTask.run();
        }

    }

    private Runnable createStartTask(final StartContext startContext, final Channel jchannel) {
        return new Runnable() {
            @Override
            public void run() {

                try {
                    // TODO this should really be the domain name, but that comes from domain.xml,
                    // and in a real, non-POC scenario this service would run before we parse domain.xml!
                    final String channelName = "domain-experiment";

                    jchannel.connect(channelName);
                    channel = jchannel;

                    future = scheduledExecutorServiceInjector.getValue().scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            Channel ourChannel = JGroupsExperimentService.this.channel;
                            if (ourChannel != null) {
                                try {
                                    channel.send(new Message(null, String.format("Host %s is master? %s",
                                            localHostInfo.getLocalHostName(), localHostInfo.isMasterDomainController())));
                                } catch (Exception e) {
                                    log.error("Failed sending JGroups experiment message", e);
                                }
                            }
                        }
                    }, 1, 5, TimeUnit.SECONDS);

                    startContext.complete();

                } catch (Exception e) {
                    startContext.failed(new StartException(e));
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };

    }


    @Override
    public synchronized void stop(StopContext context) {

        Runnable asyncTask = new Runnable() {
            @Override
            public void run() {
                try {
                    if (future != null) {
                        future.cancel(false);
                    }
                    StreamUtils.safeClose(channel);
                } finally {
                    context.complete();
                }
            }
        };

        context.asynchronous();
        try {
            executorServiceInjector.getValue().execute(asyncTask);
        } catch (RejectedExecutionException r) {
            asyncTask.run();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private String getStackConfigPath() {
        Package ourpack = getClass().getPackage();
        return ourpack.getName().replace(".", "/") + "/domain-stack.xml";
    }
}
