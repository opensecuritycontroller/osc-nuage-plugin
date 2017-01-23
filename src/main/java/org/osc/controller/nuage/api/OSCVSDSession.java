package org.osc.controller.nuage.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import net.nuagenetworks.bambou.RestSession;
import net.nuagenetworks.bambou.service.RestClientService;
import net.nuagenetworks.bambou.service.RestClientTemplate;
import net.nuagenetworks.bambou.spring.SpringConfig;
import net.nuagenetworks.vspk.v4_0.Me;

public class OSCVSDSession extends RestSession<Me> {

    public final static double VERSION = 4.0;

    @Autowired
    private RestClientTemplate restClientTemplate;

    public OSCVSDSession(){
        super(Me.class);

        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                     SpringConfig.class)) {
               applicationContext.register(RestClientService.class);
               applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
        }

    }

    public OSCVSDSession(String username, String password, String enterprise, String apiUrl) {
       this(username, password, enterprise, apiUrl, null);
    }

    public OSCVSDSession(String username, String password, String enterprise, String apiUrl, String certificate) {
       this();

       setUsername(username);
       setPassword(password);
       setEnterprise(enterprise);
       setApiUrl(apiUrl);
       setApiPrefix("nuage/api");
       setVersion(VERSION);
       setCertificate(certificate);
    }

    @Override
    public double getVersion() {
       return VERSION;
    }

    public RestClientTemplate getClientTemplate() {
       return this.restClientTemplate;
    }

    public Me getMe() {
       return super.getRootObject();
    }




}
