/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import java.util.HashMap;

/**
 * @author Povilas Marcinkevicius
 * @version 1.1.2
 */
public class ConnectionProtocolPC
{ 
  private static final String STREAM_KILL = "kill";
  private static final String STREAM_CONN = "strm";
  
  public static final int PORT_BASE_SEARCH = 33;
  public static final int PORT_BASE_SEARCH_RESPONSE = 34;
  public static final int PORT_TO_PC_DATAGRAMS = 35;
  public static final int STREAM_PORT = 36;
  
  public static final int BROADCAST_PERIOD = 4000;
  public static final HashMap<String, DataInputStream> streamMap = new HashMap<String, DataInputStream>();
  
  private static void openStream(final String address, final String command)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          DataInputStream inputStream = ((RadiostreamConnection) Connector.open("radiostream://" + address + ":" + STREAM_PORT))
              .openDataInputStream();
          streamMap.put(address, inputStream);

          Week4PC.handleStream(inputStream, address, command);
        }
        catch(IOException e)
        {
          System.err.println("SPOT " + address + " Stream failed");
          e.printStackTrace(); 
        }
      }
    }, "Main Stream Thread").start();
  }
  
  private static void startConnectionResponseServer(final int zone)
  {
    // Start thread broadcasting constantly
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          ConnectionPC responder = new ConnectionPC(ConnectionPC.BROADCAST, PORT_BASE_SEARCH_RESPONSE, 10);
          responder.getNewRadiogram().writeInt(zone);
          System.out.println("Broadcast poller initiated");
          while(true)
          {
            responder.send();
            Thread.sleep(BROADCAST_PERIOD);
          }
        }
        catch(Exception e)
        { e.printStackTrace(); }
      }
    }, "Zone Poller Thread").start();
    
    // Forever listen for requests
    ConnectionPC listener = new ConnectionPC(ConnectionPC.LISTEN, PORT_BASE_SEARCH, 127);
    try
    {
      Radiogram radiogramReceive;

      while(true)
      {
        radiogramReceive = listener.receive();
        String req = radiogramReceive.readUTF();

        String address = radiogramReceive.getAddress();
        System.out.println("\tSPOT " + address + ": " + req);
        if(req.startsWith(STREAM_CONN))
          openStream(address, req.replace(STREAM_CONN, ""));
        else if(req.equals(STREAM_KILL) && streamMap.containsKey(address))
          streamMap.remove(address).close();
      }
    }
    catch(IOException e)
    {
      System.err.println("Failed on connection response server");
      e.printStackTrace(); 
    }
  }
  
  public static void startConnectionResponseServer(final int zone, final boolean newThread)
  {
    if(newThread)
    {
      new Thread(new Runnable()
      {
        public void run()
        { startConnectionResponseServer(zone); }
      }, "Connection Response Server").start();
    }
    else
      startConnectionResponseServer(zone);
  }
}
