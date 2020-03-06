package org.itech.datarequester.bulk.web.api;

import java.util.List;

import javax.validation.Valid;

import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestServerService;
import org.itech.datarequester.bulk.web.api.dto.AddDataRequestTaskDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "servers/{serverId}/dataRequestTasks")
public class ServerDataRequestTaskController {

	private DataRequestServerService dataRequestServerService;

	public ServerDataRequestTaskController(DataRequestServerService dataRequestServerService) {
		this.dataRequestServerService = dataRequestServerService;
	}

	@GetMapping
	public ResponseEntity<List<DataRequestTask>> getDataRequestTasksForServer(
			@PathVariable(value = "serverId") Long serverId) {
		return ResponseEntity.ok(dataRequestServerService.getDataRequestTasksForServer(serverId));
	}

	@PostMapping
	public ResponseEntity<String> addDataRequestTaskForServer(@PathVariable(value = "serverId") Long serverId,
			@RequestBody @Valid AddDataRequestTaskDTO dto) {
		dataRequestServerService.saveTaskToServer(serverId, dto.getDataRequestType(), dto.getInterval());
		return ResponseEntity.ok("success");
	}
}
