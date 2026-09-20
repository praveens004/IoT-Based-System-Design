package com.example.rest;

public class SensorData {
    private String deviceId;
    private double humidity;
    private double pressure;
    private double moisture;


    private String recordedAt;
    private String protocol;

    public SensorData() {}

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }

    public double getMoisture() { return moisture; }
    public void setMoisture(double moisture) { this.moisture = moisture; }

    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
}
