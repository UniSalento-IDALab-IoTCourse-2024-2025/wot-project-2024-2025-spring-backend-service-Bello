package bello.antonio.carrier_management_service.dto;

public class RouteInfoDTO {
    private String polyline;
    private double distanceKm;

    public RouteInfoDTO(String polyline, double distanceKm) {
        this.polyline = polyline;
        this.distanceKm = distanceKm;
    }

    public String getPolyline() { return polyline; }
    public double getDistanceKm() { return distanceKm; }
}

