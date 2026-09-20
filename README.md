# From Sensors to Services

This project shows how a Service-Oriented Architecture (SOA) works using a small IoT example. A sensor device (an ESP32, simulated here in Java) measures humidity, air pressure and soil moisture and sends the readings to a service. The service stores them in a database, and other programs, such as a web dashboard, read them back through the same service. Nobody talks to the database directly except the service itself.

The same service is available in two ways. One is a SOAP web service, where the client is generated from a WSDL contract. The other is a REST service, where clients simply send JSON over HTTP. Both write to the same database, so you can compare the two styles side by side.

## What is in the project

The project is a Maven multi-module build. Each folder has a clear job:

- **sensor-service-contract** holds the WSDL file, `sensor-service.wsdl`. This is the contract for the SOAP service.
- **sensor-service-server** is the SOAP service. It publishes the service on port 8080 and saves readings to the database.
- **sensor-client** is a Java program that pretends to be an ESP32. It calls the SOAP service through a client stub that is generated from the WSDL at build time.
- **rest-service** is the REST service on port 8082, plus a small Java program that sends one reading over REST.
- **frontend** is the web dashboard (plain HTML, CSS and JavaScript). It talks to the REST service.
- **sql** contains old MySQL scripts from an earlier version. They are not used any more, because the project now uses H2.
- **h2lib** holds a copy of the H2 jar. You only need it if you want to start a standalone H2 console yourself, which is normally unnecessary.

## What need to install

You need Java latest version and Maven 3.8 or newer. You do not need to install a database, because the project uses H2, which runs inside the Java programs and keeps its data in a single file.

## Building the project

Open a terminal in the project folder (the one that contains the top-level `pom.xml`) and run:

```
mvn install -DskipTests
```

This compiles every module. It also runs a tool called `wsimport` inside `sensor-client`, which reads the WSDL and generates the SOAP client stub. The generated files end up in `sensor-client/target/generated-sources/wsimport/`. You will need to run this command again whenever you change the code, because a running service keeps using the old compiled classes until you stop it and start it again.

## Running the demo

Use a separate terminal for each program.

### 1. Start the REST service

```
cd rest-service
mvn exec:java
```

When it is ready you will see something like this:

```
H2 table ready.
H2 Console: http://localhost:8083 (JDBC URL: jdbc:h2:file:.../soa-iot-demo-data/soa_iot;AUTO_SERVER=TRUE, ...)
REST Sensor Service is running
```

The REST service is now listening on `http://localhost:8082/api/sensors`, and it has also opened the H2 web console on port 8083.

### 2. Start the SOAP service

In a second terminal:

```
cd sensor-service-server
mvn exec:java
```

You should see `SOAP Sensor Service is running`. If the REST service was already running, you will also see a warning that the H2 console could not start because port 8083 is in use. That is expected because the REST service already opened the console.

To see the contract that the service publishes, open this address in a browser:

```
http://127.0.0.1:8080/sensor?wsdl
```

### 3. Send readings through the SOAP client

In a third terminal:

```
cd sensor-client
mvn exec:java
```

The first line of output shows the client stub, for example `Client stub obtained: jdk.proxy3.$Proxy48`. After that the program sends three readings, and for each one the service replies with `OK - data stored for device ...`. The number after `$Proxy` can be different on your machine. What matters is that the name contains `$Proxy`, because that shows the stub is a class Java created while the program was running.

### 4. Optionally, send a reading through the REST Java client

With the REST service running:

```
cd rest-service
mvn exec:java```


## Using the dashboard

Open `frontend/index.html` in a browser by double-clicking it. The REST service must be running.

The left side of the page plays the role of the device. Click **Send Sensor Data** to send one reading, or tick **Auto-stream** to send a new, slightly changed reading every three seconds.

The right side plays the role of a user. Click **Fetch from service** to read the latest readings. If you leave the device field empty you get readings from all devices. You can also tick **Auto-fetch** to refresh every three seconds. Each row shows when the reading was recorded, the three values, and a small tag showing whether it arrived through REST or SOAP.



To see both interfaces sharing one database, run the SOAP client from step 3 and then click **Fetch from service** again. The rows sent by the Java client appear with a SOAP tag next to the REST rows.

The dashboard only uses REST. Section "SOAP or REST" below explains why a web page cannot easily call the SOAP endpoint.

