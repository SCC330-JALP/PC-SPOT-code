/**
* @author Povilas Marcinkevicius
**/

package org.sunspotworld;

import com.sun.spot.peripheral.ota.OTACommandServer;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class Week3PC implements Runnable
{ 
  private static Thread pollingThread = null;
  private static final Storage storage = new Storage();
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
  private static final SimpleDateFormat hourFormat = new SimpleDateFormat("hh");
  private static final SPOTDBcommunication db = new SPOTDBcommunication("zone" + storage.getOrCreateEntry("ZONE"));
  private static final SPOTDBcommunication dbHourly = new SPOTDBcommunication("zone" + storage.getOrCreateEntry("ZONE") + "hourly");

  private static String currentHour = hourFormat.format(System.currentTimeMillis());
  
  public Week3PC()
  {
    try
    {
      pollingThread = new Thread(this, "pollingService");
      pollingThread.setDaemon(true);
      pollingThread.start();
    }
    catch(Exception e)
    {
      System.err.println("Unable to initialise polling");
      e.printStackTrace();
    }
  }
  
  public void run()
  {
    ConnectionPC.startConnectionResponseServer(Integer.parseInt(storage.getOrCreateEntry("ZONE")), false);
  }
  
  public static void printStreamData(DataInputStream inputStream) throws IOException
  {
    double light = inputStream.readDouble();
    double temp = inputStream.readDouble();
    long timeStamp = inputStream.readLong();
    
    System.out.println(String.format("Light (lx): %.2f\nTemp (Celcius): %.2f\nTime: %s",
        light, temp, dateFormat.format(timeStamp)));

    try
    {
      db.update(Integer.parseInt(storage.getOrCreateEntry("ZONE")), temp, light, timeStamp);
    
      String currentNewHour = hourFormat.format(System.currentTimeMillis());
      if(!currentNewHour.equals(currentHour))
      {
        currentHour = currentNewHour;
        dbHourly.update(Integer.parseInt(storage.getOrCreateEntry("ZONE")), temp, light, timeStamp); 
      }
    }
    catch(InterruptedException e)
    { e.printStackTrace(); }
  }
  
  public static void main(String[] args)
  {
    OTACommandServer.start("SwitchMonitorApplication");
    Week3PC switchMonitorApp = new Week3PC();
  }
}
