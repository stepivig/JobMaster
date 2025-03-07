package org.demo.jobmaster.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Abstract repository class for handling serialization and deserialization of JSON data
 * stored in database columns. Provides utility methods to convert between JSON strings
 * and Java Map objects.
 */
public abstract class SerializableObjectAbstractRepository {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a repository with an ObjectMapper for JSON processing.
     *
     * @param objectMapper the ObjectMapper instance used for serialization and deserialization
     */
    protected SerializableObjectAbstractRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes a JSON string from a database column into a Map.
     *
     * @param rs          the ResultSet containing the data
     * @param columnLabel the column label from which JSON data is retrieved
     * @return a Map representation of the JSON data, or null if the column value is null
     * @throws SQLException if JSON parsing fails
     */
    protected Map<String, String> deserialize(ResultSet rs, String columnLabel) throws SQLException {
        String customInputJson = rs.getString(columnLabel);
        if (customInputJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(customInputJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new SQLException("Failed to parse column data - " + columnLabel, e);
        }
    }

    /**
     * Serializes a Map into a JSON string for database storage.
     *
     * @param map the Map to serialize
     * @return a JSON string representation of the map
     * @throws RuntimeException if serialization fails
     */
    protected String serialize(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize codeInput", e);
        }
    }
}