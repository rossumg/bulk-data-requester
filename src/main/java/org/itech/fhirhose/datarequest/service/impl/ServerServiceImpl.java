package org.itech.fhirhose.datarequest.service.impl;

import java.net.URI;

import org.hibernate.ObjectNotFoundException;
import org.itech.fhirhose.common.service.impl.CrudServiceImpl;
import org.itech.fhirhose.datarequest.dao.ServerDAO;
import org.itech.fhirhose.datarequest.model.Server;
import org.itech.fhirhose.datarequest.service.ServerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ServerServiceImpl extends CrudServiceImpl<Server, Long> implements ServerService {

	private ServerDAO serverRepository;

	public ServerServiceImpl(ServerDAO serverRepository) {
		super(serverRepository);
		log.info(this.getClass().getName() + " has started");
		this.serverRepository = serverRepository;
	}

	@Override
	public Server saveNewServer(String name, String serverAddress) {
		log.debug("saving new server");
		Server server = new Server(name, serverAddress);
		Server addedServer = serverRepository.save(server);
		return addedServer;
	}

	@Override
	public Server saveNewServer(String name, URI dataRequestUrl) {
		log.debug("saving new server");
		Server server = new Server(name, dataRequestUrl);
		Server addedServer = serverRepository.save(server);
		return addedServer;
	}

	@Override
	public void updateServerIdentifier(String oldName, String newName) {
		log.debug("updating server identifier");
		Server server = serverRepository.findByName(oldName)
				.orElseThrow(() -> new ObjectNotFoundException(oldName, Server.class.getName()));
		server.setName(newName);
	}

}
