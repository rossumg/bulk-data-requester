package org.itech.datarequester.bulk.service.impl;

import java.time.Instant;
import java.util.List;

import org.itech.datarequester.bulk.dao.DataRequestAttemptDAO;
import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestCheckerService;
import org.itech.datarequester.bulk.service.DataRequestService;
import org.itech.datarequester.bulk.service.data.model.DataRequestTaskService;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.ServerService;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestCheckerServiceImpl implements DataRequestCheckerService {

	private ServerService serverService;
	private DataRequestTaskService serverDataRequestTaskService;
	private DataRequestAttemptDAO dataRequestAttemptRepository;
	private DataRequestService dataRequestService;

	public DataRequestCheckerServiceImpl(ServerService serverService,
			DataRequestTaskService serverDataRequestTaskService,
			DataRequestAttemptDAO dataRequestAttemptRepository,
			DataRequestService dataRequestService) {
		log.info(this.getClass().getName() + " has started");
		this.serverService = serverService;
		this.serverDataRequestTaskService = serverDataRequestTaskService;
		this.dataRequestAttemptRepository = dataRequestAttemptRepository;
		this.dataRequestService = dataRequestService;
	}

	@Override
	@Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
	@Transactional
	public void checkDataRequestNeedsRunning() {
		log.debug("checking if servers need data request to be made");

		Instant now = Instant.now();

		Iterable<Server> servers = serverService.getDAO().findAll();
		for (Server server : servers) {
			for (DataRequestTask dataRequestTask : serverDataRequestTaskService.getDAO()
					.findDataRequestTasksFromServer(server.getId())) {
				Instant nextDataRequestTime;
				List<DataRequestAttempt> latestDataRequestAttempts = dataRequestAttemptRepository
						.findLatestDataRequestAttemptsByDataRequestTask(PageRequest.of(0, 1), dataRequestTask.getId());
				if (!latestDataRequestAttempts.isEmpty()) {
					DataRequestAttempt lastDataRequestAttempt = latestDataRequestAttempts.get(0);
					nextDataRequestTime = lastDataRequestAttempt.getStartTime()
							.plus(dataRequestTask.getDataRequestInterval(), DataRequestTask.INTERVAL_UNITS);
				} else {
					nextDataRequestTime = now;
				}
				if (nextDataRequestTime.compareTo(now) <= 0) {
					log.debug("server found with dataRequest task needing to be run");
					dataRequestService.runDataRequestTask(dataRequestTask.getId());
				}

			}
		}
	}

}
