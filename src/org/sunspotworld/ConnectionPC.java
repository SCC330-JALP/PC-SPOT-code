package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import java.io.IOException;
import javax.microedition.io.Connector;

/**
 * @author Povilas Marcinkevicius
 * @version 1.1.3
 */
public class ConnectionPC
{
  public static final String BROADCAST = "broadcast";
  public static final String LISTEN = "";

  private RadiogramConnImpl conn;
  private Radiogram radiogram;
  
  /**
   * @param addr "broadcast", "" for listen or IEEEAddress to unicast
   */
  public ConnectionPC(String addr, int port, int radiogramSize)
  { start(addr, port, radiogramSize); }
  
  public void start(String addr, int port, int radiogramSize)
  {
    try
    {
      conn = (RadiogramConnImpl) Connector.open("radiogram://" + addr + ":" + port);
      radiogram = new Radiogram(radiogramSize, conn);
    }
    catch(IOException e)
    {
      System.err.println("Failed to open connection");
      e.printStackTrace();
    }
  }
  
  public Radiogram receive() throws IOException
  {
    conn.receive(radiogram);
    return radiogram;
  }
  
  public Radiogram getNewRadiogram()
  {
    radiogram.reset();
    return radiogram;
  }
  
  public void send() throws IOException
  { conn.send(radiogram); }
  
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