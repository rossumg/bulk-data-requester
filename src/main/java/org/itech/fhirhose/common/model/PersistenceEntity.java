package org.itech.fhirhose.common.model;

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
