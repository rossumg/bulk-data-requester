package org.itech.fhirhose.fhir;

import org.apache.commons.validator.GenericValidator;
import org.itech.fhirhose.config.FhirConfigProperties;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;

@Component
public class FhirUtil {

    private FhirContext fhirContext;
	private FhirConfigProperties fhirConfigProperties;

	public FhirUtil(FhirConfigProperties fhirConfigProperties, FhirContext fhirContext) {
		this.fhirConfigProperties = fhirConfigProperties;
		this.fhirContext = fhirContext;
	}

    public IGenericClient getFhirClient(String fhirStorePath) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirStorePath);
		if (!GenericValidator.isBlankOrNull(fhirConfigProperties.getUsername())
				&& !fhirConfigProperties.getFhirstoreUri().equals(fhirStorePath)) {
			IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfigProperties.getUsername(),
					fhirConfigProperties.getPassword());
			fhirClient.registerInterceptor(authInterceptor);
		}

        return fhirClient;
    }

    public IParser getFhirParser() {
        return fhirContext.newJsonParser();
    }

	public IGenericClient getLocalFhirClient() {
		return getFhirClient(fhirConfigProperties.getFhirstoreUri());
	}

}
