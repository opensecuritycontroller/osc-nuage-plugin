package org.osc.controller.nuage.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.nuagenetworks.vspk.v4_0.fetchers.IngressAdvFwdTemplatesFetcher;
import net.nuagenetworks.vspk.v4_0.fetchers.PolicyGroupsFetcher;
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

    public NetworkElement createPolicyGroup(List<NetworkElement> elements, String domainId) throws RestException {
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

    private void addPolicyGroupPorts(Domain selectDomain, PolicyGroup pg, List<NetworkElement> elements) throws RestException {
        List<VPort> vports =new ArrayList<>();
        for (NetworkElement element: elements){
            List<VPort> temp =new ArrayList<>();
            //protected VM OS ID
            String filter = String.format("name like '%s'", element.getElementId());
            VPortsFetcher vportFet = selectDomain.getVPorts();
            temp  = vportFet.fetch(filter, null, null, null, null, null, Boolean.FALSE);
            if (!CollectionUtils.isEmpty(temp)) {
                vports.add(temp.get(0));
            }
        }

        pg.assign(vports);
    }

    public NetworkElement updatePolicyGroup(NetworkElement policyGroup, List<NetworkElement> protectedPorts,
            String domainId) throws RestException {

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
                addPolicyGroupPorts(selectDomain, polGrp, protectedPorts);
                return policyGroup;
            }
        }

        return null;
    }

    public void deletePolicyGroup(NetworkElement policyGroup) throws RestException {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        String selectDomainId = policyGroup.getParentId();
        Domain selectDomain = getDomain(session, selectDomainId);
        PolicyGroupsFetcher pgsFetcher = selectDomain.getPolicyGroups();
        List<PolicyGroup> pgs = pgsFetcher.get();
        for (PolicyGroup pg : pgs) {
            if (pg.getId().equals(policyGroup.getElementId())) {
                pg.assign(new ArrayList<VPort>(), VPort.class);
                pg.delete();
            }
        }
    }

    private Domain getDomain(OSCVSDSession session, String domainId) throws RestException {
        Me me = session.getMe();
        Enterprise enterprise = me.getEnterprises().getFirst();

        Domain selectDomain = null;
        DomainsFetcher fetcher = new DomainsFetcher(enterprise);
        String filter = String.format("name like '%s'", domainId);
        List<Domain> dms = fetcher.fetch(filter, null, null, null, null, null, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(dms)) {
            selectDomain = dms.get(0);
        }
        return selectDomain;
    }

    private List<IngressAdvFwdTemplate> getForwardingPolicies(Domain selectDomain) throws RestException {
        String filter = String.format("active like '%s'", true);
        IngressAdvFwdTemplatesFetcher advFwdFetcher = selectDomain.getIngressAdvFwdTemplates();
        List<IngressAdvFwdTemplate> advFwdPolicies = advFwdFetcher.fetch(filter, null, null, null, null, null, false);
        return advFwdPolicies;
    }

    private IngressAdvFwdTemplate createNewForwardingPolicy(Domain selectDomain) throws RestException {
        if (selectDomain != null){
            long priority = getNextFwdPolicyPriority(getForwardingPolicies(selectDomain));
            String externalId = UUID.randomUUID().toString();
            //create forwarding policy
            IngressAdvFwdTemplate fwdPolicy = new IngressAdvFwdTemplate();
            fwdPolicy.setActive(true);
            fwdPolicy.setPriority(Long.valueOf(priority));

            fwdPolicy.setName("FwdPolicy-" + externalId);
            fwdPolicy.setExternalID(externalId);
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

    public IngressAdvFwdTemplate getFwdPolicy(String inspecHookId) throws Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();
        Me me = session.getMe();

        Enterprise enterprise = me.getEnterprises().getFirst();

        DomainsFetcher domainsFetcher = enterprise.getDomains();
        List<Domain> domains = domainsFetcher.get();

        for(Domain domain: domains) {
            IngressAdvFwdTemplatesFetcher fwdPoliciesFetcher = domain.getIngressAdvFwdTemplates();
            List<IngressAdvFwdTemplate> fwdPolicies = fwdPoliciesFetcher.get();

            for (IngressAdvFwdTemplate fwdPolicy : fwdPolicies) {
                if (fwdPolicy.getExternalID() != null && fwdPolicy.getExternalID().equals(inspecHookId)) {
                    return fwdPolicy;
                }
            }
        }

        return null;
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

    public String installInspectionHook(NetworkElement policyGroup, InspectionPortElement inspectionPort)
            throws IOException, Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        PolicyGroup fetchPG = new PolicyGroup();
        fetchPG.setId(policyGroup.getElementId());
        fetchPG.fetch();
        IngressAdvFwdTemplate fwdPolicy = null;
        if (fetchPG != null){
            String selectDomainId = fetchPG.getParentId();
            Domain selectDomain = new Domain();
            selectDomain.setId(selectDomainId);
            selectDomain.fetch();
            beginPolicyChanges(selectDomain);
            fwdPolicy = createNewForwardingPolicy(selectDomain);
            createFwdPolicyIngressEgressEntries(fetchPG, fwdPolicy, inspectionPort, selectDomain);
            applyPolicyChanges(selectDomain);
        }

        return fwdPolicy == null ? null : fwdPolicy.getExternalID();
    }

    public void deleteInspectionHook(String inspectionHookId)
            throws RestException, IOException, Exception {
        IngressAdvFwdTemplate fwrdPolicy = getFwdPolicy(inspectionHookId);
        if (fwrdPolicy != null) {
            fwrdPolicy.fetch();
            fwrdPolicy.delete();
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

    private int getNextFwdPolicyPriority(List<IngressAdvFwdTemplate> fwdPolicies) {
        if (CollectionUtils.isEmpty(fwdPolicies)) {
            return 0;
        }

        fwdPolicies.sort(new FwrdPolicyPriorityComparator());
        long currentPriority = 0;
        for (IngressAdvFwdTemplate fwdPolicy : fwdPolicies) {
            if (fwdPolicy.getPriority() - currentPriority > 1) {
                break;
            }

            currentPriority = fwdPolicy.getPriority();
        }

        return (int) (currentPriority + 1);
    }

    public void deleteInspectionPort( String selectDomainId, InspectionPortElement inspPort) throws Exception {
        OSCVSDSession session = this.nuageRestApi.getVsdSession();
        session.start();

        Domain selectDomain = getDomain(session, selectDomainId);

        String ingrInspectionPortOSId = inspPort.getIngressPort().getElementId(),
                egrInspectionPortOSId = inspPort.getEgressPort().getElementId();
        if (ingrInspectionPortOSId != null) {
            handleDeleteRT(selectDomain, ingrInspectionPortOSId);
        }
        if (egrInspectionPortOSId != null) {
            handleDeleteRT(selectDomain, egrInspectionPortOSId);
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
                rt.assign(new ArrayList<VPort>(), VPort.class);
                rt.delete();
            }
        }
    }

    public void disableDefaultForwardingPolicy(PolicyGroup policyGroup, Domain selectDomain) throws RestException {
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

    private class FwrdPolicyPriorityComparator implements Comparator<IngressAdvFwdTemplate> {
        @Override
        public int compare(IngressAdvFwdTemplate fwrdPol1, IngressAdvFwdTemplate fwrdPol2) {
            return (int) (fwrdPol1.getPriority() - fwrdPol2.getPriority());
        }
    }
}
