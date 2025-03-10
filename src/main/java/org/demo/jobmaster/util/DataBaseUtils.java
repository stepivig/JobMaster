package org.demo.jobmaster.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Utility class for database operations.
 * <p>
 * This class provides methods to convert between database types and Java types.
 * </p>
 */
public final class DataBaseUtils {

    private DataBaseUtils() {
        // Utility class
    }

    public static final String DELIMITER = ",";

    /**
     * Converts a ResultSet column value to a List<String>.
     *
     * @param rs          the ResultSet
     * @param columnLabel the column label
     * @return the List<String> or null if the column value is null
     * @throws SQLException if an error occurs during conversion
     */
    public static List<String> toList(ResultSet rs, String columnLabel) throws SQLException {
        String customInputJson = rs.getString(columnLabel);
        if (customInputJson == null) {
            return null;
        }
        try {
            return List.of(customInputJson.split(DELIMITER));
        } catch (Exception e) {
            throw new SQLException("Failed to convert String to List<String>", e);
        }
    }

    /**
     * Converts a List<String> to a String representation.
     *
     * @param input the List<String>
     * @return the String representation
     */
    public static String toString(List<String> input) {
        if (input == null) {
            return null;
        }
        return String.join(DELIMITER, input);
    }
}