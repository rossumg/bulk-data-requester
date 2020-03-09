package org.itech.datarequester.bulk.service.data.model;

import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.CrudService;

public interface DataRequestTaskService extends CrudService<DataRequestTask, Long> {

	@Override
	DataRequestTaskDAO getDAO();

	DataRequestTask saveDefaultTaskToServer(Server server);

	DataRequestTask saveTaskToServer(Long id, String dataRequestType);

	DataRequestTask saveTaskToServer(Server server, String dataRequestType);

	DataRequestTask saveTaskToServer(Long id, String dataRequestType, Integer interval);

	DataRequestTask saveTaskToServer(Server server, String dataRequestType, Integer interval);
}
