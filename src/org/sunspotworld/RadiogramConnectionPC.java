package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import javax.microedition.io.Connector;

/**
 * @author Povilas Marcinkevicius
 * @version 1.1.6
 * 
 * PSP LOC: 32
 */
public class RadiogramConnectionPC
{
  public static final String BROADCAST = "broadcast";
  public static final String LISTEN = "";

  private RadiogramConnImpl conn;
  private Radiogram radiogram;
  private boolean sending = false; // acts as a lock between getNewRadiogram() and send()
  private String address;
  
  /**
   * @param addr "broadcast", "" for listen or IEEEAddress to unicast
   */
  public RadiogramConnectionPC(String addr, int port, int radiogramSize)
  {
    try
    {
      conn = (RadiogramConnImpl) Connector.open("radiogram://" + addr + ":" + port);
      radiogram = new Radiogram(radiogramSize, conn);
      address = addr.length() == 0 ? "listen" : addr.length() > 15 ? addr.substring(15) : addr;
    }
    catch(IOException e)
    {
      Logger.log(Logger.ERROR, "Failed to create connection for " + address + ":" + port, true);
      e.printStackTrace();
    }
  }
  
  public synchronized Radiogram receive() throws IOException
  {
    conn.receive(radiogram);
    return radiogram;
  }
  
  public Radiogram getNewRadiogram()
  {
    Utils.sleep(50);
    sending = true;
    radiogram.reset();
    return radiogram;
  }
  
  public void send() throws IOException
  {
    conn.send(radiogram);
    sending = false;
  }
  
  public void close()
  {
    try
    { conn.close(); }
    catch(IOException e)
    {
      Logger.log(Logger.ERROR, "Failed to close connection for " + address + ":" + conn.getLocalPort(), true);
      e.printStackTrace();
    }
  }

  public static void sendSingleUtfMessage(long delay, String addr, int port, String message) throws IOException
  {
    Utils.sleep(delay);
    RadiogramConnectionPC responseConn = new RadiogramConnectionPC(addr, port, message.length() + 10);
    responseConn.getNewRadiogram().writeUTF(message);
    responseConn.send();
    responseConn.close();
  }
  
  // Unless I copy data from radiograms to such data structure right away, they don't function properly. Strange. But neccessary
  public static class RadiogramRef
  {
    public String address;
    public int rssi;
    public String utf;
    public RadiogramRef(Radiogram radiogram)
    {
      try
      {
        this.address = radiogram.getAddress();
        this.rssi = radiogram.getRssi();
        this.utf = radiogram.readUTF();
      }
      catch(IOException e)
      {
        Logger.log(Logger.ERROR, "Failed to read radiogram", true);
        e.printStackTrace();
      }
    }
  }
  
  public static ArrayList<RadiogramRef> receiveMessagesForTime(String addr, int port, int searchTimeMs)
  {
    final RadiogramConnectionPC conn = new RadiogramConnectionPC(addr, port, 127);
    final ArrayList<RadiogramRef> responses = new ArrayList<RadiogramRef>();

    Thread listener = new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          while(true)
          {
            Radiogram radiogram = conn.receive(); 
            responses.add(new RadiogramRef(radiogram));
          }
        }
        catch(IOException e)
        { return; } // Just stop when interrupted
      }
    });

    listener.start();
    Utils.sleep(searchTimeMs);
    listener.interrupt();
    listener.interrupt();
    conn.close();
    return responses;
  }
}