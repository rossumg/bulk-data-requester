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
    private String mobile_phone;
    private String sex;
    private Timestamp birthdate;
    
    private String address_apartment_number;
    private String address_street;
    private String address_city;
    private String address_state_province;
    private String address_country;
    private String address_zip_postal_code;
    
    private String temp_address_hotel_name;
    private String temp_address_apartment_number;
    private String temp_address_number_and_street;
    private String temp_address_city;
    private String temp_address_state_province;
    private String temp_address_country;
    private String temp_address_zip_postal_code;
    
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
    private String health_office;
    private String testKitId;
    private String originalServiceRequestId;
    
    private Boolean previous_infection;
    private Boolean fever;
    private Boolean sore_throat;
    private Boolean joint_pain;
    private Boolean cough;
    private Boolean breathing_difficulty;
    private Boolean rash;
    private Boolean sense_of_smell_or_taste;
    private Boolean contact_with_infected_individual;

    public ETLRecord() {
    }

}