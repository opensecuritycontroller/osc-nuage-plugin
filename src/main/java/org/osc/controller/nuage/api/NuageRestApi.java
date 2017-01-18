package org.osc.controller.nuage.api;

import org.apache.log4j.Logger;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;

import net.nuagenetworks.vspk.v4_0.VSDSession;

public class NuageRestApi implements AutoCloseable {

    private Logger log = Logger.getLogger(NuageRestApi.class);
    private final int PORT = 8443;
    protected VirtualizationConnectorElement vc;
    private VSDSession vsdSession ;

    public static final String EMPTY_JSON = "{ }";

    public NuageRestApi(){
    }

    public NuageRestApi(VirtualizationConnectorElement vc) throws Exception {
        this.vc = vc;
        //Fix CCL issue ClassUtils.class in Spring
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        this.vsdSession = getNuageVSDSession();
    }

    public VirtualizationConnectorElement getVc() {
        return this.vc;
    }

    public VSDSession getNuageVSDSession(){
        String urlPrefix = "https"  + "://" + this.vc.getControllerIpAddress()
        + (this.PORT > 0 ? ":" + this.PORT : "");
        setVsdSession(new VSDSession(this.vc.getControllerUsername(),
                this.vc.getControllerPassword(), "csp", urlPrefix ));
        return this.vsdSession;
    }

    @Override
    public void close() {
    }

    public VSDSession getVsdSession() {
        return this.vsdSession;
    }

    public VSDSession setVsdSession(VSDSession vsdSession) {
        this.vsdSession = vsdSession;
        return vsdSession;
    }
}
