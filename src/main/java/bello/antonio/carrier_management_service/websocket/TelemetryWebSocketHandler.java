package bello.antonio.carrier_management_service.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mappa sessione -> idTrip (solo sessioni che hanno fatto handshake)
    private final ConcurrentHashMap<String, String> sessionTripMap = new ConcurrentHashMap<>();

    // Mappa sessionId -> WebSocketSession (per poter inviare)
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Non facciamo nulla: aspettiamo l'handshake con InfoSimulationDTO
        System.out.println("🔌 WebSocket connesso: " + session.getId() + " — in attesa di handshake...");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tripId = sessionTripMap.remove(session.getId());
        activeSessions.remove(session.getId());
        System.out.println("❌ WebSocket disconnesso: " + session.getId() + " (tripId: " + tripId + ")");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Primo messaggio = handshake con InfoSimulationDTO
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());

            if (json.has("idTrip") && json.has("vehicleName")) {
                String idTrip = json.get("idTrip").asText();
                String vehicleName = json.get("vehicleName").asText();

                sessionTripMap.put(session.getId(), idTrip);
                activeSessions.put(session.getId(), session);

                System.out.println("✅ Handshake completato — sessionId: " + session.getId()
                        + " | vehicleName: " + vehicleName + " | idTrip: " + idTrip);

                // Conferma al frontend
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(
                                java.util.Map.of("type", "handshake_ok", "idTrip", idTrip, "vehicleName", vehicleName)
                        )
                ));
            } else {
                System.out.println("⚠️ Messaggio WebSocket non riconosciuto: " + message.getPayload());
            }

        } catch (Exception e) {
            System.err.println("❌ Errore handshake WebSocket: " + e.getMessage());
        }
    }

    /**
     * Invia dati solo alle sessioni che hanno registrato quell'idTrip.
     */
    public void sendTelemetry(String idTrip, String jsonData) {
        if (idTrip == null) return;

        for (var entry : sessionTripMap.entrySet()) {
            if (idTrip.equals(entry.getValue())) {
                WebSocketSession session = activeSessions.get(entry.getKey());
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonData));
                    } catch (IOException e) {
                        System.err.println("❌ Errore invio WebSocket: " + e.getMessage());
                    }
                }
            }
        }
    }

    public boolean hasActiveSession() {
        return !activeSessions.isEmpty();
    }
}