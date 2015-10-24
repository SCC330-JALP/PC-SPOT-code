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
import static org.sunspotworld.ConnectionPC.BROADCAST;
import static org.sunspotworld.ConnectionPC.LISTEN;

/**
 *
 * @author Chicken
 */
public class ConnectionProtocolPC
{
  private static final String FIND_CONNS = "find";
  private static final String STREAM_CONN = "strm";
  
  public static final int PORT_BASE_SEARCH = 33;
  public static final int PORT_BASE_SEARCH_RESPONSE = 34;
  public static final int PORT_TO_PC_DATAGRAMS = 35;
  public static final int STREAM_PORT = 36;
  
  private static void openStream(final String address)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          DataInputStream inputStream = ((RadiostreamConnection) Connector.open("radiostream://" + address + ":" + STREAM_PORT))
              .openDataInputStream();

          while(true)
            Week3PC.printStreamData(inputStream);
        }
        catch(IOException e)
        {
          System.err.println("SPOT " + address + " Stream failed");
          e.printStackTrace(); 
        }
      }
    }).start();
  }
  
  private static void startConnectionResponseServer(final int zone)
  {
    Random random = new Random();
    random.setSeed(System.currentTimeMillis());
    ConnectionPC listener = new ConnectionPC(LISTEN, PORT_BASE_SEARCH, 127);
    ConnectionPC responder = new ConnectionPC(BROADCAST, PORT_BASE_SEARCH_RESPONSE, 10);

    try
    {
      Radiogram radiogramReceive;
      Radiogram radiogramSend = responder.getNewRadiogram();
      radiogramSend.writeInt(zone);

      while(true)
      {
        radiogramReceive = listener.receive();
        String req = radiogramReceive.readUTF();
        System.out.println("got request '" + req + "' from " + radiogramReceive.getAddress());

        if(req.equals(FIND_CONNS))
        {
          Utils.sleep(random.nextInt(990) + 10);
          responder.send();
        }
        else if(req.equals(STREAM_CONN))
          openStream(radiogramReceive.getAddress());
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
      }).start();
    }
    else
      startConnectionResponseServer(zone);
  }
}
