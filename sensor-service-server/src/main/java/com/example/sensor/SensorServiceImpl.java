package com.example.sensor;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import java.sql.SQLException;
import java.util.List;

/**
 * SOAP Web Service implementation (Service Provider).
 *
 * This class is annotated with @WebService so that JAX-WS / Metro
 * can publish it as a SOAP endpoint.
 *
 * Contract-first: the WSDL published at ?wsdl is the one from the sensor-service-contract
 * module (wsdlLocation below), the same file the client stubs are generated from.
 * This class must match that contract, not define it.
 */
@WebService(
        name = "SensorServicePortType",
        serviceName = "SensorService",
        targetNamespace = "http://example.com/sensor",
        portName = "SensorServicePort",
        wsdlLocation = "sensor-service.wsdl"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL,
             parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public class SensorServiceImpl {

    
    private static final String NS = "http://example.com/sensor";

    @WebMethod(operationName = "sendSensorData")
    @WebResult(name = "result", targetNamespace = NS)
    public String sendSensorData(
            @WebParam(name = "data", targetNamespace = NS) SensorData data) {
        System.out.println("=== SOAP Request received: sendSensorData ===");
        System.out.println("Device ID : " + data.getDeviceId());
        System.out.println("Humidity  : " + data.getHumidity());
        System.out.println("Pressure  : " + data.getPressure());
        System.out.println("Moisture  : " + data.getMoisture());

        try {
            DatabaseHelper.saveSensorData(data);
            String msg = "OK - data stored for device " + data.getDeviceId();
            System.out.println("=== Stored in H2 database successfully ===");
            return msg;
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    @WebMethod(operationName = "getSensorData")
    @WebResult(name = "data", targetNamespace = NS)
    public List<SensorData> getSensorData(
            @WebParam(name = "deviceId", targetNamespace = NS) String deviceId) {
        System.out.println("=== SOAP Request received: getSensorData for " + deviceId + " ===");
        try {
            return DatabaseHelper.getSensorDataByDevice(deviceId);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
