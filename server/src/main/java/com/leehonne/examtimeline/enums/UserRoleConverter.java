package com.leehonne.examtimeline.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public UserRole convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : UserRole.fromCode(dbData);
    }
}
