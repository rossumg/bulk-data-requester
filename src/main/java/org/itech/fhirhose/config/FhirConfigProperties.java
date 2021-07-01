package org.itech.fhirhose.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "org.itech.fhirhose.fhir")
@Data
public class FhirConfigProperties {

	private String fhirstoreUri;
	private String username = "";
	private String password = "";

}
