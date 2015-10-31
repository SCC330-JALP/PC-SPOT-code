/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import com.sun.spot.util.Utils;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;
import javax.microedition.io.Connector;

/**
 * @author Povilas Marcinkevicius
 * @version 1.1.2
 */
public class ConnectionProtocolPC
{ 
  private static final String FIND_CONNS = "find";
  private static final String STREAM_CONN = "strm";
  
  public static final int PORT_BASE_SEARCH = 33;
  public static final int PORT_BASE_SEARCH_RESPONSE = 34;
  public static final int PORT_TO_PC_DATAGRAMS = 35;
  public static final int STREAM_PORT = 36;
  
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
    Random random = new Random();
    random.setSeed(System.currentTimeMillis());
    ConnectionPC listener = new ConnectionPC(ConnectionPC.LISTEN, PORT_BASE_SEARCH, 127);

    try
    {
      Radiogram radiogramReceive;

      while(true)
      {
        radiogramReceive = listener.receive();
        String req = radiogramReceive.readUTF();
        System.out.println("got request '" + req + "' from " + radiogramReceive.getAddress()); // TODO: remove

        if(req.equals(FIND_CONNS))
        {
          ConnectionPC responder = new ConnectionPC(radiogramReceive.getAddress(), PORT_BASE_SEARCH_RESPONSE, 10);
          responder.getNewRadiogram().writeInt(zone);
          Utils.sleep(random.nextInt(990) + 10);
          responder.send();
          responder.close();
        }
        else if(req.startsWith(STREAM_CONN))
          openStream(radiogramReceive.getAddress(), req.replace(STREAM_CONN, ""));
        else
          System.out.println("\tSPOT " + radiogramReceive.getAddress() + " sent:\n\t" + req + "\n\tEND");
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
