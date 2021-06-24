package org.itech.datarequester.bulk.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;

@Configuration
@PropertySource("file:/run/secrets/common.properties")
public class FhirConfig {

//    @Value("${org.openelisglobal.oe.fhir.system:http://openelis-global.org}")
//    private String oeFhirSystem;
//    @Value("${org.openelisglobal.fhirstore.uri}")
//    private String localFhirStorePath;
//    @Value("${org.openelisglobal.remote.source.uri}")
//    private String[] remoteStorePaths;
//    @Value("${org.openelisglobal.fhirstore.username:}")
//    private String username;
//    @Value("${org.openelisglobal.fhirstore.password:}")
//    private String password;

    @Autowired
    CloseableHttpClient httpClient;

//    public String getLocalFhirStorePath() {
//        return localFhirStorePath;
//    }

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.R4);
        configureFhirHttpClient(fhirContext);
        return fhirContext;
    }

    public void configureFhirHttpClient(FhirContext fhirContext) {
        IRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(fhirContext);

        clientFactory.setHttpClient(httpClient);
        fhirContext.setRestfulClientFactory(clientFactory);

    }

//    public String getOeFhirSystem() {
//        return oeFhirSystem;
//    }
//
//    public String getUsername() {
//        return username;
//    }
}
