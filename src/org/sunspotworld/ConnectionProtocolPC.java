package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * @author Povilas Marcinkevicius
 * @version 1.1.11
 * 
 * PSP LOC: 26
 */
public class ConnectionProtocolPC
{
  private static final String REQUEST_INTRODUCTION = "intr";
  private static final String REQUEST_ESTABLISH_CONNECTION = "conn";
  private static final String RESPONSE_CONFIRM_CONNECTION = "cnok";
  
  public static final int PORT_NEW_SPOT_LISTEN = 33;
  public static final int PORT_NEW_SPOT_RESPOND = 34;
  public static final int PORT_LISTEN_SPOT_DATA = 36;
  
  public static final HashMap<String, ActiveSpotConnection> streamMap = new HashMap<String, ActiveSpotConnection>();
  
  // Checks if there is stream leftover for the same address and port and removes it
  // Creates a stream to the SPOT with the address provided
  // Saves it in the stream list
  // Launches the code in main function to handle the stream
  private static void openStream(final String address)
  {
    boolean confirmed = true;
    try
    { RadiogramConnectionPC.sendSingleUtfMessage(150, address, PORT_NEW_SPOT_RESPOND, RESPONSE_CONFIRM_CONNECTION); }
    catch(IOException e)
    {
      confirmed = false;
      Logger.log(Logger.WARN, "Failed to confirm stream connection to SPOT " + address, true);
      e.printStackTrace();
    }
    
    if(confirmed)
    {
      new Thread(new Runnable()
      {
        public void run()
        {
          String address4char = address.substring(15);
          if(streamMap.containsKey(address))
          {
            streamMap.get(address4char).close(); // No duplicates
            streamMap.remove(address4char);
            Utils.sleep(100);
          }
          
          StreamConnectionIn inputStream = new StreamConnectionIn(address, PORT_LISTEN_SPOT_DATA);
          streamMap.put(address4char, new ActiveSpotConnection(inputStream, address4char));
          ScriptManager.spotsConnectedChanged();
          Logger.log(Logger.INFO, "Added SPOT " + address4char, true);
        }
      }, "Main Stream Thread for SPOT " + address).start();
    }
  }
  
  // Initialises broadcaster for SPOTs to detect and starts listening for commands
  public static void startConnectionResponseServer()
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        RadiogramConnectionPC listener = new RadiogramConnectionPC(RadiogramConnectionPC.LISTEN, PORT_NEW_SPOT_LISTEN, 127);
        try
        {
          Radiogram radiogramReceive;

          while(true)
          {
            radiogramReceive = listener.receive();
            String request = radiogramReceive.readUTF();
            String address = radiogramReceive.getAddress(); // XXXX.XXXX.XXXX.XXXX
            if(request.startsWith(REQUEST_INTRODUCTION))
              RadiogramConnectionPC.sendSingleUtfMessage(Math.abs(new Random(System.currentTimeMillis()).nextInt()) % 75 + 50, address, PORT_NEW_SPOT_RESPOND, REQUEST_INTRODUCTION);
            else if(request.startsWith(REQUEST_ESTABLISH_CONNECTION))
              openStream(address);
          }
        }
        catch(IOException e)
        {
          Logger.log(Logger.CRITICAL, "Response server failed", true);
          e.printStackTrace(); 
        }
      }
    }, "Connection Response Server").start();
  }
}
