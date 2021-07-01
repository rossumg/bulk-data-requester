package org.itech.fhirhose.etl.service.impl;

import java.util.List;

import org.itech.fhirhose.common.service.impl.CrudServiceImpl;
import org.itech.fhirhose.etl.dao.ETLRecordDAO;
import org.itech.fhirhose.etl.model.ETLRecord;
import org.itech.fhirhose.etl.service.ETLRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ETLRecordServiceImpl extends CrudServiceImpl<ETLRecord, Long> implements ETLRecordService {

	private ETLRecordDAO etlRecordRepository;

	public ETLRecordServiceImpl(ETLRecordDAO etlRecordRepository) {
		super(etlRecordRepository);
		this.etlRecordRepository = etlRecordRepository;
	}

	@Override
	public boolean saveAll(List<ETLRecord> etlRecords) {
		log.debug("saveAll: " + etlRecords.size());
		List<ETLRecord> savedRecords = etlRecordRepository.saveAll(etlRecords);
		log.debug("savedRecords: " + savedRecords.size());
		if (etlRecords.size() == savedRecords.size()) {
			return true;
		} else {
			return false;
		}
	}
}
