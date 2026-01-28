package bello.antonio.carrier_management_service.service;

import com.google.maps.model.LatLng;

import java.util.List;

public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double distanceToPolyline(LatLng p, List<LatLng> polyline) {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < polyline.size() - 1; i++) {
            double d = distancePointToSegment(
                    p,
                    polyline.get(i),
                    polyline.get(i + 1)
            );
            min = Math.min(min, d);
        }
        return min;
    }

    private static double distancePointToSegment(LatLng p, LatLng a, LatLng b) {

        double lat1 = Math.toRadians(a.lat);
        double lon1 = Math.toRadians(a.lng);
        double lat2 = Math.toRadians(b.lat);
        double lon2 = Math.toRadians(b.lng);
        double lat3 = Math.toRadians(p.lat);
        double lon3 = Math.toRadians(p.lng);

        double dx = lon2 - lon1;
        double dy = lat2 - lat1;

        if (dx == 0 && dy == 0) {
            return haversineKm(p, a);
        }

        double t = ((lon3 - lon1) * dx + (lat3 - lat1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projLon = lon1 + t * dx;
        double projLat = lat1 + t * dy;

        return haversineKm(
                p,
                new LatLng(Math.toDegrees(projLat), Math.toDegrees(projLon))
        );
    }

    public static double haversineKm(LatLng p1, LatLng p2) {
        double dLat = Math.toRadians(p2.lat - p1.lat);
        double dLon = Math.toRadians(p2.lng - p1.lng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(p1.lat))
                * Math.cos(Math.toRadians(p2.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
