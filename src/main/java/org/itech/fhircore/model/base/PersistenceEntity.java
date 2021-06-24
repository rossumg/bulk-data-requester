package org.itech.fhircore.model.base;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;

@MappedSuperclass
@Data
public abstract class PersistenceEntity<I> {

	@Id
	@GeneratedValue
	I id;

}
