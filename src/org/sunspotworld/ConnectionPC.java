package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.io.DataInputStream;
import javax.microedition.io.Connector;
import java.util.Random;

/**
 * @author Povilas Marcinkevicius
 * 
 * @version 1.0.0
 */
public class ConnectionPC
{
  public static final String BROADCAST = "broadcast";
  public static final String LISTEN = "";
  
  private static final String FIND_CONNS = "find";
  private static final String STREAM_CONN = "strm";
  
  public static final int PORT_BASE_SEARCH = 33;
  public static final int PORT_BASE_SEARCH_RESPONSE = 34;
  public static final int PORT_TO_PC_DATAGRAMS = 35;
  public static final int STREAM_PORT = 36;
  
  private static final int STREAM_TIMEOUT = 60000;
  
  private RadiogramConnImpl conn;
  private Radiogram radiogram;
  
  /**
   * @param addr "broadcast", "" for listen or IEEEAddress to unicast
   */
  public ConnectionPC(String addr, int port, int radiogramSize)
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

          long lastMsgTime = System.currentTimeMillis();
          long now;
          while(true)
          {
            Week3PC.printStreamData(inputStream);
            now = System.currentTimeMillis();
            if(lastMsgTime + STREAM_TIMEOUT < now)
            {
              System.out.println("Closing stream connection to " + address + " because it timed out");
              inputStream.close();
              return;
            }
            lastMsgTime = now;
          }
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