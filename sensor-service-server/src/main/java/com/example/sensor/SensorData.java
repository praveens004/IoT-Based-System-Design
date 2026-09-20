package com.example.sensor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Matches the SensorData complex type in the WSDL.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SensorData", propOrder = {
        "deviceId",
        "humidity",
        "pressure",
        "moisture"
})
public class SensorData {

    private String deviceId;
    private double humidity;
    private double pressure;
    private double moisture;

    public SensorData() {
    }

    public SensorData(String deviceId, double humidity, double pressure, double moisture) {
        this.deviceId = deviceId;
        this.humidity = humidity;
        this.pressure = pressure;
        this.moisture = moisture;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public double getMoisture() {
        return moisture;
    }

    public void setMoisture(double moisture) {
        this.moisture = moisture;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "deviceId='" + deviceId + '\'' +
                ", humidity=" + humidity +
                ", pressure=" + pressure +
                ", moisture=" + moisture +
                '}';
    }
}
