package org.itech.datarequester.bulk.service.impl;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import org.hibernate.ObjectNotFoundException;
import org.itech.datarequester.bulk.dao.DataRequestAttemptDAO;
import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestStatusService;
import org.itech.datarequester.bulk.service.event.DataRequestStatusEvent;
import org.itech.datarequester.bulk.service.job.DataRequestTimeoutJob;
import org.itech.datarequester.bulk.service.queue.DataRequestAttemptWaitQueue;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestStatusServiceImpl implements DataRequestStatusService {

	private static String DATA_REQUEST_ATTEMPT_TIMEOUT_JOB_GROUP = "DataRequestAttemptTimeout";

	private DataRequestAttemptDAO dataRequestAttemptRepository;
	private DataRequestAttemptWaitQueue dataRequestAttemptWaitQueue;
	private ApplicationEventPublisher applicationEventPublisher;
	private Scheduler scheduler;

	public DataRequestStatusServiceImpl(DataRequestAttemptDAO dataRequestAttemptRepository,
			DataRequestAttemptWaitQueue dataRequestAttemptWaitQueue,
			ApplicationEventPublisher applicationEventPublisher, Scheduler scheduler) {
		this.dataRequestAttemptRepository = dataRequestAttemptRepository;
		this.dataRequestAttemptWaitQueue = dataRequestAttemptWaitQueue;
		this.applicationEventPublisher = applicationEventPublisher;
		this.scheduler = scheduler;
	}

	@Transactional
	@EventListener(DataRequestStatusEvent.class)
	public void onApplicationEvent(DataRequestStatusEvent event) {
		log.debug("DataRequestEvent  " + event.getDataRequestStatus() + " for  " + event.getDataRequestAttemptId()
				+ " detected");
		DataRequestAttempt dataRequestAttempt;
		log.debug(">>>DataRequestEvent 0 ");
		if (dataRequestAttemptWaitQueue.contains(event.getDataRequestAttemptId())) {
		    log.debug(">>>DataRequestEvent 1 ");
			dataRequestAttempt = dataRequestAttemptWaitQueue.getDataRequestAttempt(event.getDataRequestAttemptId());
			log.debug(">>>DataRequestEvent 2 ");
		} else {
		    log.debug(">>>DataRequestEvent 3 ");
			dataRequestAttempt = dataRequestAttemptRepository.findById(event.getDataRequestAttemptId())
					.orElseThrow(() -> new ObjectNotFoundException(event.getDataRequestAttemptId(),
							DataRequestAttempt.class.getName()));
			log.debug(">>>DataRequestEvent 4 ");
		}
		synchronized (dataRequestAttempt) {
			switch (event.getDataRequestStatus()) {
			case GENERATED:
				// never published
				break;
			case REQUESTED:
				dataRequestAttemptWaitQueue.addDataRequestAttempt(dataRequestAttempt);
				break;
			case ACCEPTED:
				this.createTimeoutJob(dataRequestAttempt);
				break;
			case COMPLETE:
				deleteTimeoutJob(dataRequestAttempt.getId());
			case FAILED:
			case TIMED_OUT:
			case PARTIAL:
				dataRequestAttemptWaitQueue.removeDataRequestAttempt(event.getDataRequestAttemptId());
				dataRequestAttempt.setEndTime(Instant.now());
			}
			dataRequestAttempt.setDataRequestStatus(event.getDataRequestStatus());
			dataRequestAttemptRepository.save(dataRequestAttempt);
		}
	}

	private void deleteTimeoutJob(Long dataRequestAttemptId) {
		try {
			scheduler.deleteJob(findTimeoutJobKey(dataRequestAttemptId.toString()));
			log.debug("deleted " + DATA_REQUEST_ATTEMPT_TIMEOUT_JOB_GROUP + " for data request attempt "
					+ dataRequestAttemptId);
		} catch (SchedulerException e) {
			log.warn("could not find job key of group " + DATA_REQUEST_ATTEMPT_TIMEOUT_JOB_GROUP + "with id"
					+ dataRequestAttemptId);
		}
	}

	private void createTimeoutJob(DataRequestAttempt dataRequestAttempt) {
		JobDetail jobDetail = createJobDetail(dataRequestAttempt);
		Trigger trigger = buildOneTimeJobTrigger(jobDetail, dataRequestAttempt);
		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			log.error("couldn't schedule dataRequestAttempt timeout", e);
		}
		log.debug("created job for checking timeout of data request attempt " + dataRequestAttempt.getId());
	}

	@Override
	public void changeDataRequestAttemptStatus(Long dataRequestAttemptId, DataRequestStatus requestStatus) {
		publishDataRequestEvent(dataRequestAttemptId, requestStatus);
	}

	private void publishDataRequestEvent(Long dataRequestAttemptId, DataRequestStatus dataRequestStatus) {
		log.debug("publishing dataRequest event for dataRequestAttempt " + dataRequestAttemptId + " with status "
				+ dataRequestStatus);
		applicationEventPublisher.publishEvent(new DataRequestStatusEvent(this, dataRequestAttemptId, dataRequestStatus));
	}

	private JobDetail createJobDetail(DataRequestAttempt dataRequestAttempt) {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("dataRequestAttemptId", dataRequestAttempt.getId());

		return JobBuilder.newJob(DataRequestTimeoutJob.class)
				.withIdentity(dataRequestAttempt.getId().toString(), DATA_REQUEST_ATTEMPT_TIMEOUT_JOB_GROUP)
				.usingJobData(jobDataMap).build();
	}

	private Trigger buildOneTimeJobTrigger(JobDetail jobDetail, DataRequestAttempt dataRequestAttempt) {
		log.debug("creating one time trigger with timeout " + dataRequestAttempt.getTimeout() + " "
				+ DataRequestTask.TIMEOUT_UNITS);
		return TriggerBuilder.newTrigger().forJob(jobDetail)
				.withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
				.startAt(Date.from(Instant.now().plus(dataRequestAttempt.getTimeout(), DataRequestTask.TIMEOUT_UNITS)))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()).build();
	}

	private JobKey findTimeoutJobKey(String jobName) throws SchedulerException {
		// Check all jobs if not found
		for (String groupName : scheduler.getJobGroupNames()) {
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
				if (Objects.equals(jobName, jobKey.getName())) {
					return jobKey;
				}
			}
		}
		return null;
	}

}