### Looking at the data

Open `http://localhost:8083` in a browser. In the JDBC URL field, enter exactly the URL that the service printed when it started, which looks like `jdbc:h2:file:C:/Users/<you>/soa-iot-demo-data/soa_iot;AUTO_SERVER=TRUE`. Use `sa` as the user name and leave the password empty. Then you can run queries such as:

```sql
SELECT * FROM sensor_data ORDER BY recorded_at DESC;
SELECT protocol, COUNT(*) FROM sensor_data GROUP BY protocol;
```


## How the SOAP service works

### The contract

Everything starts with `sensor-service.wsdl`, in the `sensor-service-contract` module. WSDL stands for Web Services Description Language. It is an XML document that describes the service in a form that both people and tools can read. This one defines four things. First, the data: a `SensorData` type made of a `deviceId` string and three decimal numbers, `humidity`, `pressure` and `moisture`. Second, the operations the service offers, called `sendSensorData` and `getSensorData`. Third, the binding, which says messages are sent as SOAP over HTTP. And fourth, the address where the service listens, `http://localhost:8080/sensor`.

The project follows a contract-first approach. The WSDL file is the single source of truth. The server publishes this exact file at `?wsdl`, and the client stub is generated from this exact file, so the two sides cannot drift apart.

### The service code

The service itself is the class `SensorServiceImpl` in `sensor-service-server`. It is an ordinary Java class with a few annotations. An annotation is a label starting with `@` that a framework reads at runtime to decide how to treat your code. Here the framework is JAX-WS, running on the Eclipse Metro implementation, and its job is to turn the class into a SOAP service.

`@WebService` tells the framework to publish the class as a SOAP service. Its `name`, `serviceName`, `portName` and `targetNamespace` values must match the names in the WSDL. The `wsdlLocation` attribute tells it to serve the WSDL file from the contract module instead of generating one from the Java code. `@SOAPBinding` sets the message style, document and literal, which must match the WSDL binding.

On the method, `@WebMethod(operationName = "sendSensorData")` says this Java method is the WSDL operation of that name. `@WebParam(name = "data")` says the method parameter appears in the XML as an element called `data`, and `@WebResult(name = "result")` says the returned string appears as an element called `result`. Both use the contract's XML namespace, because the WSDL declares its elements as namespace-qualified.

The method body contains no XML and no HTTP code. The framework reads the incoming HTTP request, converts the XML into a `SensorData` object, calls the method, and converts the returned string back into XML for the reply. The only real work in the method is one call to `DatabaseHelper.saveSensorData(data)`, which runs an SQL INSERT.

A small file called `package-info.java` sets the XML namespace rules for the server's `SensorData` class so that they match the WSDL and the client.

The service is started by a single line in `SensorServicePublisher`:

```java
Endpoint.publish("http://localhost:8080/sensor", new SensorServiceImpl());
```

Inside the server, an incoming request passes through the JDK's built-in HTTP server, then Metro's `WSHttpHandler`, `HttpAdapter` and `WSEndpointImpl`, and finally `SEIInvokerTube`, which converts the XML into Java objects and calls `SensorServiceImpl.sendSensorData`.

### The client stub and how it is generated

A client stub is a local Java object that has the same methods as the remote service. When you call one of its methods, the stub does the network work. it turns your Java objects into a SOAP message, sends it, waits for the reply, and turns the reply back into a Java value. Your code never builds XML or opens an HTTP connection.

Nobody writes the stub by hand. It is generated from the WSDL every time you build. The `sensor-client/pom.xml` file configures the `jaxws-maven-plugin` to run its `wsimport` goal during the `generate-sources` phase. It reads `sensor-service.wsdl` from the contract module and writes Java source files into `sensor-client/target/generated-sources/wsimport/com/example/client/generated/`. A second plugin, `build-helper-maven-plugin`, adds that folder to the compile path so the generated code is compiled together with your own. Because the files live under `target`, they are regenerated on every build, and you should never edit them.

`wsimport` generates nine files:

