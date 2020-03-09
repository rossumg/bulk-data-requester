package org.itech.datarequester.bulk.service.job;

import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.itech.datarequester.bulk.service.DataRequestStatusService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataRequestTimeoutJob extends QuartzJobBean {

	// using @Autowired as constructor autowiring wont work as job is made by
	// Quartz, not Spring
	@Autowired
	private DataRequestStatusService dataRequestStatusService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobDataMap = context.getMergedJobDataMap();
		Long dataRequestAttemptId = jobDataMap.getLong("dataRequestAttemptId");
		log.debug("running timeout job " + dataRequestAttemptId);
		dataRequestStatusService.changeDataRequestAttemptStatus(dataRequestAttemptId, DataRequestStatus.TIMED_OUT);

	}
}
