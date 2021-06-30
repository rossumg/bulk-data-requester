package org.itech.fhircore.web.api;

import javax.validation.Valid;

import org.hibernate.ObjectNotFoundException;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.ServerService;
import org.itech.fhircore.web.dto.CreateServerDTO;
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

	private ServerService serverService;

	public ServerController(ServerService serverService) {
		this.serverService = serverService;
	}


	@GetMapping
	public ResponseEntity<Iterable<Server>> getServers() {
		return ResponseEntity.ok(serverService.getDAO().findAll());
	}

	@PostMapping
	public ResponseEntity<Server> createServer(@RequestBody @Valid CreateServerDTO dto) {
		Server newServer = serverService.saveNewServer(dto.getName(),
				dto.getServerAddress());
		return ResponseEntity.ok(newServer);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<Server> getServer(@PathVariable(value = "id") Long id) {
		Server server = serverService.getDAO().findById(id)
				.orElseThrow(() -> new ObjectNotFoundException(id, Server.class.getName()));
		return ResponseEntity.ok(server);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<String> deleteServer(@PathVariable(value = "id") Long id) {
		serverService.getDAO().deleteById(id);
		return ResponseEntity.ok("deleted");
	}

}