- **SensorServicePortType.java** is the stub's interface. It has one Java method for each WSDL operation: `String sendSensorData(SensorData data)` and `List<SensorData> getSensorData(String deviceId)`.
- **SensorService.java** is the factory. It extends `jakarta.xml.ws.Service`, reads the WSDL when you create it, and has a method `getSensorServicePort()` that returns the stub.
- **SensorData.java** is the data class, with getters and setters. JAXB converts it to and from XML.
- **SendSensorData.java** and **SendSensorDataResponse.java** are the request and response wrappers for the `sendSensorData` operation.
- **GetSensorData.java** and **GetSensorDataResponse.java** are the request and response wrappers for `getSensorData`.
- **ObjectFactory.java** is a JAXB helper that creates the classes above.
- **package-info.java** holds the XML namespace settings.

`SensorService` is not the stub. It is only the factory. `SensorServicePortType` is the interface of the stub. The object you actually use at runtime is a dynamic proxy, a class that Java creates in memory while the program runs, using `java.lang.reflect.Proxy`. Its handler inside Metro is called `SEIStub`. That is why the client prints a class name such as `jdk.proxy3.$Proxy48`: there is no source file for it.

Here is how the client uses the stub, taken from `Esp32Simulator`:

```java
SensorService service = new SensorService();
SensorServicePortType port = service.getSensorServicePort();

SensorData data = new SensorData();
data.setDeviceId("ESP32-01");
data.setHumidity(65.4);
data.setPressure(1012.8);
data.setMoisture(42.1);

String result = port.sendSensorData(data);
```

The first line creates the generated factory, the second gets the stub from it, and the last line looks like a normal method call but is really a network call. When you make it, the stub converts the `SensorData` object to XML using JAXB, wraps the XML in a SOAP envelope, sends an HTTP POST to the address given in the WSDL, receives the SOAP response, converts the XML back into a Java string, and returns it.

In WSDL:

```
POST /sensor
Content-Type: text/xml

<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <sendSensorData xmlns="http://example.com/sensor">
      <data>
        <deviceId>ESP32-01</deviceId>
        <humidity>65.4</humidity>
        <pressure>1012.8</pressure>
        <moisture>42.1</moisture>
      </data>
    </sendSensorData>
  </S:Body>
</S:Envelope>
```

Two practical notes. JAX-WS and JAXB stopped being part of the JDK after Java 11, so the modules list `jakarta.xml.ws-api`, `jaxws-rt` (Eclipse Metro 4.0.2) and `jaxb-runtime` as dependencies. And the generated `SensorService` class stores the file path of the WSDL, so if you move the project folder, run `mvn install -DskipTests` again to regenerate it.

## How the REST service works

The REST service is the class `RestSensorServer` in `rest-service`. It uses the HTTP server that comes with the JDK (`com.sun.net.httpserver.HttpServer`) and the Gson library for JSON. There is no web framework and no generated client. Routing, status codes and JSON handling are all written by hand, which makes it short and easy to read.

When it starts, it creates the HTTP server on port 8082 and registers one handler, `SensorHandler`, for the path `/api/sensors`. Every request to that path goes through the handler, which looks at the HTTP method and the rest of the path.

A `POST` to `/api/sensors` saves one reading. The body is a JSON object like this:

```json
{"deviceId":"ESP32-01","humidity":65.4,"pressure":1012.8,"moisture":42.1}
```

The handler parses the JSON with Gson, checks that `deviceId` is present, and then calls `DatabaseHelper.save(data, "REST")`, which runs an INSERT. If the `deviceId` is missing it answers with status 400. On success it answers 201 Created with `{"status":"OK","deviceId":"ESP32-01"}`.

A `GET` to `/api/sensors` returns the latest 20 readings across all devices, and a `GET` to `/api/sensors/ESP32-01` returns the latest 20 for one device. Both run a SELECT ordered by `recorded_at`, newest first, and return a JSON array. Each reading looks like this:

```json
{"deviceId":"ESP32-01","humidity":65.4,"pressure":1012.8,"moisture":42.1,
 "recordedAt":"2026-09-19T21:59:35.267","protocol":"REST"}
```

## SOAP or REST

Both styles implement the same idea, so neither is better in general. They suit different consumers.

SOAP sends XML inside an envelope and comes with a formal, machine-readable contract, the WSDL. Because of that contract, tools can generate a typed client stub, and calling the service feels like calling a local method. It is common in enterprise systems that need strict contracts. 

REST sends plain HTTP requests, usually with JSON. There is no required contract file and no required generated client. Any language with an HTTP library can call it, and the messages are small, which is why it is popular for web pages, mobile apps and small devices.



