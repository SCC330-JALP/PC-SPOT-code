package org.sunspotworld;

import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.microedition.io.Connector;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.0
 * 
 * Serves the purpose of putting a lock on stream input to make sure it can't be used by multiple instances
 */
public class StreamConnectionOut
{
  private DataOutputStream conn;     
  private boolean receiving = false; // acts as a lock between getConn() and done()
  private Random random = new Random(System.currentTimeMillis());
  
  public StreamConnectionOut(String addr, int port)
  {
    try
    {
      conn = ((RadiostreamConnection) Connector.open("radiostream://" + addr + ":" + port)).openDataOutputStream();
    }
    catch(IOException e)
    {
      System.err.println("Failed to create stream connection for " + addr + ":" + port);
      e.printStackTrace();
    }
  }
  
  public DataOutputStream getConn()
  {
    try
    {
      while(receiving) 
      {
        Thread.sleep((Math.abs(random.nextInt()) % 100) + 20);
        System.err.println("Two instances are trying to use Stream Connection Out");
      }
    }
    catch (InterruptedException e)
    { System.out.println("getConn() sleep interrupted"); }
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
      System.err.println("Failed to close connection");
      e.printStackTrace();
    }
  }
}