package org.itech.fhirhose.datarequest.service.data.model.impl;

import org.itech.fhirhose.common.service.impl.CrudServiceImpl;
import org.itech.fhirhose.datarequest.dao.DataRequestTaskDAO;
import org.itech.fhirhose.datarequest.dao.ServerDAO;
import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.itech.fhirhose.datarequest.model.Server;
import org.itech.fhirhose.datarequest.service.FhirResourceGroupService.FhirResourceCategories;
import org.itech.fhirhose.datarequest.service.data.model.DataRequestTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DataRequestTaskServiceImpl extends CrudServiceImpl<DataRequestTask, Long>
		implements DataRequestTaskService {

	private DataRequestTaskDAO dataRequestTaskDAO;
	private ServerDAO serverDAO;

	public DataRequestTaskServiceImpl(DataRequestTaskDAO dataRequestTaskDAO, ServerDAO serverDAO) {
		super(dataRequestTaskDAO);
		this.dataRequestTaskDAO = dataRequestTaskDAO;
		this.serverDAO = serverDAO;
	}

	@Override
	public DataRequestTaskDAO getDAO() {
		return dataRequestTaskDAO;
	}

	@Override
	public DataRequestTask saveDefaultTaskToServer(Server server) {
		log.debug("saving default dataRequest task to server");
		DataRequestTask dataRequestTask = new DataRequestTask(server, FhirResourceCategories.All.name());
		return dataRequestTaskDAO.save(dataRequestTask);
	}

	@Override
	public DataRequestTask saveTaskToServer(Long id, String dataRequestType) {
		return saveTaskToServer(serverDAO.findById(id).get(), dataRequestType);
	}

	@Override
	public DataRequestTask saveTaskToServer(Server server, String dataRequestType) {
		log.debug("saving dataRequest task of type " + dataRequestType.toString() + " to server");
		DataRequestTask dataRequestTask = new DataRequestTask(server, dataRequestType);
		return dataRequestTaskDAO.save(dataRequestTask);
	}

	@Override
	public DataRequestTask saveTaskToServer(Long id, String dataRequestType, Integer interval) {
		return saveTaskToServer(serverDAO.findById(id).get(), dataRequestType, interval);
	}

	@Override
	public DataRequestTask saveTaskToServer(Server server, String dataRequestType, Integer interval) {
		log.debug("saving dataRequest task of type " + dataRequestType + " to server with dataRequest interval "
				+ interval);
		DataRequestTask dataRequestTask = new DataRequestTask(server, dataRequestType);
		dataRequestTask.setDataRequestInterval(interval);
		return dataRequestTaskDAO.save(dataRequestTask);
	}

}
