package org.osc.controller.nuage.api;

import static org.osc.sdk.controller.Constants.*;

import java.util.HashMap;

import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osc.sdk.controller.Status;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        configurationPid = "org.osc.controller.nuage.SdnController",
        property = {
                PLUGIN_NAME + "=Nuage",
                SUPPORT_OFFBOX_REDIRECTION + ":Boolean=true",
                SUPPORT_SFC + ":Boolean=false",
                SUPPORT_FAILURE_POLICY + ":Boolean=false",
                USE_PROVIDER_CREDS + ":Boolean=false",
                QUERY_PORT_INFO + ":Boolean=false",
                SUPPORT_PORT_GROUP + ":Boolean=true" })
public class NuageSdnControllerApi implements SdnControllerApi {

    private Config config;

    @SuppressWarnings("unused")
    private String region;

    @ObjectClassDefinition
    @interface Config {
        @AttributeDefinition(
                min = "0",
                max = "65535",
                required = false,
                description = "The port to use when connecting to Nuage instances. "
                        + "The value '0' indicates that a default port of '443' (or '80' if HTTPS is not enabled) should be used."
                        + "If not provided the default value of the port will be '8443' for Nuage environments.")
        int port() default 8443;
    }

    public NuageSdnControllerApi() {
    }

    @Activate
    void start(Config config) {
        this.config = config;
    }

    @Override
    public Status getStatus(VirtualizationConnectorElement vc, String region) throws Exception {
        Status status = null;
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(vc, this.config.port())) {
            nuageSecApi.test();
        }
        status = new Status("Nuage", "0.1", true);
        return status;
    }

    @Override
    public SdnRedirectionApi createRedirectionApi(VirtualizationConnectorElement vc, String region) {
        return new NuageSdnRedirectionApi(vc, region, this.config);
    }

    @Override
    public HashMap<String, FlowPortInfo> queryPortInfo(VirtualizationConnectorElement vc, String region,
            HashMap<String, FlowInfo> portsQuery) throws Exception {
        throw new UnsupportedOperationException("Nuage SDN Controller does not support flow based query");
    }

    @Override
    public void close() throws Exception {
    }

}
