package org.itech.datarequester.bulk.model;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.model.base.AuditableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class DataRequestTask extends AuditableEntity<Long> {

	public static final TemporalUnit INTERVAL_UNITS = ChronoUnit.MINUTES;
	public static final TemporalUnit TIMEOUT_UNITS = ChronoUnit.SECONDS;

	private static final Integer DEFAULT_INTERVAL = 60 * 24 * 7;
	private static final Byte DEFAULT_RETRY_ATTEMPTS = 3;
	private static final Integer DEFAULT_TIMEOUT = 60 * 2;

	private String dataRequestType;

	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "remote_server_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Server remoteServer;

	private Byte dataRequestAttemptRetries = DEFAULT_RETRY_ATTEMPTS;

	private Integer dataRequestAttemptTimeout = DEFAULT_TIMEOUT;

	private Integer dataRequestInterval = DEFAULT_INTERVAL;

	DataRequestTask() {
	}

	public DataRequestTask(Server server, String dataRequestType) {
		this.dataRequestType = dataRequestType;
		this.remoteServer = server;
	}

}
