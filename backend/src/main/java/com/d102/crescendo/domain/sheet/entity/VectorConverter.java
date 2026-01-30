package com.d102.crescendo.domain.sheet.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.Arrays;

@Slf4j
@Converter
public class VectorConverter implements AttributeConverter<float[], Object> {

    @Override
    public Object convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(formatVector(attribute));
            return pgObject;
        } catch (SQLException e) {
            log.error("Vector 직렬화 실패", e);
            throw new RuntimeException("Vector 직렬화 실패", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            String vectorString;
            if (dbData instanceof PGobject) {
                vectorString = ((PGobject) dbData).getValue();
            } else if (dbData instanceof String) {
                vectorString = (String) dbData;
            } else {
                log.error("예상치 못한 타입: {}", dbData.getClass().getName());
                return null;
            }

            return parseVector(vectorString);
        } catch (Exception e) {
            log.error("Vector 역직렬화 실패", e);
            throw new RuntimeException("Vector 역직렬화 실패", e);
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return null;
        }
        // "[1.0,2.0,3.0]" 형식을 파싱
        String cleaned = vectorString.trim();
        if (cleaned.startsWith("[")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}