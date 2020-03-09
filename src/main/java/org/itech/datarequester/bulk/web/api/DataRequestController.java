package org.itech.datarequester.bulk.web.api;

import org.itech.datarequester.bulk.service.DataRequestService;
import org.itech.fhircore.model.Server;
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
