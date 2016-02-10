package org.sunspotworld;

import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.io.Connector;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.0
 * 
 * Serves the purpose of putting a lock on stream input to make sure it can't be used by multiple instances
 * PSP LOC: 12
 */
public class StreamConnectionIn
{
  private DataInputStream conn;     
  private boolean receiving = false; // acts as a lock between getNewRadiogram() and send()
  private String address;
  private int port;
  
  public StreamConnectionIn(String addr, int port)
  {
    address = addr.substring(15);
    this.port = port;
    try
    { conn = ((RadiostreamConnection) Connector.open("radiostream://" + addr + ":" + port)).openDataInputStream(); }
    catch(IOException e)
    {
      Logger.log(Logger.ERROR, "Failed to create stream connection for " + address + ":" + port, true);
      e.printStackTrace();
    }
  }
  
  public DataInputStream getConn()
  {
    try
    {
      while(receiving) 
        Thread.sleep(50);
    }
    catch (InterruptedException e)
    { Logger.log(Logger.ERROR, "Stream connection lock instance broken", true); }
    receiving = true;
    return conn;
  }
  
  public void done()
  { receiving = false; }
  
  public void close()
  {
    try
    { conn.close(); }
    catch(IOException e)
    {
      Logger.log(Logger.ERROR, "Failed to close stream connection for " + address + ":" + port, true);
      e.printStackTrace();
    }
  }
}