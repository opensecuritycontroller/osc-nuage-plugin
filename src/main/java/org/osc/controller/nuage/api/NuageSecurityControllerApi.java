package org.osc.controller.nuage.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;
import org.springframework.util.CollectionUtils;

import net.nuagenetworks.bambou.RestException;
import net.nuagenetworks.vspk.v4_0.Domain;
import net.nuagenetworks.vspk.v4_0.Enterprise;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdEntryTemplate;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdEntryTemplate.Action;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdEntryTemplate.LocationType;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdEntryTemplate.NetworkType;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdEntryTemplate.UplinkPreference;
import net.nuagenetworks.vspk.v4_0.IngressAdvFwdTemplate;
import net.nuagenetworks.vspk.v4_0.Job;
import net.nuagenetworks.vspk.v4_0.Job.Status;
import net.nuagenetworks.vspk.v4_0.Me;
import net.nuagenetworks.vspk.v4_0.PolicyGroup;
import net.nuagenetworks.vspk.v4_0.RedirectionTarget;
import net.nuagenetworks.vspk.v4_0.RedirectionTarget.EndPointType;
import net.nuagenetworks.vspk.v4_0.VPort;
import net.nuagenetworks.vspk.v4_0.VPort.AddressSpoofing;
import net.nuagenetworks.vspk.v4_0.fetchers.DomainsFetcher;
import net.nuagenetworks.vspk.v4_0.fetchers.IngressAdvFwdEntryTemplatesFetcher;
import net.nuagenetworks.vspk.v4_0.fetchers.IngressAdvFwdTemplatesFetcher;
import net.nuagenetworks.vspk.v4_0.fetchers.RedirectionTargetsFetcher;
import net.nuagenetworks.vspk.v4_0.fetchers.VPortsFetcher;

public class NuageSecurityControllerApi implements Closeable {
    private Logger log = Logger.getLogger(NuageSecurityControllerApi.class);

    private NuageRestApi nuageRestApi = null;

    public NuageSecurityControllerApi(VirtualizationConnectorElement vc, int port) throws Exception {
        this.nuageRestApi = new NuageRestApi(vc, port);
    }

