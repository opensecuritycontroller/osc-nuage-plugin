package org.osc.controller.nuage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.PathUtils;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OSGiIntegrationTest extends AbstractNuageTest {
    @Inject
    ConfigurationAdmin configAdmin;

    @Inject
    BundleContext context;

    private ServiceTracker<SdnControllerApi, SdnControllerApi> tracker;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {

        return options(
                // Load the current module from its built classes so we get the latest from Eclipse
                bundle("reference:file:" + PathUtils.getBaseDir() + "/target/classes/"),

                // And some dependencies
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),

                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                // Fragment bundles cannot be started
                mavenBundle("org.slf4j", "slf4j-simple").versionAsInProject().noStart(),

                mavenBundle("commons-logging", "commons-logging").versionAsInProject(),
                mavenBundle("org.apache.directory.studio", "org.apache.commons.lang").versionAsInProject(),
                mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-annotations").versionAsInProject(),
                mavenBundle("org.osc.api", "sdn-controller-api").versionAsInProject(),

                mavenBundle("org.glassfish.jersey.bundles.repackaged", "jersey-guava").versionAsInProject(),
                mavenBundle("org.glassfish.hk2", "hk2-api").versionAsInProject(),
                mavenBundle("org.glassfish.hk2", "hk2-utils").versionAsInProject(),
                mavenBundle("org.glassfish.hk2", "osgi-resource-locator").versionAsInProject(),
                mavenBundle("org.glassfish.hk2.external", "aopalliance-repackaged").versionAsInProject(),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance").versionAsInProject(),


                mavenBundle("com.fasterxml.jackson.core", "jackson-core").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-databind").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.jaxrs", "jackson-jaxrs-json-provider").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.jaxrs", "jackson-jaxrs-base").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.module", "jackson-module-jaxb-annotations").versionAsInProject(),

                mavenBundle("org.codehaus.woodstox", "stax2-api").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml").versionAsInProject(),

                mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("javax.ws.rs", "javax.ws.rs-api").versionAsInProject(),

                // Just needed for the test so we can configure the client to point at the local test server
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.10"),

                // Needed for testing and the test server
                mavenBundle("org.glassfish.jersey.core", "jersey-server").versionAsInProject(),
                mavenBundle("org.glassfish.jersey.core", "jersey-common").versionAsInProject(),
                mavenBundle("org.glassfish.jersey.core", "jersey-client").versionAsInProject(),
                mavenBundle("javax.servlet", "javax.servlet-api").versionAsInProject(),
                mavenBundle("javax.annotation", "javax.annotation-api").versionAsInProject(),
                mavenBundle("javax.validation", "validation-api").versionAsInProject(),
                mavenBundle("org.glassfish.hk2", "hk2-locator").versionAsInProject(),
                mavenBundle("org.javassist", "javassist").versionAsInProject(),
                mavenBundle("org.glassfish.jersey.containers", "jersey-container-jetty-http").versionAsInProject(),
                mavenBundle("org.eclipse.jetty", "jetty-http").versionAsInProject(),
                mavenBundle("org.eclipse.jetty", "jetty-server").versionAsInProject(),
                mavenBundle("org.eclipse.jetty", "jetty-util").versionAsInProject(),
                mavenBundle("org.eclipse.jetty", "jetty-io").versionAsInProject(),
                mavenBundle("org.eclipse.jetty", "jetty-continuation").versionAsInProject(),

                // Uncomment this line to allow remote debugging
                // CoreOptions.vmOption("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044"),

                junitBundles()
                );
    }

    @Before
    public void setup() throws IOException {
        this.tracker = new ServiceTracker<>(this.context, SdnControllerApi.class, null);
        this.tracker.open();

        Configuration configuration = this.configAdmin.getConfiguration("org.osc.controller.nuage.SdnController", "?");

        Dictionary<String, Object> config = new Hashtable<>();
        config.put("port", this.serverPort);
        config.put("testing", true);

        configuration.update(config);

        // Set up a tracker which only picks up "testing" services
        this.tracker = new ServiceTracker<SdnControllerApi, SdnControllerApi>(this.context,
                SdnControllerApi.class, null) {
            @Override
            public SdnControllerApi addingService(ServiceReference<SdnControllerApi> ref) {
                if(Boolean.TRUE.equals(ref.getProperty("testing"))) {
                    return this.context.getService(ref);
                }
                return null;
            }
        };

        this.tracker.open();
    }

    @Test
    public void testRegistered() throws InterruptedException {
        SdnControllerApi service = this.tracker.waitForService(5000);
        assertNotNull(service);

        ServiceObjects<SdnControllerApi> so = this.context.getServiceObjects(this.tracker.getServiceReference());

        SdnControllerApi objectA = so.getService();
        SdnControllerApi objectB = so.getService();

        assertSame(objectA, objectB);
    }

    /**
     * This test doesn't really validate much, it would be better if
     * we could start a simple local server to connect to...
     * @throws Exception
     */
    @Test
    public void testConnect() throws Exception {
        SdnControllerApi service = this.tracker.waitForService(5000);
        assertNotNull(service);

        ServiceObjects<SdnControllerApi> so = this.context.getServiceObjects(this.tracker.getServiceReference());

        SdnControllerApi object = so.getService();

        object.getStatus(new VirtualizationConnectorElement() {

            @Override
            public boolean isProviderHttps() {
                return false;
            }

            @Override
            public boolean isControllerHttps() {
                return false;
            }

            @Override
            public String getProviderUsername() {
                return "user";
            }

            @Override
            public String getProviderPassword() {
                return "password";
            }

            @Override
            public String getProviderIpAddress() {
                return "127.0.0.1";
            }

            @Override
            public Map<String, String> getProviderAttributes() {
                return new HashMap<>();
            }

            @Override
            public String getProviderAdminTenantName() {
                return "bar";
            }

            @Override
            public String getName() {
                return "baz";
            }

            @Override
            public String getControllerUsername() {
                return "fizz";
            }

            @Override
            public String getControllerPassword() {
                return "buzz";
            }

            @Override
            public String getControllerIpAddress() {
                return "127.0.0.1";
            }

            @Override
            public SSLContext getSslContext() {
                try {
                    return SSLContext.getDefault();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public TrustManager[] getTruststoreManager() throws Exception {
                return null;
            }

			@Override
			public String getProviderAdminDomainId() {
				return "default";
			}
        }, "foo");
    }
}
