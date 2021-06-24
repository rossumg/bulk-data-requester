package org.itech.fhircore.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.itech.fhircore.model.base.AuditableEntity;
import org.itech.fhircore.validation.annotation.ValidName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class CustomFhirResourceGroup extends AuditableEntity<Long> {

	// persistence
	@Column(unique = true)
	// validation
	@NotBlank
	@Size(min = 1, max = 255)
	@ValidName
	private String resourceGroupName;

	CustomFhirResourceGroup() {
	}

	public CustomFhirResourceGroup(String resourceGroupName) {
		this.resourceGroupName = resourceGroupName;
	}

}