    public void test() throws Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();
        me.getEnterprises().getFirst();
    }

    public NetworkElement createPolicyGroup(List<NetworkElement> elements, String domainId) throws RestException{
        DefaultNetworkPort portGroup = new DefaultNetworkPort();
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();
        Enterprise enterprise = me.getEnterprises().getFirst();

        Domain selectDomain = null;
        DomainsFetcher fetcher = new DomainsFetcher(enterprise);
        String filter = String.format("name like '%s'", domainId);
        List<Domain> dms = fetcher.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(dms)) {
            selectDomain = dms.get(0);
            PolicyGroup pg = new PolicyGroup();
            pg.setName("PolicyGroup-"+UUID.randomUUID().toString());
            selectDomain.createChild(pg);
            portGroup.setElementId(pg.getId());
            List<VPort> vports =new ArrayList<>();
            for (NetworkElement element: elements){
                List<VPort> temp =new ArrayList<>();
                //protected VM OS ID
                filter = String.format("name like '%s'", element.getElementId());
                VPortsFetcher vportFet = selectDomain.getVPorts();
                temp  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
                if (!CollectionUtils.isEmpty(temp)) {
                    vports.add(temp.get(0));
                }
            }
            pg.assign(vports);
        }
        return portGroup;
    }

    public NetworkElement updatePolicyGroup(NetworkElement policyGroup, List<NetworkElement> protectedPorts,
            String domainId) throws RestException{
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        PolicyGroup polGrp = null;
        try {
            polGrp = new PolicyGroup();
            polGrp.setId(policyGroup.getElementId());
            polGrp.fetch();
        } catch (Exception e) {
            polGrp = null;
        }
        if (polGrp == null){
            return createPolicyGroup(protectedPorts, domainId);
        } else {
            //handle Security Group update add/remove workload VMs
            return null;
        }
    }

    public void deletePolicyGroup(NetworkElement policyGroup) throws RestException {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        PolicyGroup pg = null;
        try {
            pg = new PolicyGroup();
            pg.setId(policyGroup.getElementId());
            pg.fetch();
        } catch (Exception e) {
            String format = String.format("PolicyGroup with ID '%s' not found" , policyGroup.getElementId());
            this.log.debug(format);
            return;
        }
        //get FWD policy for pg and delete it after deleting pg
        String selectDomainId = pg.getParentId();
        Domain selectDomain = new Domain();
        selectDomain.setId(selectDomainId);
        selectDomain.fetch();

        List<IngressAdvFwdTemplate> fwdPolicies = new ArrayList<>();
        IngressAdvFwdTemplatesFetcher advFwdFetcher = selectDomain.getIngressAdvFwdTemplates();
        List<IngressAdvFwdTemplate> list = advFwdFetcher.fetch();
        String filter = String.format("networkID like '%s' or locationID like '%s'", pg.getId(), pg.getId());
        for (IngressAdvFwdTemplate fwdPolicy : list){
            IngressAdvFwdEntryTemplatesFetcher entryFetcher = fwdPolicy.getIngressAdvFwdEntryTemplates();
            List<IngressAdvFwdEntryTemplate> entries = entryFetcher.fetch(filter, null, null, null, null, null, false);
            if (!entries.isEmpty()){
                fwdPolicies.add(fwdPolicy);
            }
        }
        for (IngressAdvFwdTemplate fwdPolicy : fwdPolicies){
            fwdPolicy.delete();
        }
        //unassign all VPorts
        VPortsFetcher vpFetcher = pg.getVPorts();
        for (VPort vport : vpFetcher.fetch()){
            PolicyGroup removeThis = null;
            for (PolicyGroup pgFor : vport.getPolicyGroups().fetch()){
                if (pgFor.getId().equals(pg.getId())){
                    removeThis = pgFor;
                    break;
                }
            }
            vport.getPolicyGroups().remove(removeThis);
            vport.assign(vport.getPolicyGroups());
        }
        pg.delete();
    }

    private IngressAdvFwdTemplate getForwardingPolicy(Domain selectDomain, PolicyGroup polGrp) throws RestException {
        if (selectDomain != null){
            String filter = String.format("active like '%s'", true);
            IngressAdvFwdTemplatesFetcher advFwdFetcher = selectDomain.getIngressAdvFwdTemplates();
            List<IngressAdvFwdTemplate> advFwdPolicies = advFwdFetcher.fetch(filter, null, null, null, null, null, false);
            if (!CollectionUtils.isEmpty(advFwdPolicies)){
                IngressAdvFwdTemplate fwdPolicy = advFwdPolicies.get(0);
                return fwdPolicy;
            }
        }
        return null;
    }

    private IngressAdvFwdTemplate createNewForwardingPolicy(Domain selectDomain) throws RestException {
        if (selectDomain != null){
            //create forwarding policy
            IngressAdvFwdTemplate fwdPolicy = new IngressAdvFwdTemplate();
            fwdPolicy.setActive(true);
            fwdPolicy.setPriority(1L);
            fwdPolicy.setName("FwdPolicy-" + UUID.randomUUID().toString());
            selectDomain.createChild(fwdPolicy);
            return fwdPolicy;
        }
        return null;
    }

    public void createRedirectionTarget(InspectionPortElement inspectionPort, String domainId) throws Exception{

        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();

        Enterprise enterprise = me.getEnterprises().getFirst();

        Domain selectDomain = null;
        DomainsFetcher fetcher = new DomainsFetcher(enterprise);
        String filter = String.format("name like '%s'", domainId);
        List<Domain> dms = fetcher.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(dms)) {
            selectDomain = dms.get(0);
            //create redirection target if not exists for inspection ports
            //single inspection interface
            if (inspectionPort.getIngressPort().getElementId().equals(inspectionPort.getEgressPort().getElementId())){
                createRedirectionTargetAndAssignVPorts("RT-IngAndEgr-" + UUID.randomUUID().toString(),
                        inspectionPort.getIngressPort().getElementId(), selectDomain);
            } else {
                //dual inspection interface
                createRedirectionTargetAndAssignVPorts("RT-Ingress-" + UUID.randomUUID().toString(),
                        inspectionPort.getIngressPort().getElementId(), selectDomain);
                createRedirectionTargetAndAssignVPorts("RT-Egress-" + UUID.randomUUID().toString(),
                        inspectionPort.getEgressPort().getElementId(), selectDomain);
            }
        }
    }

    private void createRedirectionTargetAndAssignVPorts(String rtName, String inspectionPortOSId, Domain selectDomain)
            throws RestException {
        String filter;

        RedirectionTarget rt = new RedirectionTarget();
        rt.setName(rtName);
        rt.setEndPointType(EndPointType.VIRTUAL_WIRE);
        rt.setRedundancyEnabled(false);
        selectDomain.createChild(rt);

        filter = String.format("name like '%s'", inspectionPortOSId);
        VPortsFetcher vportFet = selectDomain.getVPorts();
        List<VPort> vports  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        // set source address spoofing to enabled on inspection vport
        if (!CollectionUtils.isEmpty(vports)){
            VPort vport = vports.get(0);
            vport.setAddressSpoofing(AddressSpoofing.ENABLED);
            rt.assign(vports);
        }

    }

    public InspectionPortElement getRedirectionTarget(InspectionPortElement inspectionPort, String domainId) throws Exception{
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();
        Enterprise enterprise = me.getEnterprises().getFirst();

        Domain selectDomain = null;
        DomainsFetcher fetcher = new DomainsFetcher(enterprise);
        String filter = String.format("name like '%s'", domainId);
        List<Domain> dms = fetcher.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(dms)){
            selectDomain = dms.get(0);
            String ingrInspectionPortOSId = inspectionPort.getIngressPort().getElementId(),
                    egrInspectionPortOSId = inspectionPort.getEgressPort().getElementId();
            if (!isRedirectionTargetRegistered(ingrInspectionPortOSId, selectDomain)){
                this.log.info("Inspection port ingress: '" + ingrInspectionPortOSId + "' not registered.");
                return null;
            }

            if (!isRedirectionTargetRegistered(egrInspectionPortOSId, selectDomain)){
                this.log.info("Inspection port egress: '" + egrInspectionPortOSId + "' not registered.");
                return null;
            }
            return new DefaultInspectionPort(
                    new DefaultNetworkPort(ingrInspectionPortOSId, null),
                    new DefaultNetworkPort(egrInspectionPortOSId, null));

        }
        return null;
    }

    private boolean isRedirectionTargetRegistered(String inspectionPortOSId, Domain selectDomain)
            throws RestException {
        String filter = String.format("name like '%s'", inspectionPortOSId);
        VPortsFetcher vportFet = selectDomain.getVPorts();
        List<VPort> vports  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(vports)){
            RedirectionTargetsFetcher rtFetch = vports.get(0).getRedirectionTargets();
            RedirectionTarget rt = rtFetch.getFirst();
            if (rt != null){
                return true;
            }
        }
        return false;
    }

    public void installInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort)
            throws RestException, IOException, Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        PolicyGroup fetchPG = new PolicyGroup();
        fetchPG.setId(policyGroup.getElementId());
        fetchPG.fetch();
        if (fetchPG != null){
            String selectDomainId = fetchPG.getParentId();
            Domain selectDomain = new Domain();
            selectDomain.setId(selectDomainId);
            selectDomain.fetch();
            beginPolicyChanges(selectDomain);
            IngressAdvFwdTemplate fwdPolicy = null;
            fwdPolicy = getForwardingPolicy(selectDomain, fetchPG);
            if (fwdPolicy == null){
                fwdPolicy = createNewForwardingPolicy( selectDomain);
                createFwdPolicyIngressEgressEntries(fetchPG, fwdPolicy, inspectionPort, selectDomain);
            }
            applyPolicyChanges(selectDomain);
        }
    }

    private void createFwdPolicyIngressEgressEntries(PolicyGroup policyGrp, IngressAdvFwdTemplate fwdPolicy,
            InspectionPortElement inspectionPort, Domain selectDomain )
                    throws RestException
    {
        RedirectionTarget rtIngress = null, rtEgress = null;
        if (fwdPolicy != null){
            if (inspectionPort != null){
                NetworkElement ingressPort = inspectionPort.getIngressPort();
                NetworkElement egressPort = inspectionPort.getEgressPort();

                String filter = String.format("name like '%s'", ingressPort.getElementId());
                VPortsFetcher vportFet = selectDomain.getVPorts();
                List<VPort> vportsIng  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
                VPort ingress = null;
                if (!vportsIng.isEmpty()){
                    ingress = vportsIng.get(0);

                    List<RedirectionTarget> rtIngresses = ingress.getRedirectionTargets().get();
                    if (!CollectionUtils.isEmpty(rtIngresses)){
                        rtIngress = rtIngresses.get(0);

                        if (inspectionPort.getIngressPort().getElementId().equals(
                                inspectionPort.getEgressPort().getElementId())){
                            rtEgress = rtIngress;
                        } else {
                            VPort egress = null;
                            filter = String.format("name like '%s'", egressPort.getElementId());
                            vportFet = selectDomain.getVPorts();
                            List<VPort> vports  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
                            if (!vports.isEmpty()){
                                egress = vports.get(0);
                                List<RedirectionTarget> rtEgresses = egress.getRedirectionTargets().get();
                                if (!CollectionUtils.isEmpty(rtEgresses)){
                                    rtEgress = rtEgresses.get(0);
                                }
                            }
                        }

                        //create forwarding policy ingress Entry
                        IngressAdvFwdEntryTemplate ingEntry = new IngressAdvFwdEntryTemplate();
                        ingEntry.setUplinkPreference(UplinkPreference.PRIMARY_SECONDARY);
                        ingEntry.setName("ingEntry-" + UUID.randomUUID().toString());
                        ingEntry.setNetworkType(NetworkType.POLICYGROUP);
                        ingEntry.setNetworkID(policyGrp.getId());
                        ingEntry.setLocationType(LocationType.ANY);
                        ingEntry.setProtocol("ANY");//Any
                        ingEntry.setDescription("Forwarding Policy Ingress Entry");
                        ingEntry.setAction(Action.REDIRECT);
                        ingEntry.setRedirectVPortTagID(rtIngress.getId());
                        fwdPolicy.createChild(ingEntry);

                        //create forwarding policy egress Entry
                        IngressAdvFwdEntryTemplate egrEntry = new IngressAdvFwdEntryTemplate();
                        egrEntry.setUplinkPreference(UplinkPreference.PRIMARY_SECONDARY);
                        egrEntry.setName("egrEntry-" + UUID.randomUUID().toString());
                        egrEntry.setNetworkType(NetworkType.ANY);
                        egrEntry.setNetworkID(null);
                        egrEntry.setLocationType(LocationType.POLICYGROUP);
                        egrEntry.setLocationID(policyGrp.getId());
                        egrEntry.setProtocol("ANY");//Any
                        egrEntry.setDescription("Forwarding Policy Egress Entry");
                        egrEntry.setAction(Action.REDIRECT);
                        egrEntry.setRedirectVPortTagID(rtEgress.getId());
                        fwdPolicy.createChild(egrEntry);
                    }
                }
            }
        }
    }


    public void removeInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort)
            throws RestException, IOException, Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        PolicyGroup fetchPG = new PolicyGroup();
        fetchPG.setId(policyGroup.getElementId());
        fetchPG.fetch();
        if (fetchPG != null){
            String selectDomainId = fetchPG.getParentId();
            Domain selectDomain = new Domain();
            selectDomain.setId(selectDomainId);
            selectDomain.fetch();
            IngressAdvFwdTemplatesFetcher fetcher = selectDomain.getIngressAdvFwdTemplates();
            String filter = String.format("parentID like '%s' and active == '%s'", selectDomainId, true);
            List<IngressAdvFwdTemplate> fwdPolicies = fetcher.fetch(filter, null, null, null, null, null, Boolean.FALSE);
            if (!CollectionUtils.isEmpty(fwdPolicies)){
                IngressAdvFwdTemplate activeFwdPolicy = fwdPolicies.get(0);
                activeFwdPolicy.delete();
            }

        }
    }

    public void deleteInspectionPort(NetworkElement policyGroup, InspectionPortElement inspPort) throws Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        PolicyGroup fetchPG = new PolicyGroup();
        fetchPG.setId(policyGroup.getElementId());
        fetchPG.fetch();
        if (fetchPG != null){
            String selectDomainId = fetchPG.getParentId();
            Domain selectDomain = new Domain();
            selectDomain.setId(selectDomainId);
            selectDomain.fetch();


            String ingrInspectionPortOSId = inspPort.getIngressPort().getElementId(),
                    egrInspectionPortOSId = inspPort.getEgressPort().getElementId();
            if (ingrInspectionPortOSId != null) {
                handleDeleteRT(selectDomain, ingrInspectionPortOSId);
            }
            if (egrInspectionPortOSId != null) {
                handleDeleteRT(selectDomain, ingrInspectionPortOSId);
            }
        }
    }

    private void handleDeleteRT(Domain selectDomain, String ingrInspectionPortOSId) throws RestException {
        String filter = String.format("name like '%s'", ingrInspectionPortOSId);
        VPortsFetcher vportFet = selectDomain.getVPorts();
        List<VPort> vports  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(vports)){
            RedirectionTargetsFetcher rtFetch = vports.get(0).getRedirectionTargets();
            RedirectionTarget rt = rtFetch.getFirst();
            if (rt != null){
                rt.delete();
            }
        }
    }

    public void disableDefaultForwardingPolicy(PolicyGroup policyGroup, Domain selectDomain) throws RestException{

        String filter = String.format("description like '%s'", "default Policy");
        IngressAdvFwdTemplatesFetcher vportFet = selectDomain.getIngressAdvFwdTemplates();
        List<IngressAdvFwdTemplate> fwdPolicies  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);

        if (!CollectionUtils.isEmpty(fwdPolicies)){
            IngressAdvFwdTemplate fwdPolicy = fwdPolicies.get(0);
            fwdPolicy.setActive(false);
            fwdPolicy.save();
        }
    }

    private void beginPolicyChanges(Domain selectDomain) throws RestException {
        if (selectDomain != null) {
            Job fwdPolicyJob = new Job();
            fwdPolicyJob.setCommand(Job.Command.BEGIN_POLICY_CHANGES);
            selectDomain.createChild(fwdPolicyJob);
            int count = 0;
            while (!fwdPolicyJob.getStatus().equals(Status.SUCCESS)) {
                try {
                    Thread.sleep(2000);
                    count++;
                    if(count == 100) {
                        break;
                    }
                    fwdPolicyJob.fetch();
                    this.log.info(fwdPolicyJob);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void applyPolicyChanges(Domain selectDomain ) throws RestException{
        if (selectDomain != null){
            Job fwdPolicyJob = new Job();
            fwdPolicyJob.setCommand(Job.Command.APPLY_POLICY_CHANGES);
            selectDomain.createChild(fwdPolicyJob);
            int count = 0;
            while (!fwdPolicyJob.getStatus().equals(Status.SUCCESS)) {
                try {
                    Thread.sleep(2000);
                    count++;
                    if(count == 100) {
                        break;
                    }
                    fwdPolicyJob.fetch();
                    this.log.info(fwdPolicyJob);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.nuageRestApi.getVsdSession().reset();
    }
}
