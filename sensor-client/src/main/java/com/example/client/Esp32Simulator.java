package com.example.client;

import com.example.client.generated.SensorData;
import com.example.client.generated.SensorService;
import com.example.client.generated.SensorServicePortType;


public class Esp32Simulator {

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================");
        System.out.println("  ESP32 Simulator (SOAP Client)");
        System.out.println("==============================================");

        // 1. Create the Service object from the generated class.
        //    This class was produced by wsimport from the WSDL.
        SensorService service = new SensorService();

        // 2. Obtain the port (this is the CLIENT STUB / PROXY).
        //    SensorServicePortType is the interface that mirrors the operations
        //    defined in the WSDL portType.
        SensorServicePortType port = service.getSensorServicePort();

        System.out.println("Client stub obtained: " + port.getClass().getName());
        System.out.println("Sending sensor readings to the SOAP service...\n");

        // Simulate a few readings
        String[] deviceIds = {"ESP32-01", "ESP32-01", "ESP32-02"};
        double[] humidities = {65.4, 66.1, 58.2};
        double[] pressures  = {1012.8, 1013.1, 1009.5};
        double[] moistures  = {42.1, 41.8, 55.0};

        for (int i = 0; i < deviceIds.length; i++) {
            SensorData data = new SensorData();
            data.setDeviceId(deviceIds[i]);
            data.setHumidity(humidities[i]);
            data.setPressure(pressures[i]);
            data.setMoisture(moistures[i]);

            System.out.println("--- Sending reading " + (i + 1) + " ---");
            System.out.println("  deviceId  = " + data.getDeviceId());
            System.out.println("  humidity  = " + data.getHumidity());
            System.out.println("  pressure  = " + data.getPressure());
            System.out.println("  moisture  = " + data.getMoisture());

            // THIS is the call that uses the generated STUB.
            // Internally the stub:
            //   1. Encodes the SensorData Java object into XML
            //   2. Wraps it in a SOAP envelope
            //   3. Sends an HTTP POST to http://localhost:8080/sensor
            //   4. Receives the SOAP response
            //   5. Decodes the result back into a Java String
            String result = port.sendSensorData(data);

            System.out.println("  Service replied: " + result);
            System.out.println();
            Thread.sleep(800); 
        }

        System.out.println("All readings sent. Check the H2 table sensor_data (H2 console or the dashboard).");
    }
}
