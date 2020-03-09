package org.itech.datarequester.bulk.service;

public interface DataRequestService {

	void runDataRequestTasksForServer(Long serverId);

	void runDataRequestTask(Long dataRequestId);

}
