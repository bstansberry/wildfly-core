package org.jboss.as.test.integration.jmx;


import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;


public class RemJmxLeakTest implements Runnable {

    /**
     * change this to your location
     */
    private static final File JBOSS_CLIENT = new File("/Users/bstansberry/dev/wildfly/wildfly/dist/target/wildfly-11.0.0.Beta1-SNAPSHOT/bin/client/jboss-client.jar");



    public static ClassLoader buildClassLoader() throws Exception {
        ClassLoader loader = new URLClassLoader(new URL[] { JBOSS_CLIENT.toURI().toURL() }, RemJmxLeakTest.class.getClassLoader());
        return loader;
    }

    private final int id;
    private JMXConnector jmxConnector;
    //private MBeanServerConnection serverConnection;

    //private MBeanServer mbeanServer;

    public RemJmxLeakTest(int id) {
        this.id = id;
    }

    public void connect(String url, String user, String pass) throws Exception {

        JMXServiceURL jmxUrl = new JMXServiceURL(url);
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
        String[] credentials = new String[] { user, pass };
        env.put(JMXConnector.CREDENTIALS, credentials);
        this.jmxConnector = JMXConnectorFactory.connect(jmxUrl, env);
        //this.serverConnection = this.jmxConnector.getMBeanServerConnection();
    }

    public void disconnect() throws Exception {
        if (this.jmxConnector != null) {
            this.jmxConnector.close();
            this.jmxConnector = null;
            System.out.println("Disconnected " + id);
        }
    }

    public static void main(String[] args) throws Exception {
        int count = 10000;
        for (int i = 0; i < count; i++) {
            RemJmxLeakTest main = new RemJmxLeakTest(i);
            System.out.println("Starting client thread " + i);
            Thread t = new Thread(main);
            t.setContextClassLoader(buildClassLoader());
            t.start();
            Thread.currentThread().join(50L); // Thread.sleep(...) is clearer
            //t.join(5000L);
        }

    }

    public void run() {
        try {
            // only load some class (verify in visualvm, that it gets GCed)
            //Thread.currentThread().getContextClassLoader().loadClass("org.jboss.remotingjmx.RemotingConnector");

            // connect & disconnect
            //connect("service:jmx:http-remoting-jmx://127.0.0.1:9990", "rhqadmin", "rhqadmin");
            // This variant requires setting up native-management on the server
            connect("service:jmx:remoting-jmx://127.0.0.1:9999", "rhqadmin", "rhqadmin");
            disconnect();
            Thread.currentThread().join(1000L); // not sure why this was here, plus Thread.sleep(1000L) does the same thing
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
