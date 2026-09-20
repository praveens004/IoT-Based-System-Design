
package com.example.client.generated;

import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.example.client.generated package. 
 * <p>An ObjectFactory allows you to programmatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.example.client.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SendSensorData }
     * 
     * @return
     *     the new instance of {@link SendSensorData }
     */
    public SendSensorData createSendSensorData() {
        return new SendSensorData();
    }

    /**
     * Create an instance of {@link SensorData }
     * 
     * @return
     *     the new instance of {@link SensorData }
     */
    public SensorData createSensorData() {
        return new SensorData();
    }

    /**
     * Create an instance of {@link SendSensorDataResponse }
     * 
     * @return
     *     the new instance of {@link SendSensorDataResponse }
     */
    public SendSensorDataResponse createSendSensorDataResponse() {
        return new SendSensorDataResponse();
    }

    /**
     * Create an instance of {@link GetSensorData }
     * 
     * @return
     *     the new instance of {@link GetSensorData }
     */
    public GetSensorData createGetSensorData() {
        return new GetSensorData();
    }

    /**
     * Create an instance of {@link GetSensorDataResponse }
     * 
     * @return
     *     the new instance of {@link GetSensorDataResponse }
     */
    public GetSensorDataResponse createGetSensorDataResponse() {
        return new GetSensorDataResponse();
    }

}
