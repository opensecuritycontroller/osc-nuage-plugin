package org.osc.controller.nuage;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class AbstractNuageTest {
    Logger log = LoggerFactory.getLogger(AbstractNuageTest.class);

    private Server server;
    protected int serverPort;

    @Path("/nuage/api/v4_0")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TestResource {
        @GET
        @Path("/me")
        @Produces(MediaType.APPLICATION_JSON)
        public Response test() {
            return Response.ok("[{}]", MediaType.APPLICATION_JSON).build();
        }

        @GET
        @Path("/enterprises")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getEnterprises() {
            return Response.ok("[{}]", MediaType.APPLICATION_JSON).build();
        }
    }

    @Before
    public void setupServer() {
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(0).build();
        ResourceConfig config = new ResourceConfig(TestResource.class, JacksonJaxbJsonProvider.class);
        this.server = JettyHttpContainerFactory.createServer(baseUri, config);

        this.serverPort = ((ServerConnector)this.server.getConnectors()[0]).getLocalPort();
    }

    @After
    public void tearDownServer() throws Exception {
        this.server.stop();
    }
}