package com.example.sensor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    // H2 file-based database (data is saved on disk)
    private static final String DB_PATH = System.getProperty("soa.db",
            System.getProperty("user.home").replace('\\', '/') + "/soa-iot-demo-data/soa_iot");
    private static final String JDBC_URL = "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    static {
        // AUTO_SERVER lets the SOAP and REST processes share the file. By default H2 advertises the
        // LAN IP, which Windows Firewall / VPNs often block ("Permission denied: getsockopt").
        System.setProperty("h2.bindAddress", System.getProperty("h2.bindAddress", "127.0.0.1"));
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC Driver not found", e);
        }
    }

    public static String getJdbcUrl() {
        return "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=TRUE";
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    public static void saveSensorData(SensorData data) throws SQLException {
        // "protocol" records which service front door delivered the reading (shown on the dashboard)
        String sql = "INSERT INTO sensor_data (device_id, humidity, pressure, moisture, recorded_at, protocol) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'SOAP')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getDeviceId());
            ps.setDouble(2, data.getHumidity());
            ps.setDouble(3, data.getPressure());
            ps.setDouble(4, data.getMoisture());
            ps.executeUpdate();
        }
    }

    public static List<SensorData> getSensorDataByDevice(String deviceId) throws SQLException {
        List<SensorData> list = new ArrayList<>();
        String sql = "SELECT device_id, humidity, pressure, moisture FROM sensor_data " +
                     "WHERE device_id = ? ORDER BY recorded_at DESC LIMIT 20";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SensorData d = new SensorData();
                    d.setDeviceId(rs.getString("device_id"));
                    d.setHumidity(rs.getDouble("humidity"));
                    d.setPressure(rs.getDouble("pressure"));
                    d.setMoisture(rs.getDouble("moisture"));
                    list.add(d);
                }
            }
        }
        return list;
    }

    public static void ensureTableExists() throws SQLException {
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
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Tables created by earlier versions have no protocol column
            stmt.execute("ALTER TABLE sensor_data ADD COLUMN IF NOT EXISTS protocol VARCHAR(16)");
        }
    }
}
