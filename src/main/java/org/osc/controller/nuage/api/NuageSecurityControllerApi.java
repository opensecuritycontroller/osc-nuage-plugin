package org.osc.controller.nuage.api;

import java.io.Closeable;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;

import net.nuagenetworks.vspk.v4_0.Enterprise;
import net.nuagenetworks.vspk.v4_0.Me;
import net.nuagenetworks.vspk.v4_0.VSDSession;

public class NuageSecurityControllerApi implements Closeable {
    private Logger log = Logger.getLogger(NuageSecurityControllerApi.class);

    private NuageRestApi nuageRestApi = null;

    public NuageSecurityControllerApi(VirtualizationConnectorElement vc) throws Exception {
         this.nuageRestApi = new NuageRestApi(vc);
    }

    public void test() throws Exception {
        VSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();
        Enterprise enterprise = me.getEnterprises().getFirst();
    }

    @Override
    public void close() throws IOException {
        this.nuageRestApi.getVsdSession().reset();
    }
}
