package bello.antonio.carrier_management_service.dto;

public class SelectedTripDTO {
    private TripDTO trip;
    private ShipmentDTO shipment;

    public TripDTO getTrip() { return trip; }
    public void setTrip(TripDTO trip) { this.trip = trip; }

    public ShipmentDTO getShipment() { return shipment; }
    public void setShipment(ShipmentDTO shipment) { this.shipment = shipment; }
}
