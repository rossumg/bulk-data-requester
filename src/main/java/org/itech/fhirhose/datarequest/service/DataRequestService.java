package org.itech.fhirhose.datarequest.service;

public interface DataRequestService {

	void runDataRequestTasksForServer(Long serverId);

	void runDataRequestTask(Long dataRequestId);
	
}
