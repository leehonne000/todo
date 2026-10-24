package com.leehonne.examtimeline.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ReviewStatusConverter implements AttributeConverter<ReviewStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ReviewStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ReviewStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : ReviewStatus.fromCode(dbData);
    }
}
