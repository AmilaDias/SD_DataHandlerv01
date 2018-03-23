package sd2018.sd_datahandlerv01;

/**
 * Created by Amila Dias on 3/22/2018.
 */

public class DroneTelemetryData {

    private double altitude;
    private double airSpeed;
    private double currLatitude;
    private double currLongitude;
    private int batteryPercentage;

    public DroneTelemetryData() {
        setAltitude(0.0);
        setAirSpeed(0.0, 0.0);
        setCurrLatitude(0.0);
        setCurrLongitude(0.0);
        setBatteryPercentage(0);
    }


    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAirSpeed() {
        return airSpeed;
    }

    public void setAirSpeed(double speedX, double speedY) {

        if(Math.abs(speedX) > Math.abs(speedY)){
            this.airSpeed = speedX;
        } else {
            this.airSpeed = speedY;
        }
    }

    public double getCurrLatitude() {
        return currLatitude;
    }

    public void setCurrLatitude(double currLatitude) {
        this.currLatitude = currLatitude;
    }

    public double getCurrLongitude() {
        return currLongitude;
    }

    public void setCurrLongitude(double currLongitude) {
        this.currLongitude = currLongitude;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(int batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }
}
