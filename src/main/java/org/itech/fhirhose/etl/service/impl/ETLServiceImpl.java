package org.itech.fhirhose.etl.service.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.itech.fhirhose.etl.model.ETLRecord;
import org.itech.fhirhose.etl.service.ETLRecordService;
import org.itech.fhirhose.etl.service.ETLService;
import org.itech.fhirhose.fhir.FhirUtil;
import org.itech.fhirhose.util.JSONUtils;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ETLServiceImpl implements ETLService {

	private ETLRecordService etlRecordService;
	private FhirUtil fhirUtil;

	public ETLServiceImpl(ETLRecordService etlRecordService, FhirUtil fhirUtil) {
		this.etlRecordService = etlRecordService;
		this.fhirUtil = fhirUtil;
	}

	@Override
	public void createPersistETLRecords(List<Bundle> searchBundles) {
		log.debug("createPersistETLRecords: ");
		if (searchBundles != null && searchBundles.size() > 0) {

			for (Bundle bundle : searchBundles) {
				int numObservations = searchBundles.get(0).getEntry().size();
				List<Observation> observations = new ArrayList<>(numObservations);
				for (BundleEntryComponent entry : bundle.getEntry()) {
					observations.add((Observation) entry.getResource());
				}
				log.debug("observations:size: " + observations.size());
				List<ETLRecord> etlRecordList = convertToEtlRecords(observations);
				log.debug("etlRecordList:size: " + etlRecordList.size());

				// add records to data mart
				if (etlRecordService.saveAll(etlRecordList)) {
					log.debug("saveAll:success ");
				} else {
					log.debug("saveAll:fail ");
				}
			}

		}

	}

	private List<ETLRecord> convertToEtlRecords(List<Observation> observations) {
		log.debug("convertToEtlRecords:observations:size: " + observations.size());

		List<ETLRecord> etlRecordList = new ArrayList<>(observations.size());
		IGenericClient localFhirClient = fhirUtil.getLocalFhirClient();

		int numFail = 0;
		// csl
		int i = -1;
		for (Observation observation : observations) {
			try {
				log.trace("convertToEtlRecords:observation " + ++i);
				if (i % 100 == 0) {
					log.debug("convertToEtlRecords:observation " + i);
				}
				Observation fhirObservation = observation;
				Patient fhirPatient = new Patient();
				ServiceRequest fhirServiceRequest = new ServiceRequest();
				Practitioner fhirPractitioner = new Practitioner();
				Organization fhirOrganization = new Organization();
				Specimen fhirSpecimen = new Specimen();
				QuestionnaireResponse fhirQuestionnaireResponse = new QuestionnaireResponse();

				log.trace("observation: " + fhirUtil.getFhirParser().encodeResourceToString(fhirObservation));
				log.trace("observation based on id: " + fhirObservation.getBasedOnFirstRep().getReference());
				// get ServiceRequest
				if (hasReference(fhirObservation.getBasedOnFirstRep())) {
					String srString = fhirObservation.getBasedOnFirstRep().getReference();
					log.trace("reading " + srString);
					fhirServiceRequest = localFhirClient.read()//
							.resource(ServiceRequest.class)//
							.withId(srString)//
							.execute();
					log.trace("fhirServiceRequest: "
							+ fhirUtil.getFhirParser().encodeResourceToString(fhirServiceRequest));
				} else {
					log.error("observation with id: " + fhirObservation.getIdElement().getIdPart()
							+ " is missing a service request");
                }

                // get QuestionnaireResponse
				String srString = fhirServiceRequest.getBasedOnFirstRep().getReference();
				log.debug("convertToEtlRecords:search for QR.basedOn: " + srString);
				Bundle bundle = localFhirClient.search()//
			                .forResource(QuestionnaireResponse.class)//
			                .returnBundle(Bundle.class)//
			                .where(QuestionnaireResponse.BASED_ON.hasAnyOfIds(srString))//
			                .execute();
				if (bundle.hasEntry()) {
				     fhirQuestionnaireResponse = (QuestionnaireResponse) bundle.getEntryFirstRep().getResource();
			    } else {
                    log.error("QuestionnaireResponse with based on: " + srString + " is missing");
                }
				
                // get Organization
                if (hasReference(fhirServiceRequest.getLocationReferenceFirstRep())) {
					String oString = fhirServiceRequest.getLocationReferenceFirstRep().getReference();
					log.trace("reading " + oString);
					fhirOrganization = localFhirClient.read()//
							.resource(Organization.class)//
							.withId(oString)//
							.execute();
				} else {
					log.warn("serviceRequest with id: " + fhirServiceRequest.getIdElement().getIdPart()
							+ " is missing a location reference");
				}

				// get Practitioner
				if (hasReference(fhirServiceRequest.getRequester())) {
					String pracString = fhirServiceRequest.getRequester().getReference();
					log.trace("reading " + pracString);
					fhirPractitioner = localFhirClient.read()//
							.resource(Practitioner.class)//
							.withId(pracString)//
							.execute();
				} else {
					log.warn("serviceRequest with id: " + fhirServiceRequest.getIdElement().getIdPart()
							+ " is missing a requester reference");
				}

				// get Patient
				if (hasReference(fhirObservation.getSubject())) {
					String patString = fhirObservation.getSubject().getReference();
					log.trace("reading " + patString);
					fhirPatient = localFhirClient.read()//
							.resource(Patient.class)//
							.withId(patString)//
							.execute();
				} else {
					log.warn("serviceRequest with id: " + fhirServiceRequest.getIdElement().getIdPart()
							+ " is missing a subject reference");
				}

				// get Specimen
				if (hasReference(fhirObservation.getSpecimen())) {
					String spString = fhirObservation.getSpecimen().getReference();
					log.trace("reading " + spString);
					fhirSpecimen = localFhirClient.read()//
							.resource(Specimen.class)//
							.withId(spString)//
							.execute();
				} else {
					log.warn("serviceRequest with id: " + fhirServiceRequest.getIdElement().getIdPart()
							+ " is missing a specimen reference");
				}

				ETLRecord etlRecord = convertoToETLRecord(fhirObservation, fhirPatient, fhirServiceRequest,
						fhirQuestionnaireResponse, fhirOrganization, fhirPractitioner, fhirSpecimen);

				etlRecordList.add(etlRecord);
			} catch (RuntimeException e) {
				numFail++;
				log.error("exception converting observation: " + observation.getIdElement().getIdPart());
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				log.error(sw.toString());
			}
		}
		if (numFail > 0) {
			log.warn("number of failed observation conversions: " + numFail);
		}

		return etlRecordList;
	}

	private boolean hasReference(Reference reference) {
		return reference != null && reference.hasReference()
				&& !GenericValidator.isBlankOrNull(StringUtils.trimToNull(reference.getReferenceElement().getIdPart()));
	}

	private ETLRecord convertoToETLRecord(Observation fhirObservation, Patient fhirPatient,
			ServiceRequest fhirServiceRequest, QuestionnaireResponse fhirQuestionnaireResponse, Organization fhirOrganization, Practitioner fhirPractitioner,
			Specimen fhirSpecimen) {
		log.trace("convertoToETLRecord");
		ETLRecord etlRecord = new ETLRecord();
		etlRecord.setData(fhirUtil.getFhirParser().encodeResourceToString(fhirObservation));
		putObservationValuesIntoETLRecord(etlRecord, fhirObservation);
		putServiceRequestValuesIntoETLRecord(etlRecord, fhirServiceRequest);
		putQuestionnaireResponseValuesIntoETLRecord(etlRecord, fhirQuestionnaireResponse);
		putOrganizationValuesIntoETLRecord(etlRecord, fhirOrganization);
		putPatientValuesIntoETLRecord(etlRecord, fhirPatient);
		putSpecimenValuesIntoETLRecord(etlRecord, fhirSpecimen);
		putPractitionerValuesIntoETLRecord(etlRecord, fhirPractitioner);
		return etlRecord;
	}

	private void putPractitionerValuesIntoETLRecord(ETLRecord etlRecord, Practitioner fhirPractitioner) {
		log.trace("putPractitionerValuesIntoETLRecord");
//		if (fhirPractitioner.hasName()) {
//			etlRecord.setReferer(fhirPractitioner.getNameFirstRep().getGivenAsSingleString() + " "
//					+ fhirPractitioner.getNameFirstRep().getFamily());
//		}
	}
	
    private void putQuestionnaireResponseValuesIntoETLRecord(ETLRecord etlRecord,
            QuestionnaireResponse fhirQuestionnaireResponse) {
        log.trace("putQuestionnaireResponseValuesIntoETLRecord");
        if (fhirQuestionnaireResponse.hasItem()) {
            for (QuestionnaireResponseItemComponent item : fhirQuestionnaireResponse.getItem()) {
                switch (item.getText()) {
                case "Reason for Visit":
                    break;
                case "Countries Vistied within 6 Months":
                    etlRecord.setCountries_visited(item.getAnswerFirstRep().toString());
                    break;
                case "Flight":
                    etlRecord.setFlight(item.getAnswerFirstRep().toString());
                    break;
                case "Date of Arrival":
                    break;
                case "Airline":
                    etlRecord.setAirline(item.getAnswerFirstRep().toString());
                    break;
                case "Nationality":
                    etlRecord.setNationality(item.getAnswerFirstRep().toString());
                    break;
                case "Seat":
                    etlRecord.setSeat(item.getAnswerFirstRep().toString());
                    break;
                case "Address in MU":
                    break;

                }
            }
        }
    }

	private void putSpecimenValuesIntoETLRecord(ETLRecord etlRecord, Specimen fhirSpecimen) {
		log.trace("putSpecimenValuesIntoETLRecord");
		if (fhirSpecimen.hasReceivedTime()) {
			etlRecord.setDate_recpt(new Timestamp(fhirSpecimen.getReceivedTime().getTime()));
		}
		if (fhirSpecimen.hasCollection() && fhirSpecimen.getCollection().hasCollectedDateTimeType()) {
			DateTimeType collectionTime = fhirSpecimen.getCollection().getCollectedDateTimeType();
			etlRecord.setDate_collect(new Timestamp(collectionTime.getValue().getTime()));
		}
	}

	private void putObservationValuesIntoETLRecord(ETLRecord etlRecord, Observation fhirObservation) {
		log.trace("putObservationValuesIntoETLRecord");
		if (fhirObservation.hasValue()) {
			Type value = fhirObservation.getValue();
			if (value instanceof CodeableConcept) {
				etlRecord.setResult(fhirObservation.getValueCodeableConcept().getCodingFirstRep().getDisplay());
			} else if (value instanceof IntegerType) {
				etlRecord.setResult(fhirObservation.getValueIntegerType().asStringValue());
			} else if (value instanceof Quantity) {
				etlRecord.setResult(fhirObservation.getValueQuantity().getValue().toPlainString());
			} else if (value instanceof StringType) {
				etlRecord.setResult(fhirObservation.getValueStringType().asStringValue());
			}
		}

		if (fhirObservation.hasStatus()) {
			etlRecord.setOrder_status(fhirObservation.getStatus().toString());
		}

		etlRecord.setExternalId(fhirObservation.getIdElement().getIdPart());
	}

	private void putPatientValuesIntoETLRecord(ETLRecord etlRecord, Patient fhirPatient) {
		log.trace("putPatientValuesIntoETLRecord");
		etlRecord.setPatientId(fhirPatient.getIdElement().getIdPart());
		if (fhirPatient.hasIdentifier()) {
			for (Identifier identifier : fhirPatient.getIdentifier()) {
				if (identifier.getSystem().equalsIgnoreCase("http://openelis-global.org/pat_nationalId")) {
					etlRecord.setIdentifier(identifier.getValue());
				}
			}
		}

		if (fhirPatient.hasGender()) {
			etlRecord.setSex(fhirPatient.getGender().toString());
		}

		if (fhirPatient.hasBirthDate()) {
			etlRecord.setBirthdate(new java.sql.Timestamp(fhirPatient.getBirthDate().getTime()));
		}

		if (fhirPatient.hasName()) {
			HumanName name = fhirPatient.getNameFirstRep();
			if (name.hasFamily()) {
				etlRecord.setLast_name(name.getFamily());
			}
			if (name.hasGiven()) {
				etlRecord.setFirst_name(name.getGivenAsSingleString());
			}
		}

		if (fhirPatient.hasAddress()) {
			Address address = fhirPatient.getAddressFirstRep();
			etlRecord.setAddress_street(StringUtils
					.join(address.getLine().stream().map(e -> e.asStringValue()).collect(Collectors.toList()), ", "));
			etlRecord.setAddress_city(address.getCity());
			etlRecord.setAddress_country(address.getCountry());
		}

		if (fhirPatient.hasTelecom()) {
			for (ContactPoint contact : fhirPatient.getTelecom()) {
				if (ContactPointUse.HOME.equals(contact.getUse())) {
					etlRecord.setHome_phone(contact.getValue());
				} else if (ContactPointUse.WORK.equals(contact.getUse())) {
					etlRecord.setWork_phone(contact.getValue());
				}
			}
		}
		if ((etlRecord.getBirthdate() != null) && (etlRecord.getDate_entered() != null)) {
			LocalDate birthdate = etlRecord.getBirthdate().toLocalDateTime().toLocalDate();
			LocalDate date_entered = etlRecord.getDate_entered().toLocalDateTime().toLocalDate();
			int age_days = Period.between(birthdate, date_entered).getDays();
			int age_years = Period.between(birthdate, date_entered).getYears();
			int age_months = Period.between(birthdate, date_entered).getMonths();
			int age_weeks = Math.round(age_days) / 7;

			if (age_days > 3) {
				etlRecord.setAge_weeks(age_weeks + 1);
			}
			if (age_weeks > 2) {
				etlRecord.setAge_months(age_months + 1);
			}
			etlRecord.setAge_years((age_months > 5) ? age_years + 1 : age_years);
			etlRecord.setAge_months((12 * age_years) + age_months);
			etlRecord.setAge_weeks((52 * age_years) + (4 * age_months) + age_weeks);
		}

	}

	private void putServiceRequestValuesIntoETLRecord(ETLRecord etlRecord, ServiceRequest fhirServiceRequest) {
		log.trace("putServiceRequestValuesIntoETLRecord");
		if (fhirServiceRequest.hasRequisition()) {
			etlRecord.setLabno(fhirServiceRequest.getRequisition().getValue());
		}
		if (fhirServiceRequest.hasCategory()) {
			etlRecord.setProgram(fhirServiceRequest.getCategoryFirstRep().getCodingFirstRep().getDisplay());
		}
		if (fhirServiceRequest.hasCode()) {
			etlRecord.setTest(fhirServiceRequest.getCode().getCodingFirstRep().getDisplay());
		}
		if (fhirServiceRequest.hasAuthoredOn()) {
			etlRecord.setDate_entered(new Timestamp(fhirServiceRequest.getAuthoredOn().getTime()));
		}
	}

	private void putOrganizationValuesIntoETLRecord(ETLRecord etlRecord, Organization fhirOrganization) {
		log.trace("putOrganizationValuesIntoETLRecord");
		if (fhirOrganization.hasName()) {
			etlRecord.setReferer(fhirOrganization.getName());
		}
	}

	public List<ETLRecord> getLatestFhirforETL(List<Observation> observations) {
		log.debug("getLatestFhirforETL:size: " + observations.size());

		List<ETLRecord> etlRecordList = new ArrayList<>();

		org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
		org.hl7.fhir.r4.model.Observation fhirObservation = new org.hl7.fhir.r4.model.Observation();
		org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = new org.hl7.fhir.r4.model.ServiceRequest();
		org.hl7.fhir.r4.model.Practitioner fhirPractitioner = new org.hl7.fhir.r4.model.Practitioner();
		org.hl7.fhir.r4.model.Specimen fhirSpecimen = new org.hl7.fhir.r4.model.Specimen();
		IGenericClient localFhirClient = fhirUtil.getLocalFhirClient();
		org.json.simple.JSONObject code = null;
		org.json.simple.JSONArray coding = null;
		org.json.simple.JSONObject jCoding = null;

		// gnr

		for (Observation observation : observations) {
			fhirObservation = observation;
			System.out.println("observation: " + fhirUtil.getFhirParser().encodeResourceToString(fhirObservation));
//            log.debug( "glfe: " +   fhirObservation.getBasedOnFirstRep().getReference().toString());
			String srString = fhirObservation.getBasedOnFirstRep().getReference().toString();
			String srUuidString = srString.substring(srString.lastIndexOf("/") + 1);

			// sr, prac
			Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
					.where(new TokenClientParam("_id").exactly().code(srUuidString)).prettyPrint().execute();

			if (srBundle.hasEntry()) {
				fhirServiceRequest = (ServiceRequest) srBundle.getEntryFirstRep().getResource();
				// log.debug( "glfe:fhirServiceRequest: " +
				// fhirUtil.getFhirParser().encodeResourceToString(fhirServiceRequest));
				// get Practitioner
				String pracString = fhirServiceRequest.getRequester().getReference().toString();
				String pracUuidString = pracString.substring(pracString.lastIndexOf("/") + 1);
				// log.debug( "glfe:fhirServiceRequest: " + pracString + " " + pracUuidString);
				Bundle pracBundle = (Bundle) localFhirClient.search().forResource(Practitioner.class)
						.where(new TokenClientParam("_id").exactly().code(pracUuidString)).prettyPrint().execute();

				if (pracBundle.hasEntry()) {
					fhirPractitioner = (Practitioner) pracBundle.getEntryFirstRep().getResource();
					// log.debug( "glfe:fhirPractitioner: " +
					// fhirUtil.getFhirParser().encodeResourceToString(fhirPractitioner));
				} else {
					// log.debug( "glfe: NO PRACTITIONER ");
				}
			}

			String patString = fhirObservation.getSubject().getReference().toString();
			String patUuidString = patString.substring(patString.lastIndexOf("/") + 1);

			Bundle patBundle = (Bundle) localFhirClient.search().forResource(org.hl7.fhir.r4.model.Patient.class)
					.where(new TokenClientParam("_id").exactly().code(patUuidString)).prettyPrint().execute();

			if (patBundle.hasEntry()) {
				fhirPatient = (org.hl7.fhir.r4.model.Patient) patBundle.getEntryFirstRep().getResource();
			}

			// sp
			String spString = fhirObservation.getSpecimen().getReference().toString();
			String spUuidString = spString.substring(spString.lastIndexOf("/") + 1);

			Bundle spBundle = (Bundle) localFhirClient.search().forResource(Specimen.class)
					.where(new TokenClientParam("_id").exactly().code(spUuidString)).prettyPrint().execute();

			if (spBundle.hasEntry()) {
				fhirSpecimen = (Specimen) spBundle.getEntryFirstRep().getResource();
			}

			JSONObject jResultUUID = null;
			JSONObject jSRRef = null;
			JSONObject reqRef = null;

			int j = 0;

			ETLRecord etlRecord = new ETLRecord();
			try {
				String observationStr = fhirUtil.getFhirParser().encodeResourceToString(fhirObservation);
				// log.debug( "glfe: " + observationStr);
				JSONObject observationJson = null;
				observationJson = JSONUtils.getAsObject(observationStr);
				// log.debug( "glfe: " + observationJson.toString());
				if (!JSONUtils.isEmpty(observationJson)) {

					org.json.simple.JSONArray identifier = JSONUtils.getAsArray(observationJson.get("identifier"));
					for (j = 0; j < identifier.size(); ++j) {
						// log.debug( "glfe: " + identifier.get(j).toString());
						jResultUUID = JSONUtils.getAsObject(identifier.get(j));
						// log.debug( "glfe: " + jResultUUID.get("system").toString());
						// log.debug( "glfe: " + jResultUUID.get("value").toString());
					}
					try {
						code = JSONUtils.getAsObject(observationJson.get("valueCodeableConcept"));
						coding = JSONUtils.getAsArray(code.get("coding"));
						for (j = 0; j < coding.size(); ++j) {
							// log.debug( "glfe: " + coding.get(0).toString());
							jCoding = JSONUtils.getAsObject(coding.get(0));
							// log.debug( "glfe: " + jCoding.get("system").toString());
							// log.debug( "glfe: " + jCoding.get("code").toString());
							// log.debug( "glfe: " + jCoding.get("display").toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					etlRecord.setResult(jCoding.get("display").toString());
					// log.debug( "glfe: " + observationJson.get("subject").toString());

					JSONObject subjectRef = null;
					subjectRef = JSONUtils.getAsObject(observationJson.get("subject"));
					// log.debug( "glfe: " + subjectRef.get("reference").toString());

					JSONObject specimenRef = null;
					specimenRef = JSONUtils.getAsObject(observationJson.get("specimen"));
					// log.debug( "glfe: " + specimenRef.get("reference").toString());

					org.json.simple.JSONArray serviceRequestRef = null;
					serviceRequestRef = JSONUtils.getAsArray(observationJson.get("basedOn"));
					for (j = 0; j < serviceRequestRef.size(); ++j) {
						// log.debug( "glfe: " + serviceRequestRef.get(j).toString());
						jSRRef = JSONUtils.getAsObject(serviceRequestRef.get(j));
						// log.debug( "glfe: " + jSRRef.get("reference").toString());
					}
					etlRecord.setOrder_status(observationJson.get("status").toString());
					etlRecord.setData(observationStr);
				}

				String patientStr = fhirUtil.getFhirParser().encodeResourceToString(fhirPatient);
				JSONObject patientJson = null;
				patientJson = JSONUtils.getAsObject(patientStr);
				// log.debug( "glfe: " + patientJson.toString());
				if (!JSONUtils.isEmpty(patientJson)) {

					org.json.simple.JSONArray identifier = JSONUtils.getAsArray(patientJson.get("identifier"));
					for (j = 0; j < identifier.size(); ++j) {
						// log.debug( "glfe: " + identifier.get(j).toString());
						JSONObject patIds = JSONUtils.getAsObject(identifier.get(j));
						// log.debug( "glfe: " + patIds.get("system").toString());
						// log.debug( "glfe: " + patIds.get("value").toString());
						if (patIds.get("system").toString()
								.equalsIgnoreCase("http://openelis-global.org/pat_nationalId")) {
							etlRecord.setIdentifier(patIds.get("value").toString());
						}
						etlRecord.setSex(patientJson.get("gender").toString());
						// 1994
						try {
							// String timestampToDate =
							// patientJson.get("birthDate").toString().substring(0,10);
							String timestampToDate = patientJson.get("birthDate").toString().substring(0, 4);
							timestampToDate = timestampToDate + "-01-01";
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							Date parsedDate = dateFormat.parse(timestampToDate);
							Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
							etlRecord.setBirthdate(timestamp);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					org.json.simple.JSONArray name = JSONUtils.getAsArray(patientJson.get("name"));
					for (j = 0; j < name.size(); ++j) {
						// log.debug( "glfe: " + name.get(j).toString());
						JSONObject jName = JSONUtils.getAsObject(name.get(j));
						// log.debug( "glfe: " + jName.get("family").toString());
						// log.debug( "glfe: " + jName.get("given").toString());
						etlRecord.setLast_name(jName.get("family").toString());
						org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
						etlRecord.setFirst_name(givenName.get(0).toString());
					}

					org.json.simple.JSONArray address = JSONUtils.getAsArray(patientJson.get("address"));
					for (j = 0; j < address.size(); ++j) {
						JSONObject jAddress = JSONUtils.getAsObject(address.get(j));
						org.json.simple.JSONArray jLines = JSONUtils.getAsArray(jAddress.get("line"));
						etlRecord.setAddress_street(jLines.get(0).toString());
						etlRecord.setAddress_city(jAddress.get("city").toString());
//                        etlRecord.setAddress_country(jAddress.get("country").toString());
					}

					org.json.simple.JSONArray telecom = JSONUtils.getAsArray(patientJson.get("telecom"));
					for (j = 0; j < telecom.size(); ++j) {
						// log.debug( "glfe: " + telecom.get(j).toString());
						JSONObject jTelecom = JSONUtils.getAsObject(telecom.get(j));

						if (jTelecom.get("system").toString().equalsIgnoreCase("other")) {
							// log.debug( "glfe: " + jTelecom.get("system").toString());
							// log.debug( "glfe: " + jTelecom.get("value").toString());
							etlRecord.setHome_phone(jTelecom.get("value").toString());
						} else if (jTelecom.get("system").toString().equalsIgnoreCase("sms")) {
							// log.debug( "glfe: " + jTelecom.get("system").toString());
							// log.debug( "glfe: " + jTelecom.get("value").toString());
							etlRecord.setWork_phone(jTelecom.get("value").toString());
						}
					}
					etlRecord.setPatientId(fhirPatient.getId());
				}

				String serviceRequestStr = fhirUtil.getFhirParser().encodeResourceToString(fhirServiceRequest);
				// log.debug( "glfe: " + serviceRequestStr);
				JSONObject srJson = null;
				srJson = JSONUtils.getAsObject(serviceRequestStr);
				// log.debug( "glfe: " + srJson.toString());

				if (!JSONUtils.isEmpty(srJson)) {

					org.json.simple.JSONArray identifier = JSONUtils.getAsArray(srJson.get("identifier"));
					for (j = 0; j < identifier.size(); ++j) {
						// log.debug( "glfe: " + identifier.get(j).toString());
						JSONObject srIds = JSONUtils.getAsObject(identifier.get(j));
						// log.debug( "glfe: " + srIds.get("system").toString());
						// log.debug( "glfe: " + srIds.get("value").toString());
					}

					reqRef = JSONUtils.getAsObject(srJson.get("requisition"));
					// log.debug( "glfe: " + reqRef.get("system").toString());
					// log.debug( "glfe: " + reqRef.get("value").toString());
					etlRecord.setLabno(reqRef.get("value").toString());

					code = JSONUtils.getAsObject(srJson.get("code"));
					coding = JSONUtils.getAsArray(code.get("coding"));

					org.json.simple.JSONArray jCategoryArray = JSONUtils.getAsArray(srJson.get("category"));
					// log.debug( "glfe: " + jCategoryArray.get(0).toString());
					JSONObject jCatJson = (JSONObject) jCategoryArray.get(0);
					coding = JSONUtils.getAsArray(jCatJson.get("coding"));
					for (j = 0; j < coding.size(); ++j) {
						jCoding = (JSONObject) coding.get(j);
						// log.debug( "glfe: " + jCoding.get("system").toString());
						// log.debug( "glfe: " + jCoding.get("code").toString());
						// log.debug( "glfe: " + jCoding.get("display").toString());
					}
					etlRecord.setProgram(jCoding.get("display").toString());

//                    reqRef = JSONUtils.getAsObject(srJson.get("locationReference"));
//                    System.out.println("srReq:" + reqRef.get("system").toString());
//                    System.out.println("srReq:" + reqRef.get("value").toString());
//                    etlRecord.setCode_referer(reqRef.get("value").toString());

//                    etlRecord.setReferer(fhirPractitioner.getName().get(0).getGivenAsSingleString() +
//                            fhirPractitioner.getName().get(0).getFamily());

					code = JSONUtils.getAsObject(srJson.get("code"));
					coding = JSONUtils.getAsArray(code.get("coding"));
					for (j = 0; j < coding.size(); ++j) {
						// log.debug( "glfe: " + coding.get(0).toString());
						jCoding = JSONUtils.getAsObject(coding.get(0));
						// log.debug( "glfe: " + jCoding.get("system").toString());
//                        log.debug( "glfe: " + jCoding.get("code").toString());
						// log.debug( "glfe: " + jCoding.get("display").toString());
					}

					etlRecord.setTest(jCoding.get("display").toString()); // test description

					// 2021-05-06T12:51:58-07:00
					try {
						String timestampToDate = srJson.get("authoredOn").toString().substring(0, 10);
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date parsedDate = dateFormat.parse(timestampToDate);
						Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
						etlRecord.setDate_entered(timestamp);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// done here because data_entered is used for age
					if ((etlRecord.getBirthdate() != null) && (etlRecord.getDate_entered() != null)) {
						LocalDate birthdate = LocalDate.parse(etlRecord.getBirthdate().toString().substring(0, 10));
						LocalDate date_entered = LocalDate
								.parse(etlRecord.getDate_entered().toString().substring(0, 10));
						int age_days = Period.between(birthdate, date_entered).getDays();
						int age_years = Period.between(birthdate, date_entered).getYears();
						int age_months = Period.between(birthdate, date_entered).getMonths();
						int age_weeks = Math.round(age_days) / 7;

						if (age_days > 3) {
							etlRecord.setAge_weeks(age_weeks + 1);
						}
						if (age_weeks > 2) {
							etlRecord.setAge_months(age_months + 1);
						}
						etlRecord.setAge_years((age_months > 5) ? age_years + 1 : age_years);
						etlRecord.setAge_months((12 * age_years) + age_months);
						etlRecord.setAge_weeks((52 * age_years) + (4 * age_months) + age_weeks);
					}
				}

				String specimenStr = fhirUtil.getFhirParser().encodeResourceToString(fhirSpecimen);
				// log.debug( "glfe: " + specimenStr);
				JSONObject specimenJson = null;
				specimenJson = JSONUtils.getAsObject(specimenStr);
				// log.debug( "glfe: " + specimenJson.toString());
				if (!JSONUtils.isEmpty(specimenJson)) {

					org.json.simple.JSONArray identifier = JSONUtils.getAsArray(specimenJson.get("identifier"));
					for (j = 0; j < identifier.size(); ++j) {
						// log.debug( "glfe: " + identifier.get(j).toString());
						JSONObject specimenId = JSONUtils.getAsObject(identifier.get(j));
						// log.debug( "glfe: " + specimenId.get("system").toString());
						// log.debug( "glfe: " + specimenId.get("value").toString());
					}

					code = JSONUtils.getAsObject(specimenJson.get("type"));
					coding = JSONUtils.getAsArray(code.get("coding"));
					for (j = 0; j < coding.size(); ++j) {
						// log.debug( "glfe: " + coding.get(0).toString());
						jCoding = JSONUtils.getAsObject(coding.get(0));
						// log.debug( "glfe: " + jCoding.get("system").toString());
						// log.debug( "glfe: " + jCoding.get("code").toString());
						// log.debug( "glfe: " + jCoding.get("display").toString());
					}
					// 2021-04-29T16:58:51-07:00
					try {
						String timestampToDate = specimenJson.get("receivedTime").toString().substring(0, 10);
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date parsedDate = dateFormat.parse(timestampToDate);
						Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
						etlRecord.setDate_recpt(timestamp);
					} catch (Exception e) {
						e.printStackTrace();
					}

					JSONObject jCollection = JSONUtils.getAsObject(specimenJson.get("collection"));
//                    JSONObject jCollectedDateTime = JSONUtils.getAsObject(jCollection.get("collectedDateTime"));
					// log.debug( "glfe: " + jCollection.get("collectedDateTime").toString());
					try {
						String timestampToDate = jCollection.get("collectedDateTime").toString().substring(0, 10);
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date parsedDate = dateFormat.parse(timestampToDate);
						Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
						etlRecord.setDate_collect(timestamp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				String practitionerStr = fhirUtil.getFhirParser().encodeResourceToString(fhirPractitioner);
				// log.debug( "glfe:practitionerStr: " + practitionerStr);
				JSONObject practitionerJson = null;
				practitionerJson = JSONUtils.getAsObject(practitionerStr);
				// log.debug( "glfe: " + practitionerJson.toString());
				if (!JSONUtils.isEmpty(practitionerJson)) {
					org.json.simple.JSONArray name = JSONUtils.getAsArray(practitionerJson.get("name"));
					for (j = 0; j < name.size(); ++j) {
						// log.debug( "glfe: " + name.get(j).toString());
						JSONObject jName = JSONUtils.getAsObject(name.get(j));
						// log.debug( "glfe: " + jName.get("family").toString());
						// log.debug( "glfe: " + jName.get("given").toString());
						org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
						etlRecord.setReferer(givenName.get(0).toString() + " " + jName.get("family").toString());
					}
				}
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}

			etlRecordList.add(etlRecord);
		}

		return etlRecordList;
	}

}
