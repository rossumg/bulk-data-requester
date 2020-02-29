package org.itech.datarequester.bulk.web.api;

import java.util.List;

import org.hibernate.ObjectNotFoundException;
import org.itech.datarequester.bulk.service.DataRequestServerService;
import org.itech.datarequester.bulk.web.api.dto.CreateServerDTO;
import org.itech.fhircore.dao.ServerDAO;
import org.itech.fhircore.model.Server;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Server.SERVER_PATH)
public class ServerController {

	private ServerDAO serverRepository;
	private DataRequestServerService dataRequestServerService;

	public ServerController(ServerDAO serverRepository, DataRequestServerService dataRequestServerService) {
		this.serverRepository = serverRepository;
		this.dataRequestServerService = dataRequestServerService;
	}


	@GetMapping
	public ResponseEntity<List<Server>> getServers() {
		return new ResponseEntity<>(serverRepository.findAll(), HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<Server> createServer(@RequestBody CreateServerDTO dto) {
		Server newServer = dataRequestServerService.saveNewServerDefaultDataRequestTask(dto.getIdentifier(),
				dto.getServerAddress());
		return new ResponseEntity<>(newServer, HttpStatus.OK);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<Server> getServer(@PathVariable(value = "id") Long id) {
		Server server = serverRepository.findById(id)
				.orElseThrow(() -> new ObjectNotFoundException(id, Server.class.getName()));
		return new ResponseEntity<>(server, HttpStatus.OK);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<String> deleteServer(@PathVariable(value = "id") Long id) {
		serverRepository.deleteById(id);
		return new ResponseEntity<>("success", HttpStatus.OK);
	}

}
