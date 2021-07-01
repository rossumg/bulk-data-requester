package org.itech.fhirhose.web.api;

import java.util.List;

import javax.validation.Valid;

import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.itech.fhirhose.datarequest.service.data.model.DataRequestTaskService;
import org.itech.fhirhose.web.api.dto.CreateDataRequestTaskDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "servers/{serverId}/dataRequestTasks")
public class DataRequestTaskController {

	private DataRequestTaskService serverDataRequestTaskService;

	public DataRequestTaskController(DataRequestTaskService serverDataRequestTaskService) {
		this.serverDataRequestTaskService = serverDataRequestTaskService;
	}

	@GetMapping
	public ResponseEntity<List<DataRequestTask>> getDataRequestTasksForServer(
			@PathVariable(value = "serverId") Long serverId) {
		return ResponseEntity.ok(serverDataRequestTaskService.getDAO().findDataRequestTasksFromServer(serverId));
	}

	@PostMapping
	public ResponseEntity<DataRequestTask> addDataRequestTaskForServer(@PathVariable(value = "serverId") Long serverId,
			@RequestBody @Valid CreateDataRequestTaskDTO dto) {
		DataRequestTask newDataRequestTask;
		if (dto.getInterval() == null) {
			newDataRequestTask = serverDataRequestTaskService.saveTaskToServer(serverId, dto.getDataRequestType());
		} else {
			newDataRequestTask = serverDataRequestTaskService.saveTaskToServer(serverId, dto.getDataRequestType(),
					dto.getInterval());
		}
		return ResponseEntity.ok(newDataRequestTask);
	}

	@PutMapping
	public ResponseEntity<DataRequestTask> updateDataRequestTaskForServer(
			@PathVariable(value = "serverId") Long serverId, @RequestBody @Valid CreateDataRequestTaskDTO dto) {
		DataRequestTask newDataRequestTask;
		if (dto.getInterval() == null) {
			newDataRequestTask = serverDataRequestTaskService.saveTaskToServer(serverId, dto.getDataRequestType());
		} else {
			newDataRequestTask = serverDataRequestTaskService.saveTaskToServer(serverId, dto.getDataRequestType(),
					dto.getInterval());
		}
		return ResponseEntity.ok(newDataRequestTask);
	}
}
