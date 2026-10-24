package com.leehonne.examtimeline.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class VerifyPurposeConverter implements AttributeConverter<VerifyPurpose, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VerifyPurpose attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public VerifyPurpose convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : VerifyPurpose.fromCode(dbData);
    }
}
