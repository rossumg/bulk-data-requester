package org.itech.fhirhose.datarequest.service.data.model;

import org.itech.fhirhose.common.service.CrudService;
import org.itech.fhirhose.datarequest.dao.DataRequestTaskDAO;
import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.itech.fhirhose.datarequest.model.Server;

public interface DataRequestTaskService extends CrudService<DataRequestTask, Long> {

	@Override
	DataRequestTaskDAO getDAO();

	DataRequestTask saveDefaultTaskToServer(Server server);

	DataRequestTask saveTaskToServer(Long id, String dataRequestType);

	DataRequestTask saveTaskToServer(Server server, String dataRequestType);

	DataRequestTask saveTaskToServer(Long id, String dataRequestType, Integer interval);

	DataRequestTask saveTaskToServer(Server server, String dataRequestType, Integer interval);
}
