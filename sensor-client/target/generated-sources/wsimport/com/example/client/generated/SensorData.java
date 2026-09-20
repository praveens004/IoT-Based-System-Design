
package com.example.client.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SensorData complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="SensorData">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="deviceId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="humidity" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         <element name="pressure" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         <element name="moisture" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SensorData", propOrder = {
    "deviceId",
    "humidity",
    "pressure",
    "moisture"
})
public class SensorData {

    @XmlElement(required = true)
    protected String deviceId;
    protected double humidity;
    protected double pressure;
    protected double moisture;

    /**
     * Gets the value of the deviceId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the value of the deviceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeviceId(String value) {
        this.deviceId = value;
    }

    /**
     * Gets the value of the humidity property.
     * 
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Sets the value of the humidity property.
     * 
     */
    public void setHumidity(double value) {
        this.humidity = value;
    }

    /**
     * Gets the value of the pressure property.
     * 
     */
    public double getPressure() {
        return pressure;
    }

    /**
     * Sets the value of the pressure property.
     * 
     */
    public void setPressure(double value) {
        this.pressure = value;
    }

    /**
     * Gets the value of the moisture property.
     * 
     */
    public double getMoisture() {
        return moisture;
    }

    /**
     * Sets the value of the moisture property.
     * 
     */
    public void setMoisture(double value) {
        this.moisture = value;
    }

}
