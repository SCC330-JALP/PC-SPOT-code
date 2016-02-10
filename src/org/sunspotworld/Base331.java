package org.sunspotworld;

import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.util.Utils;
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
    //ScriptManager.scripts.put("TestScript", new Script("7ABD ALIVE and 7ABD MOTION < 20.0", "7ABD BEEP 1000 1000 1000 3", 30000, "TestScript"));

    
    
    
    
    FirebaseConnection.download(FirebaseConnection.BRANCH_REFS); // Only to trigger setup() which, in turn, gets user account data and logs in
    ConnectionProtocolPC.startConnectionResponseServer();
    String baseMac = System.getProperty("IEEE_ADDRESS").substring(15);
    Logger.openWindow(baseMac);
    Logger.log(Logger.INFO, "started", true);
    ScriptManager.startManager();
  }
}