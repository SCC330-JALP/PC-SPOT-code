package org.sunspotworld;

import com.sun.spot.peripheral.ota.OTACommandServer;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
* @author Povilas Marcinkevicius
* @version 1.2.1
**/
public class Week4PC implements Runnable
{
  private static final Storage storage = new Storage();                                           // Could be replaced by firebase?
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // For printing; unused in final release
  private static final SimpleDateFormat hourFormat = new SimpleDateFormat("HH");                  // for determining hour change
  private static String currentHour = hourFormat.format(System.currentTimeMillis());              // current hour saved

  public static final String COMMAND_IDLE = "idle";
  public static final String COMMAND_ZONE_DATA = "zone";
  public static final char COMMAND_SENSOR_EVENT = 's';
  public static final char SENSOR_LIGHT = 'l';
  public static final char SENSOR_MOTION = 'm';
  public static final char SENSOR_TEMPERATURE = 't';
  
  private static final int PORT_COMMAND_RELAY = 37;
  
  // INIT start
  public static void main(String[] args)
  {
    OTACommandServer.start("SwitchMonitorApplication"); // Base I think?
    Week4PC switchMonitorApp = new Week4PC();           // Create object so the program does not end saying "no objects"?...
  }
  
  // Just starts the thread
  public Week4PC()
  {
    try
    {
      Thread pollingThread = new Thread(this, "Constructor Thread");
      pollingThread.setDaemon(true);
      pollingThread.start();
    }
    catch(Exception e)
    {
      System.err.println("Unable to initialise polling");
      e.printStackTrace();
    }
  }
  // INIT end
  
  public void run()
  {
    System.out.println("Using Base Station " + System.getProperty("IEEE_ADDRESS"));
    ConnectionProtocolPC.startConnectionResponseServer(Integer.parseInt(storage.getOrCreateEntry("ZONE")), false);
  }
  
  // Used by ConnectionProtocolPC.startConnectionResponseServer()
  // onSpotBoot: means this is the first on and spot needs instructions
  // command: idle if onSpotBoot and whatever it is otherwise
  public static void handleStream(final DataInputStream inputStream, String address, String command) throws IOException
  {
    final String firebaseSpotAddress = address.replace(".", " ");
    System.out.println("Address for firebase: " + firebaseSpotAddress);
    boolean firstCall = command.isEmpty();
    if(firstCall)
    {
      // TODO: get SPOT data with Liams' code to get value from firebase
      if(false /* retireved data is null i.e. no such SPOT in firebase */)
      {
        System.out.println("SPOT was not in firebase");
        command = COMMAND_IDLE;
        // Add the SPOT to the firebase using Liams' code
      }
      else
      {
        System.out.println("SPOT was in firebase, got command from there");
        command = COMMAND_ZONE_DATA; // TODO extract command string from the SPOT data gotten from firebase   command = spotData.getTask();
      }
      
      ConnectionPC commandConn = new ConnectionPC(address, PORT_COMMAND_RELAY, 10);
      commandConn.getNewRadiogram().writeUTF(command);
      try { Thread.sleep(3000); } catch (InterruptedException e) {}
        inputStream.close();
      System.out.println("Sending startup command " + command);
      commandConn.send();
      commandConn.close();
    }
    
    final String finalCommand = command;
    Thread listenerThread = new Thread(new Runnable()
    {
      public void run()
      {
        if(finalCommand.equals(COMMAND_ZONE_DATA))
          handleZoneDataStream(inputStream);
        else if(finalCommand.charAt(0) == COMMAND_SENSOR_EVENT)
          handleSensorTriggerStream(inputStream, firebaseSpotAddress);
      }
    }, "Stream Data Listener");
    listenerThread.start();
    
    // Execute the following code when the data on firebase changes. Use Liams' code
    /*
    if(!firstCall)
    {
      try
      {
        inputStream.close();
        ConnectionPC commandConn = new ConnectionPC(address.replace(" ", "."), PORT_COMMAND_RELAY, 10);
        commandConn.getNewRadiogram().writeUTF(COMMAND_ZONE_DATA);
        System.out.println("Sending changed command " + COMMAND_ZONE_DATA);
        commandConn.send();
        commandConn.close();
      }
      catch(IOException e)
      { e.printStackTrace(); }
    }
    */
  }
  
  
  
  // Used when the SPOT is transmitting zone data
  private static void handleZoneDataStream(DataInputStream inputStream)
  {
    SPOTDBcommunication db = new SPOTDBcommunication("zone" + storage.getOrCreateEntry("ZONE"));
    SPOTDBcommunication dbHourly = new SPOTDBcommunication("zone" + storage.getOrCreateEntry("ZONE") + "hourly");
    
    while(true)
    {
      try
      {
        double light = inputStream.readDouble();
        double temp = inputStream.readDouble();
        long timeStamp = inputStream.readLong();

        db.updateZoneData(Integer.parseInt(storage.getOrCreateEntry("ZONE")), temp, light, timeStamp);

        String currentNewHour = hourFormat.format(System.currentTimeMillis());
        if(!currentNewHour.equals(currentHour))
        {
          currentHour = currentNewHour;
          dbHourly.updateZoneData(Integer.parseInt(storage.getOrCreateEntry("ZONE")), temp, light, timeStamp); 
        }
      }
      catch(InterruptedException e)
      { e.printStackTrace(); }
      catch(IOException e)
      {
        System.out.println("Reading aborted");
        return;
      }
    }
  }
  
  // Used when the SPOT is transmitting sensor detections
  private static void handleSensorTriggerStream(DataInputStream inputStream, String spotAddress)
  {
    // Open DB connection TODO
    while(true)
    {
      try
      {
        long timeStamp = inputStream.readLong();
        // push reading to firebase using Liams' code
        System.out.println("Sensor event at " + dateFormat.format(timeStamp));
      }
      catch(IOException e)
      {
        System.out.println("Reading aborted");
        return;
      }
    }
  }
}