package org.itech.fhirhose.datarequest.service.queue;

import java.util.HashMap;
import java.util.Map;

import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.springframework.stereotype.Component;

@Component
public class ActiveDataRequestTaskHolder {

	private final Map<Long, DataRequestTask> dataRequestTasks = new HashMap<>();

	public void addDataRequestTask(DataRequestTask dataRequestTask) {
		dataRequestTasks.put(dataRequestTask.getId(), dataRequestTask);
	}

	public void removeDataRequestTask(DataRequestTask dataRequestTask) {
		dataRequestTasks.remove(dataRequestTask.getId());
	}

	public DataRequestTask removeDataRequestTask(Long dataRequestTaskId) {
		return dataRequestTasks.remove(dataRequestTaskId);
	}

	public boolean contains(Long dataRequestTaskId) {
		return dataRequestTasks.containsKey(dataRequestTaskId);
	}

	public DataRequestTask getDataRequestTask(Long dataRequestTaskId) {
		return dataRequestTasks.get(dataRequestTaskId);
	}

}
