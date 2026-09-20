package com.example.rest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private static final String DB_PATH = System.getProperty("soa.db",
            System.getProperty("user.home").replace('\\', '/') + "/soa-iot-demo-data/soa_iot");
    private static final String JDBC_URL = "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    static {
        System.setProperty("h2.bindAddress", System.getProperty("h2.bindAddress", "127.0.0.1"));
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJdbcUrl() {
        return "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=TRUE";
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    /** @param protocol which service front door delivered the reading ("REST" or "SOAP") */
    public static void save(SensorData data, String protocol) throws SQLException {
        String sql = "INSERT INTO sensor_data (device_id, humidity, pressure, moisture, recorded_at, protocol) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, data.getDeviceId());
            ps.setDouble(2, data.getHumidity());
            ps.setDouble(3, data.getPressure());
            ps.setDouble(4, data.getMoisture());
            ps.setString(5, protocol);
            ps.executeUpdate();
        }
    }

    public static List<SensorData> findByDevice(String deviceId) throws SQLException {
        return query("WHERE device_id = ?", deviceId);
    }

    /** Latest readings across all devices. */
    public static List<SensorData> findLatest() throws SQLException {
        return query("", null);
    }

    private static List<SensorData> query(String where, String param) throws SQLException {
        List<SensorData> list = new ArrayList<>();
        String sql = "SELECT device_id, humidity, pressure, moisture, recorded_at, protocol FROM sensor_data "
                   + where + " ORDER BY recorded_at DESC, id DESC LIMIT 20";
        try (Connection c = geConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SensorData d = new SensorData();
                    d.setDeviceId(rs.getString("device_id"));
                    d.setHumidity(rs.getDouble("humidity"));
                    d.setPressure(rs.getDouble("pressure"));
                    d.setMoisture(rs.getDouble("moisture"));
                    Timestamp ts = rs.getTimestamp("recorded_at");
                    d.setRecordedAt(ts == null ? null : ts.toLocalDateTime().toString());
                    d.setProtocol(rs.getString("protocol"));
                    list.add(d);
                }
            }
        }
        return list;
    }

    public static void ensureTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS sensor_data (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                device_id VARCHAR(64) NOT NULL,
                humidity DOUBLE NOT NULL,
                pressure DOUBLE NOT NULL,
                moisture DOUBLE NOT NULL,
                recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
            s.execute("ALTER TABLE sensor_data ADD COLUMN IF NOT EXISTS protocol VARCHAR(16)");
        }
    }
}
