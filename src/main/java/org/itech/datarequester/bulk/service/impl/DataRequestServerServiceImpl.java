package org.itech.datarequester.bulk.service.impl;

import java.net.URI;
import java.util.List;

import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestServerService;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.FhirResourceGroupService.FhirResourceCategories;
import org.itech.fhircore.service.ServerService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestServerServiceImpl implements DataRequestServerService {

	private ServerService serverService;
	private DataRequestTaskDAO dataRequestTaskRepository;

	public DataRequestServerServiceImpl(ServerService serverService, DataRequestTaskDAO dataRequestTaskRepository) {
		this.serverService = serverService;
		this.dataRequestTaskRepository = dataRequestTaskRepository;
	}

	@Override
	public Server saveNewServerDefaultDataRequestTask(String identifier, String serverAddress) {
		log.debug("saving new server with default dataRequest task");
		Server addedServer = serverService.saveNewServer(identifier, serverAddress);
		saveDefaultTaskToServer(addedServer);
		return addedServer;
	}

	@Override
	public Server saveNewServerDefaultDataRequestTask(String identifier, URI dataRequestUrl) {
		log.debug("saving new server with default dataRequest task");
		Server addedServer = serverService.saveNewServer(identifier, dataRequestUrl);
		saveDefaultTaskToServer(addedServer);
		return addedServer;
	}

	@Override
	public void saveDefaultTaskToServer(Server server) {
		log.debug("saving default dataRequest task to server");
		DataRequestTask dataRequestTask = new DataRequestTask(server, FhirResourceCategories.All.name());
		dataRequestTaskRepository.save(dataRequestTask);
	}

	@Override
	public void saveTaskToServer(Long id, String dataRequestType) {
		saveTaskToServer(serverService.getDAO().findById(id).get(), dataRequestType);
	}

	@Override
	public void saveTaskToServer(Server server, String dataRequestType) {
		log.debug("saving dataRequest task of type " + dataRequestType.toString() + " to server");
		DataRequestTask dataRequestTask = new DataRequestTask(server, dataRequestType);
		dataRequestTaskRepository.save(dataRequestTask);
	}

	@Override
	public void saveTaskToServer(Long id, String dataRequestType, Integer interval) {
		saveTaskToServer(serverService.getDAO().findById(id).get(), dataRequestType, interval);
	}

	@Override
	public void saveTaskToServer(Server server, String dataRequestType, Integer interval) {
		log.debug("saving dataRequest task of type " + dataRequestType + " to server with dataRequest interval "
				+ interval);
		DataRequestTask dataRequestTask = new DataRequestTask(server, dataRequestType);
		dataRequestTask.setDataRequestInterval(interval);
		dataRequestTaskRepository.save(dataRequestTask);
	}

	@Override
	public List<DataRequestTask> getDataRequestTasksForServer(Long id) {
		return dataRequestTaskRepository.findDataRequestTasksFromServer(id);
	}

}
