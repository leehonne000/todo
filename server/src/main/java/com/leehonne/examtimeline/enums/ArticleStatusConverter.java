package com.leehonne.examtimeline.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ArticleStatusConverter implements AttributeConverter<ArticleStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ArticleStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ArticleStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : ArticleStatus.fromCode(dbData);
    }
}
