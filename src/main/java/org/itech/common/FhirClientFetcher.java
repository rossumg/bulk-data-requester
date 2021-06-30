package org.itech.common;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public interface FhirClientFetcher {

	IGenericClient getFhirClient(String fhirStorePath);
}
