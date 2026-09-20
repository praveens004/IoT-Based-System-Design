package com.example.sensor;

import jakarta.xml.ws.Endpoint;
import org.h2.tools.Server;

public class SensorServicePublisher {

    public static void main(String[] args) {
        try {
            // Ensure the table exists before accepting requests
            DatabaseHelper.ensureTableExists();
            System.out.println("H2 table 'sensor_data' is ready.");
        } catch (Exception e) {
            System.err.println("WARNING: Could not open the H2 database or create the table.");
            System.err.println("Check that " + DatabaseHelper.getJdbcUrl() + " is writable.");
            e.printStackTrace();
        }

        try {
            Server.createWebServer("-webPort", "8083").start();
            System.out.println("H2 Console: http://localhost:8083  (JDBC URL: " + DatabaseHelper.getJdbcUrl() + ", user: sa, no password)");
        } catch (Exception e) {
            System.err.println("WARNING: Could not start H2 console – " + e.getMessage());
        }

        String address = "http://localhost:8080/sensor";
        SensorServiceImpl implementor = new SensorServiceImpl();

        Endpoint endpoint = Endpoint.publish(address, implementor);

        System.out.println("=================================================");
        System.out.println("  SOAP Sensor Service is running");
        System.out.println("  Endpoint : " + address);
        System.out.println("  WSDL     : " + address + "?wsdl");
        System.out.println("=================================================");
        System.out.println("Press Ctrl+C to stop the service.");
    }
}
