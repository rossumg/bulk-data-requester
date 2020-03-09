package org.itech.datarequester.bulk.service.data.model.impl;

import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.data.model.DataRequestTaskService;
import org.itech.fhircore.dao.ServerDAO;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.FhirResourceGroupService.FhirResourceCategories;
import org.itech.fhircore.service.impl.CrudServiceImpl;
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
