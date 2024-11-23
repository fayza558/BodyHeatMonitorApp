#include <Wire.h>
#include <Adafruit_MLX90614.h>
#include <WiFi.h>
#include <WebServer.h>

const char* ssid = "Abdullah";
const char* password = "Abdullah&2005";

Adafruit_MLX90614 mlx = Adafruit_MLX90614();
WebServer server(80);

void setup() {
    Serial.begin(115200);

    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.print(".");
    }
    Serial.println("\nConnected to WiFi");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());

    if (!mlx.begin()) {
        Serial.println("Error initializing sensor!");
        while (1);
    }

    server.on("/", handleRoot);
    server.begin();
    Serial.println("HTTP server started");
}

void loop() {
    server.handleClient();
}

void handleRoot() {
    float ambientTemp = mlx.readAmbientTempC();
    float objectTemp = mlx.readObjectTempC();

    String html = "<html><body>";
    html += "<h1>Temperature Readings</h1>";
    html += "<p>Ambient Temperature: " + String(ambientTemp) + " °C</p>";
    html += "<p>Object Temperature: " + String(objectTemp) + " °C</p>";
    html += "</body></html>";

    server.send(200, "text/html", html);
}