package bello.antonio.carrier_management_service.service;

import bello.antonio.carrier_management_service.domain.Notification;
import bello.antonio.carrier_management_service.domain.Telemetry;
import bello.antonio.carrier_management_service.dto.AnomalyMessageDTO;
import bello.antonio.carrier_management_service.dto.TelemetryMessageDTO;
import bello.antonio.carrier_management_service.repositories.NotificationRepository;
import bello.antonio.carrier_management_service.repositories.TelemetryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class MqttListenerService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttTelemetryMessage(Message<String> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload();
        String vehicleName = extractVehicleName(topic);

        try {
            TelemetryMessageDTO dto = mapper.readValue(payload, TelemetryMessageDTO.class);

            if ("completed".equals(dto.getStreamStatus())) {
                System.out.println("Stream completato per [" + vehicleName + "]");
                return;
            }

            Date timestamp = Date.from(
                    LocalDateTime.parse(dto.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            .toInstant(ZoneOffset.UTC)
            );

            Telemetry telemetry = new Telemetry();
            telemetry.setVehicleName(dto.getVehicleName());
            telemetry.setTimestamp(timestamp);
            telemetry.setRowIndex(dto.getRowIndex());
            telemetry.settAmb(dto.gettAmb());
            telemetry.settSet(dto.gettSet());
            telemetry.settCabMeas(dto.gettCabMeas());
            telemetry.settEvapSat(dto.gettEvapSat());
            telemetry.settCondSat(dto.gettCondSat());
            telemetry.setpSucBar(dto.getpSucBar());
            telemetry.setpDisBar(dto.getpDisBar());
            telemetry.setnCompHz(dto.getnCompHz());
            telemetry.setShK(dto.getShK());
            telemetry.setpCompW(dto.getpCompW());
            telemetry.setqEvapW(dto.getqEvapW());
            telemetry.setCop(dto.getCop());
            telemetry.setDoorOpen(dto.isDoorOpen());
            telemetry.setDefrostOn(dto.isDefrostOn());
            telemetry.setValveOpen(dto.isValveOpen());

            telemetryRepository.save(telemetry);
            System.out.println("Telemetria salvata [" + vehicleName + "] row=" + dto.getRowIndex());

        } catch (Exception e) {
            System.err.println("Errore parsing telemetry message: " + e.getMessage());
        }
    }


    @ServiceActivator(inputChannel = "mqttAnomalyChannel")
    public void handleMqttAnomalyMessage(Message<String> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload();
        String vehicleName = extractVehicleName(topic);

        try {
            ObjectMapper mapper = new ObjectMapper();
            AnomalyMessageDTO anomaly = mapper.readValue(payload, AnomalyMessageDTO.class);

            System.out.println("ANOMALY [" + vehicleName + "] row=" + anomaly.getRowIndex()
                    + " | error=" + String.format("%.6f", anomaly.getReconstructionError())
                    + " | detected=" + anomaly.isAnomalyDetected());

            if (!anomaly.isAnomalyDetected()) return;

            if (anomaly.getAlertMessage() != null) {
                // Trigger principale: messaggio di transizione false→true
                Notification notification = new Notification();
                notification.setVehicleName(vehicleName);
                notification.setTimestamp(anomaly.getTimestamp());
                notification.setReconstructionError(anomaly.getReconstructionError());
                notification.setRowIndex(anomaly.getRowIndex());
                notification.setAlertMessage(anomaly.getAlertMessage());
                notification.setRead(false);
                notificationRepository.save(notification);
                System.out.println("Nuova notifica salvata per [" + vehicleName + "]");

            } else if (!notificationRepository.existsByVehicleNameAndReadFalse(vehicleName)) {
                // Trigger secondario: anomalyDetected=true ma messaggio di transizione perso
                Notification notification = new Notification();
                notification.setVehicleName(vehicleName);
                notification.setTimestamp(anomaly.getTimestamp());
                notification.setReconstructionError(anomaly.getReconstructionError());
                notification.setRowIndex(anomaly.getRowIndex());
                notification.setAlertMessage("⚠️ Anomalia rilevata per " + vehicleName);
                notification.setRead(false);
                notificationRepository.save(notification);
                System.out.println("Notifica salvata (fallback) per [" + vehicleName + "]");
            }

        } catch (Exception e) {
            System.err.println("Errore parsing anomaly message: " + e.getMessage());
        }
    }

    private String extractVehicleName(String topic) {
        if (topic != null && topic.startsWith("fridge/")) {
            String[] parts = topic.split("/");
            if (parts.length >= 2) return parts[1];
        }
        return "unknown";
    }
}