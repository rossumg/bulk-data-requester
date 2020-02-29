package org.itech.datarequester.bulk.service.data.model.impl;

import java.time.Instant;
import java.util.List;

import org.itech.datarequester.bulk.dao.DataRequestAttemptDAO;
import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.itech.datarequester.bulk.service.data.model.DataRequestAttemptService;
import org.itech.fhircore.service.impl.CrudServiceImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DataRequestAttemptServiceImpl extends CrudServiceImpl<DataRequestAttempt, Long>
		implements DataRequestAttemptService {

	private DataRequestAttemptDAO dataRequestAttemptRepository;

	public DataRequestAttemptServiceImpl(DataRequestAttemptDAO dataRequestAttemptRepository) {
		super(dataRequestAttemptRepository);
		this.dataRequestAttemptRepository = dataRequestAttemptRepository;
	}

	@Override
	public Instant getLatestSuccessDate(DataRequestAttempt dataRequestAttempt) {
		Instant lastSuccess = Instant.EPOCH;

		List<DataRequestAttempt> lastRequestAttempts = dataRequestAttemptRepository
				.findLatestDataRequestAttemptsByDataRequestTaskAndStatus(PageRequest.of(0, 1),
						dataRequestAttempt.getDataRequestTask().getId(), DataRequestStatus.COMPLETE);
		if (lastRequestAttempts.size() == 1) {
			DataRequestAttempt latestAttempt = lastRequestAttempts.get(0);
			lastSuccess = latestAttempt.getStartTime();
		}
		log.debug("last success was at: " + lastSuccess);
		return lastSuccess;
	}

}
