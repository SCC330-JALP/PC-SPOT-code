package org.sunspotworld;

public class SPOTDBentry{

    private long timestamp;
    private double temp;
    private double light;
    private int zoneId;

    public SPOTDBentry() {}

    public SPOTDBentry(int zoneId, double temp, double light, long timestamp){
        this.zoneId = zoneId;
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

    public int getZoneId(){
        return zoneId;
    }
}