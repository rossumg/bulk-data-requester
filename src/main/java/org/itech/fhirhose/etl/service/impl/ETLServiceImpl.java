package org.itech.fhirhose.etl.service.impl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
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
		log.debug("createETLRecord: ");
		List<ETLRecord> etlRecordList = new ArrayList<>();

		List<Observation> observations = new ArrayList<>();
		for (Bundle bundle : searchBundles) {
			for (BundleEntryComponent entry : bundle.getEntry()) {
				observations.add((Observation) entry.getResource());
			}
		}
		log.debug("createETLRecord:observations:size: " + observations.size());
		etlRecordList = getLatestFhirforETL(observations);
		log.debug("createETLRecord:etlRecordList:size: " + etlRecordList.size());

		// add records to data mart
		if (etlRecordService.saveAll(etlRecordList)) {
			log.debug("createETLRecord:saveAll:success ");
		} else {
			log.debug("createETLRecord:saveAll:fail ");
		}

	}

	@Override
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
//	            log.debug( "glfe: " +   fhirObservation.getBasedOnFirstRep().getReference().toString());
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
					if (address != null) {
						for (j = 0; j < address.size(); ++j) {
							JSONObject jAddress = JSONUtils.getAsObject(address.get(j));
							org.json.simple.JSONArray jLines = JSONUtils.getAsArray(jAddress.get("line"));
							if (jLines.get(0) != null) {
								etlRecord.setAddress_street(jLines.get(0).toString());
							}
							if (jAddress.get("city") != null) {
								etlRecord.setAddress_city(jAddress.get("city").toString());
							}
							if (jAddress.get("country") != null) {
								etlRecord.setAddress_country(jAddress.get("country").toString());
							}
						}
					}

					org.json.simple.JSONArray telecom = JSONUtils.getAsArray(patientJson.get("telecom"));
					for (j = 0; j < telecom.size(); ++j) {
						JSONObject jTelecom = JSONUtils.getAsObject(telecom.get(j));

						if (jTelecom.get("system").toString().equalsIgnoreCase("other")
								&& jTelecom.get("value") != null) {
							etlRecord.setHome_phone(jTelecom.get("value").toString());
						} else if (jTelecom.get("system").toString().equalsIgnoreCase("sms")
								&& jTelecom.get("value") != null) {
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

//	                    reqRef = JSONUtils.getAsObject(srJson.get("locationReference"));
//	                    System.out.println("srReq:" + reqRef.get("system").toString());
//	                    System.out.println("srReq:" + reqRef.get("value").toString());
//	                    etlRecord.setCode_referer(reqRef.get("value").toString());

//	                    etlRecord.setReferer(fhirPractitioner.getName().get(0).getGivenAsSingleString() +
//	                            fhirPractitioner.getName().get(0).getFamily());

					code = JSONUtils.getAsObject(srJson.get("code"));
					coding = JSONUtils.getAsArray(code.get("coding"));
					for (j = 0; j < coding.size(); ++j) {
						// log.debug( "glfe: " + coding.get(0).toString());
						jCoding = JSONUtils.getAsObject(coding.get(0));
						// log.debug( "glfe: " + jCoding.get("system").toString());
//	                        log.debug( "glfe: " + jCoding.get("code").toString());
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
					LocalDate birthdate = LocalDate.parse(etlRecord.getBirthdate().toString().substring(0, 10));
					LocalDate date_entered = LocalDate.parse(etlRecord.getDate_entered().toString().substring(0, 10));
					if ((etlRecord.getBirthdate() != null) && (etlRecord.getDate_entered() != null)) {
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

					if (specimenJson.get("receivedTime") != null) {
						String timestampToDate = specimenJson.get("receivedTime").toString().substring(0, 10);
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date parsedDate = null;
						try {
							parsedDate = dateFormat.parse(timestampToDate);
						} catch (ParseException e) {
						}
						Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
						etlRecord.setDate_recpt(timestamp);
					}

					JSONObject jCollection = JSONUtils.getAsObject(specimenJson.get("collection"));
					if (jCollection != null) {
						JSONObject jCollectedDateTime = JSONUtils.getAsObject(jCollection.get("collectedDateTime"));
						try {
							String timestampToDate = jCollectedDateTime.toString().substring(0, 10);
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							Date parsedDate = dateFormat.parse(timestampToDate);
							Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
							etlRecord.setDate_collect(timestamp);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				String practitionerStr = fhirUtil.getFhirParser().encodeResourceToString(fhirPractitioner);
				JSONObject practitionerJson = null;
				practitionerJson = JSONUtils.getAsObject(practitionerStr);
				if (!JSONUtils.isEmpty(practitionerJson)) {
					org.json.simple.JSONArray name = JSONUtils.getAsArray(practitionerJson.get("name"));
					for (j = 0; j < name.size(); ++j) {
						JSONObject jName = JSONUtils.getAsObject(name.get(j));
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
