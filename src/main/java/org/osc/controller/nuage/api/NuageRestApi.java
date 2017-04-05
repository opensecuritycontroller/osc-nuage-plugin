package org.osc.controller.nuage.api;

import org.osc.sdk.controller.element.VirtualizationConnectorElement;

public class NuageRestApi implements AutoCloseable {
    private final int port;
    protected VirtualizationConnectorElement vc;
    private OSCVSDSession vsdSession ;

    public static final String EMPTY_JSON = "{ }";

    public NuageRestApi(VirtualizationConnectorElement vc, int port) throws Exception {
        this.vc = vc;
        this.port = port;

        //Fix CCL issue ClassUtils.class in Spring
        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            // TODO sridhar: This should come from the VC.isControllerHttps() instead.
            this.vsdSession = getNuageVSDSession(true);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCCL);
        }
    }

    public VirtualizationConnectorElement getVc() {
        return this.vc;
    }

    public OSCVSDSession getNuageVSDSession(boolean isHttps){
        String urlPrefix = (isHttps ? "https"  : "http") + "://" + this.vc.getControllerIpAddress()
        + ":" + this.port;
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
