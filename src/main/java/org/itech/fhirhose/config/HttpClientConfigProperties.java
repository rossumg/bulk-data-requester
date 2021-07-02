package org.itech.fhirhose.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "server.ssl")
@Data
public class HttpClientConfigProperties {

    private Resource trustStore;
    private String trustStorePassword;
    private Resource keyStore;
    private String keyStorePassword;
    private String keyPassword;

}
