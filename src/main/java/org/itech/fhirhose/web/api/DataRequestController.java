package org.itech.fhirhose.web.api;

import org.itech.fhirhose.datarequest.model.Server;
import org.itech.fhirhose.datarequest.service.DataRequestService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Server.SERVER_PATH + "/{serverId}")
public class DataRequestController {

	private DataRequestService dataRequestService;

	public DataRequestController(DataRequestService dataRequestService) {
		this.dataRequestService = dataRequestService;
	}

	@PostMapping(value = "/dataRequestAttempt")
	public void runManualDataRequestsForServer(@PathVariable Long serverId) {
		dataRequestService.runDataRequestTasksForServer(serverId);
	}

	@PostMapping(value = "/dataRequestTask/{dataRequestTaskId}/dataRequestAttempt")
	public void runManualDataRequest(@PathVariable Long serverId, @PathVariable Long dataRequestTaskId) {
		dataRequestService.runDataRequestTask(dataRequestTaskId);
	}

}
