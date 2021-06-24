package org.itech.fhircore.model.base;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper = true)
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity<I> extends PersistenceEntity<I> {

	@Column(name = "created")
	@CreatedDate
	private Instant created;

	@Column(name = "created_by")
	@CreatedBy
	protected String createdBy;

	@Column(name = "last_modified")
	@LastModifiedDate
	private Instant lastModified;

	@Column(name = "last_modified_by")
	@LastModifiedBy
	protected String lastModifiedBy;

}
