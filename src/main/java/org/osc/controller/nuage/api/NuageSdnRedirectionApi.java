package org.osc.controller.nuage.api;

import java.util.List;

import org.osc.controller.nuage.api.NuageSdnControllerApi.Config;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osc.sdk.controller.exception.NetworkPortNotFoundException;
import org.springframework.util.CollectionUtils;

public class NuageSdnRedirectionApi implements SdnRedirectionApi {

    private VirtualizationConnectorElement vc;
    @SuppressWarnings("unused")
    private String region;

    private final Config config;

    public NuageSdnRedirectionApi(VirtualizationConnectorElement vc, String region, Config config) {
        this.vc = vc;
        this.region = region;
        this.config = config;
    }

    @Override
    public String installInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort, Long tag,
            TagEncapsulationType encType, Long order, FailurePolicyType failurePolicyType)
                    throws NetworkPortNotFoundException, Exception {
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
            nuageSecApi.installInspectionHook(policyGroup, inspectionPort);
        }

        return null;
    }

    @Override
    public void removeInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort)
            throws Exception {
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())) {
            nuageSecApi.removeInspectionHook(policyGroup, inspectionPort);
        }
    }

    @Override
    public void removeAllInspectionHooks(NetworkElement inspectedPort) throws Exception {

    }

    @Override
    public void setInspectionHookTag(NetworkElement inspectedPort, InspectionPortElement inspectionPort, Long tag)
            throws NetworkPortNotFoundException, Exception {
    }

    @Override
    public Long getInspectionHookTag(NetworkElement inspectedPort, InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        return null;
    }

    @Override
    public void setInspectionHookFailurePolicy(NetworkElement inspectedPort, InspectionPortElement inspectionPort,
            FailurePolicyType failurePolicyType) throws NetworkPortNotFoundException, Exception {

    }

    @Override
    public FailurePolicyType getInspectionHookFailurePolicy(NetworkElement inspectedPort,
            InspectionPortElement inspectionPort) throws NetworkPortNotFoundException, Exception {
        return null;
    }

    @Override
    public InspectionHookElement getInspectionHook(NetworkElement inspectedPort, InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        return null;
    }

    @Override
    public void updateInspectionHook(InspectionHookElement inspectionHook)
            throws NetworkPortNotFoundException, Exception {

    }

    @Override
    public void updateInspectionHook(NetworkElement inspectedPort, InspectionPortElement inspectionPort, Long tag,
            TagEncapsulationType encType, Long order, FailurePolicyType failurePolicyType)
            throws NetworkPortNotFoundException, Exception {

    }

    @Override
    public InspectionPortElement getInspectionPort(InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        String domainId = null;
        if (inspectionPort != null && inspectionPort.getIngressPort() != null) {
            domainId = inspectionPort.getIngressPort().getParentId();
            try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
                return nuageSecApi.getRedirectionTarget(inspectionPort, domainId);
            }
        }
        return null;
    }

    @Override
    public void setInspectionHookOrder(NetworkElement inspectedPort, InspectionPortElement inspectionPort, Long order)
            throws NetworkPortNotFoundException, Exception {
    }

    @Override
    public Long getInspectionHookOrder(NetworkElement inspectedPort, InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        return null;
    }

    @Override
    public NetworkElement registerNetworkElement(List<NetworkElement> elements) throws Exception {
        String domainId = CollectionUtils.isEmpty(elements) ? null : elements.get(0).getParentId();
        NetworkElement policyGroup = null;
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
            policyGroup = nuageSecApi.createPolicyGroup(elements, domainId);
        }
        return policyGroup;
    }

    @Override
    public void deleteNetworkElement(NetworkElement policyGroup) throws Exception {
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
            nuageSecApi.deletePolicyGroup(policyGroup);
        }
    }

    @Override
    public List<NetworkElement> getNetworkElements(NetworkElement element) throws Exception {
        return null;
    }

    @Override
    public NetworkElement updateNetworkElement(NetworkElement policyGroup, List<NetworkElement> protectedPorts)
            throws Exception {
        String domainId = CollectionUtils.isEmpty(protectedPorts) ? null : protectedPorts.get(0).getParentId();
        try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
            return nuageSecApi.updatePolicyGroup(policyGroup, protectedPorts, domainId);
        }
    }

    @Override
    public void registerInspectionPort(InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        String domainId = null;
        if (inspectionPort != null && inspectionPort.getIngressPort() != null) {
            domainId = inspectionPort.getIngressPort().getParentId();
            try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())){
                nuageSecApi.createRedirectionTarget(inspectionPort, domainId);
            }
        }
    }

    public void removeInspectionPort( InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        String domainId = null;
        if (inspectionPort != null && inspectionPort.getIngressPort() != null) {
            domainId = inspectionPort.getIngressPort().getParentId();
            try (NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc, this.config.port())) {
                nuageSecApi.deleteInspectionPort(domainId, inspectionPort);
            }
        }

    }

    @Override
    public void close() throws Exception {

    }

}
