package org.itech.datarequester.bulk.service.data.model.impl;

import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.data.model.DataRequestTaskService;
import org.itech.fhircore.service.impl.CrudServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DataRequestTaskServiceImpl extends CrudServiceImpl<DataRequestTask, Long>
		implements DataRequestTaskService {

	@SuppressWarnings("unused")
	private DataRequestTaskDAO dataRequestTaskRepository;

	public DataRequestTaskServiceImpl(DataRequestTaskDAO dataRequestTaskRepository) {
		super(dataRequestTaskRepository);
		this.dataRequestTaskRepository = dataRequestTaskRepository;
	}

}
