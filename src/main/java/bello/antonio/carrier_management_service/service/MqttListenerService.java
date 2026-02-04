package bello.antonio.carrier_management_service.service;

import bello.antonio.carrier_management_service.websocket.TelemetryWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class MqttListenerService {

    @Autowired
    private TelemetryWebSocketHandler webSocketHandler;

    /**
     * Riceve messaggi MQTT e li inoltra al WebSocket.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload();

        // Estrai vehicleName dal topic: fridge/{vehicleName}/telemetry
        String vehicleName = extractVehicleName(topic);

        System.out.println("📡 MQTT ricevuto [" + vehicleName + "]: " + payload.substring(0, Math.min(50, payload.length())) + "...");

        // ✅ NUOVO: Verifica se è un messaggio di completamento
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            if (jsonNode.has("stream_status")) {
                String status = jsonNode.get("stream_status").asText();

                if ("completed".equals(status)) {
                    System.out.println("🏁 Stream completato per " + vehicleName);

                    // Aggiungi flag al messaggio
                    ObjectNode modifiedNode = (ObjectNode) jsonNode;
                    modifiedNode.put("is_stream_end", true);
                    payload = mapper.writeValueAsString(modifiedNode);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore parsing stream status: " + e.getMessage());
        }
        // Inoltra al WebSocket
        webSocketHandler.sendTelemetry(vehicleName, payload);
    }

    private String extractVehicleName(String topic) {
        // topic = "fridge/camion1/telemetry"
        if (topic != null && topic.startsWith("fridge/")) {
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "unknown";
    }
}
