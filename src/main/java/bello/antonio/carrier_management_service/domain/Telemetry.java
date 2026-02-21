package bello.antonio.carrier_management_service.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("telemetry")
public class Telemetry {
    @Id
    private String id;
    private String vehicleName;

    @Indexed(expireAfter = "P2D") // P2D = 2 giorni in formato ISO 8601
    private Date timestamp;

    private int rowIndex;
    private double tAmb;
    private double tSet;
    private double tCabMeas;
    private double tEvapSat;
    private double tCondSat;
    private double pSucBar;
    private double pDisBar;
    private double nCompHz;
    private double shK;
    private double pCompW;
    private double qEvapW;
    private double cop;
    private int doorOpen;
    private int defrostOn;
    private int valveOpen;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public double gettAmb() {
        return tAmb;
    }

    public void settAmb(double tAmb) {
        this.tAmb = tAmb;
    }

    public double gettSet() {
        return tSet;
    }

    public void settSet(double tSet) {
        this.tSet = tSet;
    }

    public double gettCabMeas() {
        return tCabMeas;
    }

    public void settCabMeas(double tCabMeas) {
        this.tCabMeas = tCabMeas;
    }

    public double gettEvapSat() {
        return tEvapSat;
    }

    public void settEvapSat(double tEvapSat) {
        this.tEvapSat = tEvapSat;
    }

    public double gettCondSat() {
        return tCondSat;
    }

    public void settCondSat(double tCondSat) {
        this.tCondSat = tCondSat;
    }

    public double getpSucBar() {
        return pSucBar;
    }

    public void setpSucBar(double pSucBar) {
        this.pSucBar = pSucBar;
    }

    public double getpDisBar() {
        return pDisBar;
    }

    public void setpDisBar(double pDisBar) {
        this.pDisBar = pDisBar;
    }

    public double getnCompHz() {
        return nCompHz;
    }

    public void setnCompHz(double nCompHz) {
        this.nCompHz = nCompHz;
    }

    public double getShK() {
        return shK;
    }

    public void setShK(double shK) {
        this.shK = shK;
    }

    public double getpCompW() {
        return pCompW;
    }

    public void setpCompW(double pCompW) {
        this.pCompW = pCompW;
    }

    public double getqEvapW() {
        return qEvapW;
    }

    public void setqEvapW(double qEvapW) {
        this.qEvapW = qEvapW;
    }

    public double getCop() {
        return cop;
    }

    public void setCop(double cop) {
        this.cop = cop;
    }

    public int getDoorOpen() {
        return doorOpen;
    }

    public void setDoorOpen(int doorOpen) {
        this.doorOpen = doorOpen;
    }

    public int getDefrostOn() {
        return defrostOn;
    }

    public void setDefrostOn(int defrostOn) {
        this.defrostOn = defrostOn;
    }

    public int getValveOpen() {
        return valveOpen;
    }

    public void setValveOpen(int valveOpen) {
        this.valveOpen = valveOpen;
    }
}
