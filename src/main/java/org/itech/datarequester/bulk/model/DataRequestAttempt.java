package org.itech.datarequester.bulk.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.itech.datarequester.bulk.service.converter.DataRequestStatusConverter;
import org.itech.fhircore.model.base.AuditableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class DataRequestAttempt extends AuditableEntity<Long> {

	public enum DataRequestStatus {
		GENERATED('G'), REQUESTED('R'), ACCEPTED('A'), COMPLETE('C'), FAILED('F'), TIMED_OUT('T'), PARTIAL('P');

		private char code;

		private DataRequestStatus(char code) {
			this.code = code;
		}

		public char getCode() {
			return code;
		}
	}

	@Column(updatable = false)
	Instant startTime;

	@Column
	Instant endTime;

	@ManyToOne
	@JoinColumn(name = "dataRequest_task_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	DataRequestTask dataRequestTask;

	@Convert(converter = DataRequestStatusConverter.class)
	DataRequestStatus dataRequestStatus;

	DataRequestAttempt() {
	}

	public DataRequestAttempt(DataRequestTask dataRequestTask) {
		startTime = Instant.now();
		dataRequestStatus = DataRequestStatus.GENERATED;
		this.dataRequestTask = dataRequestTask;
	}

	public int getTimeout() {
		return dataRequestTask.getDataRequestAttemptTimeout();
	}

}
