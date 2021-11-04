package org.itech.fhirhose.etl.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.itech.fhirhose.common.model.AuditableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class ETLRecord extends AuditableEntity<Long> {

    private String externalId;
    private String patientId;
    private String statusId;
    private Timestamp orderTimestamp;
	@Column(length = 65535)
    private String data;
    private String labno;
    private String identifier;
    private String first_name;
    private String last_name;
    private String home_phone;
    private String work_phone;
    private String sex;
    private Timestamp birthdate;
    private String address_street;
    private String address_city;
    private String address_country;
    private Integer age_years;
    private Integer age_months;
    private Integer age_weeks;
    private Timestamp date_recpt;
    private Timestamp date_entered;
    private Timestamp date_collect;
    private String code_referer;
    private String referer;
    private String program;
    private String order_status;
    private String test;
    private String result;
    
    private String purpose_of_visit;
    private String countries_visited;
    private String flight;
    private String date_of_arrival;
    private String airline;
    private String nationality;
    private String seat;

    public ETLRecord() {
    }

}