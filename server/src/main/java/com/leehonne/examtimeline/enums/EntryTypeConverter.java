package com.leehonne.examtimeline.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EntryTypeConverter implements AttributeConverter<EntryType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(EntryType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public EntryType convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : EntryType.fromCode(dbData);
    }
}
