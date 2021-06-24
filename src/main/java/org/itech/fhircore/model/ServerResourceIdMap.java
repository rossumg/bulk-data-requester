package org.itech.fhircore.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhircore.model.base.PersistenceEntity;
import org.itech.fhircore.validation.annotation.ValidIdMap;
import org.itech.fhircore.validation.constraint.IdMapValidator.IdType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "remote_server_id", "resource_type" }) })
public class ServerResourceIdMap extends PersistenceEntity<Long> {

	// persistence
	@Column(name = "resource_type", nullable = false, updatable = false)
	// validation
	@NotNull
	private ResourceType resourceType;

	// persistence
	@ManyToOne
	@JoinColumn(name = "remote_server_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	// validation
	@NotNull
	private Server remoteServer;

	// persistence
	@ElementCollection
	// validation
	@ValidIdMap(keyIdType = IdType.AlphaNum, valueIdType = IdType.AlphaNum)
	private Map<String, String> remoteIdToLocalIdMap;

	ServerResourceIdMap() {

	}

	public ServerResourceIdMap(Server server, ResourceType resourceType) {
		remoteIdToLocalIdMap = new HashMap<>();
		remoteServer = server;
		this.resourceType = resourceType;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof ServerResourceIdMap)) {
			return false;
		}
		ServerResourceIdMap serverResourceIdMap = (ServerResourceIdMap) object;

		return Objects.equals(serverResourceIdMap.resourceType, this.resourceType)
				&& Objects.equals(serverResourceIdMap.remoteServer, this.remoteServer);
	}

	@Override
	public int hashCode() {
		return Objects.hash(resourceType, remoteServer);
	}

}
