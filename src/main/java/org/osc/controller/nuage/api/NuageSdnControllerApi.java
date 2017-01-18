package org.osc.controller.nuage.api;

import java.util.HashMap;
import java.util.List;

import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.FlowInfo;
import org.osc.sdk.controller.FlowPortInfo;
import org.osc.sdk.controller.Status;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.osc.sdk.controller.exception.NetworkPortNotFoundException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope=ServiceScope.PROTOTYPE, property="osc.plugin.name=Nuage")
public class NuageSdnControllerApi implements SdnControllerApi {

    private VirtualizationConnectorElement vc;
    @SuppressWarnings("unused")
    private String region;

    public NuageSdnControllerApi() {
    }

    public NuageSdnControllerApi(VirtualizationConnectorElement vc, String region) throws Exception {
        this.vc = vc;
        this.region = region;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Status getStatus() throws Exception {
        Status status = null;
        try(NuageSecurityControllerApi nuageSecApi = new NuageSecurityControllerApi(this.vc); ) {
            nuageSecApi.test();
        }
        status = new Status(getName(), getVersion(), true);
        return status;
    }

    @Override
    public void installInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort, Long tag,
            TagEncapsulationType encType, Long order, FailurePolicyType failurePolicyType)
            throws NetworkPortNotFoundException, Exception {

    }

    @Override
    public void removeInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort)
            throws Exception {
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
    public InspectionHookElement getInspectionHook(NetworkElement inspectedPort,
            InspectionPortElement inspectionPort) throws NetworkPortNotFoundException, Exception {
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
        return null;
    }

    @Override
    public void setInspectionHookOrder(NetworkElement inspectedPort, InspectionPortElement inspectionPort,
            Long order) throws NetworkPortNotFoundException, Exception {
    }

    @Override
    public Long getInspectionHookOrder(NetworkElement inspectedPort, InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
        return null;
    }

    @Override
    public String getName() {
        return "Nuage";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public void setVirtualizationConnector(VirtualizationConnectorElement vc) {
        this.vc = vc;

    }

    @Override
    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public boolean isOffboxRedirectionSupported() {
        return true;
    }

    @Override
    public boolean isServiceFunctionChainingSupported() {
        return false;
    }

    @Override
    public boolean isFailurePolicySupported() {
        return false;
    }

    @Override
    public boolean isUsingProviderCreds() {
        return false;
    }

    @Override
    public boolean isSupportQueryPortInfo() {
        return false;
    }

    @Override
    public HashMap<String, FlowPortInfo> queryPortInfo(HashMap<String, FlowInfo> portsQuery) throws Exception {
        throw new UnsupportedOperationException("Nuage SDN Controller does not support flow based query");
    }

    @Override
    public NetworkElement registerNetworkElement(List<NetworkElement> elements) throws Exception {
        return null;
    }

    @Override
    public void deleteNetworkElement(NetworkElement policyGroup) throws Exception {
    }

    @Override
    public List<NetworkElement> getNetworkElements(NetworkElement element) throws Exception {
        return null;
    }

    @Override
    public NetworkElement updateNetworkElement(NetworkElement policyGroup, List<NetworkElement> childElements)
            throws Exception {
        return null;
    }

    @Override
    public void registerInspectionPort(InspectionPortElement inspectionPort)
            throws NetworkPortNotFoundException, Exception {
    }

    @Override
    public  boolean isPortGroupSupported() {
        return true;
    }

}
