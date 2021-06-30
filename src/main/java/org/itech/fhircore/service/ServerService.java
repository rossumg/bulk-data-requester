package org.itech.fhircore.service;

import java.net.URI;

import org.itech.fhircore.model.Server;

public interface ServerService extends CrudService<Server, Long> {

	Server saveNewServer(String name, String serverAddress);

	Server saveNewServer(String name, URI dataRequestUrl);

	void updateServerIdentifier(String oldIdentifier, String newIdentifier);

}
