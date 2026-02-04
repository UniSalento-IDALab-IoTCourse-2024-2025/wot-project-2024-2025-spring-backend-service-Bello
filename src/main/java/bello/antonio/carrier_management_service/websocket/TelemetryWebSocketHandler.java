package bello.antonio.carrier_management_service.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    // Sessione attiva (una sola alla volta)
    private WebSocketSession activeSession = null;
    
    // Vehicle attualmente monitorato
    private String activeVehicle = null;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✅ WebSocket connesso: " + session.getId());
        // Attiva subito la sessione (senza aspettare lo start)
        this.activeSession = session;
        System.out.println("📍 Sessione WebSocket ATTIVATA");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("❌ WebSocket disconnesso: " + session.getId());
        if (activeSession != null && activeSession.getId().equals(session.getId())) {
            activeSession = null;
            activeVehicle = null;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Il client può mandare messaggi se necessario (es. ping)
        System.out.println("📩 Messaggio ricevuto: " + message.getPayload());
    }

    /**
     * Attiva la sessione per ricevere dati di un veicolo.
     */
    public void startSession(WebSocketSession session, String vehicleName) {
        this.activeSession = session;
        this.activeVehicle = vehicleName;
        System.out.println("🚀 Sessione attivata per: " + vehicleName);
    }

    /**
     * Disattiva la sessione corrente.
     */
    public void stopSession() {
        if (activeSession != null && activeSession.isOpen()) {
            try {
                activeSession.close();
            } catch (IOException e) {
                System.err.println("Errore chiusura WebSocket: " + e.getMessage());
            }
        }
        activeSession = null;
        activeVehicle = null;
        System.out.println("⏹️ Sessione fermata");
    }

    /**
     * Invia dati al client se la sessione è attiva e il veicolo corrisponde.
     */
    public void sendTelemetry(String vehicleName, String jsonData) {
        System.out.println("ActiveSession= " + activeSession + "\njsonData= " + jsonData);
        if (activeSession != null && activeSession.isOpen()) {
            try {
                System.out.println("Sto inviano il messaggio al frontend");
                activeSession.sendMessage(new TextMessage(jsonData));
            } catch (IOException e) {
                System.err.println("Errore invio WebSocket: " + e.getMessage());
            }

        }
    }

    /**
     * Verifica se c'è una sessione attiva.
     */
    public boolean hasActiveSession() {
        return activeSession != null && activeSession.isOpen();
    }

    /**
     * Restituisce il veicolo attualmente monitorato.
     */
    public String getActiveVehicle() {
        return activeVehicle;
    }
}
