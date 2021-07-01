package org.itech.fhirhose.datarequest.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.itech.fhirhose.common.model.AuditableEntity;
import org.itech.fhirhose.datarequest.service.converter.DataRequestStatusConverter;

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

	// persistence
	@Column(updatable = false)
	// validation
	@NotNull
	private Instant startTime;

	@Column
	private Instant endTime;

	// persistence
	@ManyToOne
	@JoinColumn(name = "dataRequest_task_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	// validation
	@NotNull
	private DataRequestTask dataRequestTask;

	// persistence
	@Convert(converter = DataRequestStatusConverter.class)
	// validation
	@NotNull
	private DataRequestStatus dataRequestStatus;

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
