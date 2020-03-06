package org.itech.datarequester.bulk.service;

import java.net.URI;
import java.util.List;

import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.fhircore.model.Server;

public interface DataRequestServerService {

	Server saveNewServerDefaultDataRequestTask(String name, String serverAddress);

	Server saveNewServerDefaultDataRequestTask(String name, URI dataRequestUrl);

	void saveDefaultTaskToServer(Server server);

	void saveTaskToServer(Long id, String dataRequestType);

	void saveTaskToServer(Server server, String dataRequestType);

	void saveTaskToServer(Long id, String dataRequestType, Integer interval);

	void saveTaskToServer(Server server, String dataRequestType, Integer interval);


	List<DataRequestTask> getDataRequestTasksForServer(Long id);

}
