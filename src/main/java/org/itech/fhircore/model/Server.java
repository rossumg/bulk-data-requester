package org.itech.fhircore.model;

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.itech.fhircore.model.base.AuditableEntity;
import org.itech.fhircore.util.URIUtil;
import org.itech.fhircore.validation.annotation.ValidName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Server extends AuditableEntity<Long> {

	public static final String SERVER_PATH = "/servers";

	private URI serverUrl;

	// persistence
	@Column(name = "name", unique = true, nullable = false, length = 255)
	// validation
	@NotBlank
	@Size(max = 255)
	@ValidName
	private String name;

	Server() {
	}

	public Server(String name, String serverAddress) {
		this.name = name;
		this.serverUrl = URIUtil.createHttpUrlFromString(serverAddress);
	}

	public Server(String name, URI serverUrl) {
		this.name = name;
		this.serverUrl = serverUrl;
	}

}
