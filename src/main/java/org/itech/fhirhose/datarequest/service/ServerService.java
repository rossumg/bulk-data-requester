package org.itech.fhirhose.datarequest.service;

import java.net.URI;

import org.itech.fhirhose.common.service.CrudService;
import org.itech.fhirhose.datarequest.model.Server;

public interface ServerService extends CrudService<Server, Long> {

	Server saveNewServer(String name, String serverAddress);

	Server saveNewServer(String name, URI dataRequestUrl);

	void updateServerIdentifier(String oldIdentifier, String newIdentifier);

}
