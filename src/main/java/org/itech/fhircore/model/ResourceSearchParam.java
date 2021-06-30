package org.itech.fhircore.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhircore.model.base.PersistenceEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = { "resource_type", "param_name", "custom_fhir_resource_group_id" }) })
public class ResourceSearchParam extends PersistenceEntity<Long> {

	@Column(name = "resource_type")
	private ResourceType resourceType;

	@Column(name = "param_name")
	private String paramName;

	@ElementCollection
	private List<String> paramValues;

	@ManyToOne
	@JoinColumn(name = "custom_fhir_resource_group_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private CustomFhirResourceGroup customFhirResourceGroup;

	ResourceSearchParam() {

	}

	public ResourceSearchParam(ResourceType resourceType) {
		this.resourceType = resourceType;
		paramValues = new ArrayList<>();
	}

	public ResourceSearchParam(CustomFhirResourceGroup customFhirResourceGroup, ResourceType resourceType) {
		this.customFhirResourceGroup = customFhirResourceGroup;
		this.resourceType = resourceType;
		paramValues = new ArrayList<>();
	}


	public ResourceSearchParam(CustomFhirResourceGroup customFhirResourceGroup, ResourceType resourceType,
			String paramName, List<String> paramValues) {
		this.customFhirResourceGroup = customFhirResourceGroup;
		this.resourceType = resourceType;
		this.paramName = paramName;
		this.paramValues = paramValues;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof ResourceSearchParam)) {
			return false;
		}
		ResourceSearchParam resourceSearchParam = (ResourceSearchParam) object;


		return Objects.equals(resourceSearchParam.resourceType, this.resourceType)
				&& Objects.equals(resourceSearchParam.paramName, this.paramName) //
				&& Objects.equals(resourceSearchParam.customFhirResourceGroup, this.customFhirResourceGroup);
	}

	@Override
	public int hashCode() {
		return Objects.hash(resourceType, paramName, customFhirResourceGroup);
	}

}
