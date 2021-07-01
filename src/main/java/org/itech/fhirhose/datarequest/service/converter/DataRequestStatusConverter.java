package org.itech.fhirhose.datarequest.service.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.itech.fhirhose.datarequest.model.DataRequestAttempt.DataRequestStatus;


@Converter(autoApply = true)
public class DataRequestStatusConverter implements AttributeConverter<DataRequestStatus, Character> {

	@Override
	public Character convertToDatabaseColumn(DataRequestStatus dataRequestType) {
		if (dataRequestType == null) {
			return null;
		}
		return dataRequestType.getCode();
	}

	@Override
	public DataRequestStatus convertToEntityAttribute(Character code) {
		if (code == null) {
			return null;
		}

		return Stream.of(DataRequestStatus.values()).filter(dataRequestType -> code.equals(dataRequestType.getCode())).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
