package org.sunspotworld.firebase;

/**
 * @author Anson Cheung
 */
public class SpotDbZoneEntry
{
    private long timestamp;
    private double temp;
    private double light;

    public SpotDbZoneEntry() {}

    public SpotDbZoneEntry(double temp, double light, long timestamp){
        this.temp = temp;
        this.light = light;
        this.timestamp = timestamp;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public double getTemp(){
        return temp;
    }

    public double getLight(){
        return light;
    }
}