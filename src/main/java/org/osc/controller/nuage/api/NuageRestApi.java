package org.osc.controller.nuage.api;

import org.apache.log4j.Logger;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;

public class NuageRestApi implements AutoCloseable {

    private Logger log = Logger.getLogger(NuageRestApi.class);
    private final int PORT = 8443;
    protected VirtualizationConnectorElement vc;
    private OSCVSDSession vsdSession ;

    public static final String EMPTY_JSON = "{ }";

    public NuageRestApi(){
    }

    public NuageRestApi(VirtualizationConnectorElement vc) throws Exception {
        this.vc = vc;
        //Fix CCL issue ClassUtils.class in Spring
        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            this.vsdSession = getNuageVSDSession();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCCL);
        }
    }

    public VirtualizationConnectorElement getVc() {
        return this.vc;
    }

    public OSCVSDSession getNuageVSDSession(){
        String urlPrefix = "https"  + "://" + this.vc.getControllerIpAddress()
        + (this.PORT > 0 ? ":" + this.PORT : "");
        setVsdSession(new OSCVSDSession(this.vc.getControllerUsername(),
                this.vc.getControllerPassword(), "csp", urlPrefix ));
        return this.vsdSession;
    }

    @Override
    public void close() {
    }

    public OSCVSDSession getVsdSession() {
        return this.vsdSession;
    }

    public OSCVSDSession setVsdSession(OSCVSDSession vsdSession) {
        this.vsdSession = vsdSession;
        return vsdSession;
    }
}
