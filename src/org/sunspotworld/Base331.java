package org.sunspotworld;

import com.sun.spot.peripheral.ota.OTACommandServer;
import org.sunspotworld.firebase.*;

/**
* @author Povilas Marcinkevicius
* @version 2.0.0
* 
* Serves to get all services running
* PSP LOC: 9
**/
public class Base331 implements Runnable
{
  public static String BASE_MAC;
  
  public static void main(String[] args)
  {
    OTACommandServer.start("SwitchMonitorApplication");
    Base331 switchMonitorApp = new Base331();
  }
  
  public Base331()
  {
    try
    {
      Thread pollingThread = new Thread(this, "Main thread");
      pollingThread.setDaemon(true);
      pollingThread.start();
    }
    catch(Exception e)
    {
      System.err.println("Unable to initialise main thread");
      e.printStackTrace();
    }
  }
  
  public void run()
  {
    BASE_MAC = System.getProperty("IEEE_ADDRESS").substring(15);

    FirebaseConnection.upload(true, FirebaseConnection.BRANCH_BASES.child(BASE_MAC), "Base put itself into the list");
    
    //FirebaseConnection.download(FirebaseConnection.BRANCH_REFS); // Only to trigger setup() which, in turn, gets user account data and logs in
    
    
    
    ConnectionProtocolPC.startConnectionResponseServer();
    Logger.openWindow(BASE_MAC);
    Logger.log(Logger.INFO, "started", true);
    ScriptManager.startManager();
  }
}