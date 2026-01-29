package bello.antonio.carrier_management_service.service;

import com.google.maps.model.LatLng;

import java.util.*;

public class PolylineUtils {

    public static List<LatLng> decode(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return poly;
    }

    /**
     * Rappresenta una coppia di waypoints (departure -> arrival) per uno shipment.
     * Il vincolo è che departure DEVE essere visitato PRIMA di arrival.
     */
    public static class ShipmentWaypoints {
        public final LatLng departure;
        public final LatLng arrival;
        public final String label; // opzionale, per debug

        public ShipmentWaypoints(LatLng departure, LatLng arrival) {
            this(departure, arrival, null);
        }

        public ShipmentWaypoints(LatLng departure, LatLng arrival, String label) {
            this.departure = departure;
            this.arrival = arrival;
            this.label = label;
        }
    }

    /**
     * Ordina i waypoints di più shipment in modo ottimale, rispettando il vincolo
     * che per ogni shipment il departure deve essere visitato prima dell'arrival.
     *
     * Algoritmo greedy:
     * 1. Partendo dalla posizione corrente (tripStart), trova il waypoint più vicino
     *    tra quelli "disponibili"
     * 2. Un departure è sempre disponibile se non è stato ancora visitato
     * 3. Un arrival è disponibile solo se il suo departure corrispondente è già stato visitato
     * 4. Ripeti finché tutti i waypoints sono stati ordinati
     *
     * @param tripStart Punto di partenza del trip
     * @param shipments Lista di coppie (departure, arrival) per ogni shipment
     * @return Lista ordinata di tutti i waypoints
     */
    public static List<LatLng> orderWaypointsWithConstraints(LatLng tripStart, List<ShipmentWaypoints> shipments) {
        if (shipments == null || shipments.isEmpty()) {
            return new ArrayList<>();
        }

        List<LatLng> orderedWaypoints = new ArrayList<>();
        LatLng currentPosition = tripStart;

        // Traccia quali departure e arrival sono stati visitati
        // Usiamo l'indice dello shipment come chiave
        Set<Integer> departureVisited = new HashSet<>();
        Set<Integer> arrivalVisited = new HashSet<>();

        int totalWaypoints = shipments.size() * 2; // ogni shipment ha 2 waypoints

        while (orderedWaypoints.size() < totalWaypoints) {
            double minDistance = Double.MAX_VALUE;
            LatLng nextWaypoint = null;
            int selectedShipmentIndex = -1;
            boolean isDeparture = false;

            // Trova il waypoint disponibile più vicino
            for (int i = 0; i < shipments.size(); i++) {
                ShipmentWaypoints sw = shipments.get(i);

                // Controlla se il departure di questo shipment è disponibile
                if (!departureVisited.contains(i)) {
                    double dist = GeoUtils.haversineKm(currentPosition, sw.departure);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nextWaypoint = sw.departure;
                        selectedShipmentIndex = i;
                        isDeparture = true;
                    }
                }

                // Controlla se l'arrival di questo shipment è disponibile
                // (solo se il departure corrispondente è già stato visitato)
                if (departureVisited.contains(i) && !arrivalVisited.contains(i)) {
                    double dist = GeoUtils.haversineKm(currentPosition, sw.arrival);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nextWaypoint = sw.arrival;
                        selectedShipmentIndex = i;
                        isDeparture = false;
                    }
                }
            }

            if (nextWaypoint == null) {
                // Non dovrebbe mai succedere se l'input è corretto
                System.err.println("⚠️ orderWaypointsWithConstraints: nessun waypoint disponibile trovato!");
                break;
            }

            // Aggiungi il waypoint selezionato
            orderedWaypoints.add(nextWaypoint);
            currentPosition = nextWaypoint;

            // Aggiorna lo stato
            if (isDeparture) {
                departureVisited.add(selectedShipmentIndex);
                if (shipments.get(selectedShipmentIndex).label != null) {
                    System.out.println("      📍 Aggiunto PICKUP: " + shipments.get(selectedShipmentIndex).label);
                }
            } else {
                arrivalVisited.add(selectedShipmentIndex);
                if (shipments.get(selectedShipmentIndex).label != null) {
                    System.out.println("      📍 Aggiunto DELIVERY: " + shipments.get(selectedShipmentIndex).label);
                }
            }
        }

        return orderedWaypoints;
    }

    /**
     * Versione semplificata che accetta direttamente liste di LatLng.
     * Assume che le liste departure e arrival siano della stessa lunghezza
     * e che departure[i] corrisponda ad arrival[i].
     *
     * @param tripStart Punto di partenza del trip
     * @param departures Lista dei punti di partenza degli shipment
     * @param arrivals Lista dei punti di arrivo degli shipment
     * @return Lista ordinata di tutti i waypoints
     */
    public static List<LatLng> orderWaypointsWithConstraints(
            LatLng tripStart,
            List<LatLng> departures,
            List<LatLng> arrivals) {

        if (departures.size() != arrivals.size()) {
            throw new IllegalArgumentException(
                    "Le liste departures e arrivals devono avere la stessa lunghezza");
        }

        List<ShipmentWaypoints> shipments = new ArrayList<>();
        for (int i = 0; i < departures.size(); i++) {
            shipments.add(new ShipmentWaypoints(departures.get(i), arrivals.get(i)));
        }

        return orderWaypointsWithConstraints(tripStart, shipments);
    }

    /**
     * Versione con labels per debug.
     *
     * @param tripStart Punto di partenza del trip
     * @param departures Lista dei punti di partenza degli shipment
     * @param arrivals Lista dei punti di arrivo degli shipment
     * @param labels Lista di etichette per debug (può essere null)
     * @return Lista ordinata di tutti i waypoints
     */
    public static List<LatLng> orderWaypointsWithConstraints(
            LatLng tripStart,
            List<LatLng> departures,
            List<LatLng> arrivals,
            List<String> labels) {

        if (departures.size() != arrivals.size()) {
            throw new IllegalArgumentException(
                    "Le liste departures e arrivals devono avere la stessa lunghezza");
        }

        List<ShipmentWaypoints> shipments = new ArrayList<>();
        for (int i = 0; i < departures.size(); i++) {
            String label = (labels != null && i < labels.size()) ? labels.get(i) : null;
            shipments.add(new ShipmentWaypoints(departures.get(i), arrivals.get(i), label));
        }

        return orderWaypointsWithConstraints(tripStart, shipments);
    }
}
