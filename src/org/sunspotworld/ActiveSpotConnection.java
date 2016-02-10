package org.sunspotworld;

import com.firebase.client.DataSnapshot;
import com.sun.spot.util.Utils;
import com.sun.squawk.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.sunspotworld.firebase.FirebaseConnection;
import org.sunspotworld.firebase.FirebaseSpotData;
import org.sunspotworld.firebase.OnSpotUpdate;
import org.sunspotworld.firebase.SpotListeners;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.0
 * 
 * used to separate functionality of each spot thread from the main program.
 * PSP LOC: 47
 */
public class ActiveSpotConnection implements OnSpotUpdate
{
  public static final byte ID_COMPASS      = 0;
  public static final byte ID_TEMPERATURE  = 1;
  public static final byte ID_LIGHT        = 2;
  public static final byte ID_ACCELERATION = 3;
  public static final byte ID_BUTTON_LEFT  = 4;
  public static final byte ID_BUTTON_RIGHT = 5;
  public static final byte ID_SOUND        = 6;
  public static final byte ID_BATTERY      = 7;
  public static final byte ID_INFRARED     = 8;
  public static final byte ID_A2           = 9;
  public static final byte ID_A3           = 10;
  public static final byte ID_D2           = 11;
  public static final byte ID_D3           = 12;
  public static final byte ID_PING         = 13;
  public static final byte ID_LOG          = 14;
  
  private static final String[] idToFbElementMap =
  {
    FirebaseConnection.ELEMENT_COMPASS,
    FirebaseConnection.ELEMENT_TEMP,
    FirebaseConnection.ELEMENT_LIGHT,
    FirebaseConnection.ELEMENT_MOTION,
    FirebaseConnection.ELEMENT_BUTTON_LEFT,
    FirebaseConnection.ELEMENT_BUTTON_RIGHT,
    FirebaseConnection.ELEMENT_SOUND,
    FirebaseConnection.ELEMENT_BATTERY,
    FirebaseConnection.ELEMENT_INFRARED,
    FirebaseConnection.ELEMENT_A2,
    FirebaseConnection.ELEMENT_A3,
    FirebaseConnection.ELEMENT_D2,
    FirebaseConnection.ELEMENT_D3
  };
  
  private static final Class[] idToClassMap = 
  {
    Integer.class,
    Double.class,
    Double.class,
    Double.class,
    Boolean.class,
    Boolean.class,
    Boolean.class,
    Integer.class,
    Boolean.class,
    Double.class,
    Double.class,
    Boolean.class,
    Boolean.class
  };
  
  private static final int NUMBER_OF_LISTENERS = 13;
  public static final int PORT_COMMAND_RELAY = 37;
  public static final byte CMD_LISTENERS = 0;
  public static final byte CMD_SCRIPT = 1;
  private static final long PING_WAIT_TIME = 2500;
  
  private static final String LISTENER_UPDATE = "Update Listener";
  private static final String LISTENER_COLLECT = "Collect Listener";

  private static final String CODE_LETTERS = "ctlabrseiwxyz";
  private static final char SAVE_ALIVE = 'o';
  
  private ArrayList<Boolean> storeDataMap = new ArrayList<Boolean>();
  private StreamConnectionIn streamIn;
  public StreamConnectionOut streamOut; // used by script outputs
  private String address;
  private Thread listeningThread;
  private Thread pingThread;
  private long lastPing[] = new long[]{System.currentTimeMillis() + 5000};
  private boolean saveAlive = false;
  
  ActiveSpotConnection(StreamConnectionIn streamConn, final String address)
  {
    this.streamIn = streamConn;
    this.streamOut = new StreamConnectionOut("0014.4F01.0000." + address, PORT_COMMAND_RELAY);
    this.address = address;
    for(int i = 0; i < NUMBER_OF_LISTENERS; i++)
      storeDataMap.add(new Boolean(false));
    
    DataSnapshot spot = FirebaseConnection.download(FirebaseConnection.BRANCH_SPOTS.child(address));
    if(spot == null)
      FirebaseConnection.upload(new FirebaseSpotData(), FirebaseConnection.BRANCH_SPOTS.child(address), "Successfully added new SPOT " + address);
    
    startPingThread();
    startDataThread();
    SpotListeners.addListener(address, LISTENER_COLLECT, FirebaseConnection.BRANCH_SPOTS.child(address).child(FirebaseConnection.ELEMENT_DATA_STORED), this, String.class);
    SpotListeners.addListener(address, LISTENER_UPDATE, FirebaseConnection.BRANCH_SPOTS.child(address).child(FirebaseConnection.ELEMENT_DATA_LIVE), this, String.class);
    
    Utils.sleep(2500);
    onUpdate(FirebaseConnection.download(FirebaseConnection.BRANCH_SPOTS.child(address).child(FirebaseConnection.ELEMENT_DATA_STORED)), address, LISTENER_COLLECT, String.class);
    onUpdate(FirebaseConnection.download(FirebaseConnection.BRANCH_SPOTS.child(address).child(FirebaseConnection.ELEMENT_DATA_LIVE)), address, LISTENER_UPDATE, String.class);
    
    FirebaseConnection.updateSpotElement(address, FirebaseConnection.ELEMENT_ALIVE, true);
    if(saveAlive)
      FirebaseConnection.pushReading(FirebaseConnection.ELEMENT_ALIVE, address, true);
  } 
  
  private void startDataThread()
  {
    listeningThread = new Thread(new Runnable()
    {
      public void run()
      {
        DataInputStream inputStream = streamIn.getConn();
        byte type = -1;
        double value = 0.0;

        while(true)
        {
          try
          {
            type = inputStream.readByte();
            if(type < ID_PING)
              processInput(type, inputStream.readDouble());
            else if(type == ID_PING)
            {
              lastPing[0] = System.currentTimeMillis();
              inputStream.readDouble();
            }
            else if(type == ID_LOG)
              Logger.log(inputStream.readByte(), "SPOT " +  address + ": " + inputStream.readUTF(), false);
          }
          catch(IOException e)
          { return; }
        }
      }
    }, "Listening Thread for SPOT " + address);
    listeningThread.start();
  }
  
  private void processInput(byte type, double value)
  {
    FirebaseConnection.updateSpotElement(address, idToFbElementMap[type], idToClassMap[type] == Boolean.class ? (Boolean)(value > 0.5) : (idToClassMap[type] == Integer.class ? (Integer)(int)value : (Double)value));
    if(storeDataMap.get(type))
      FirebaseConnection.pushReading(idToFbElementMap[type], address, idToClassMap[type] == Boolean.class ? (Boolean)(value > 0.5) : (idToClassMap[type] == Integer.class ? (Integer)(int)value : (Double)value));
  }
  
  private void startPingThread()
  {
    pingThread = new Thread(new Runnable()
    {
      public void run()
      {
        while(lastPing[0] + PING_WAIT_TIME > System.currentTimeMillis())
        {
          try
          { Thread.sleep(500); }
          catch(InterruptedException e)
          { return; /* Interruptions expected when shutting connection down */ }
        } // check every 500ms
        Logger.log(Logger.WARN, "SPOT " + address + " timed out and was disconnected.", false);
        close();
        Thread.currentThread().interrupt();
      }
    }, "Ping Thread for SPOT " + address);
    pingThread.start();
  }
  
  public void close()
  {
    SpotListeners.removeSpot(address);
    FirebaseConnection.updateSpotElement(address, FirebaseConnection.ELEMENT_ALIVE, false);
    if(saveAlive)
      FirebaseConnection.pushReading(FirebaseConnection.ELEMENT_ALIVE, address, false);
    listeningThread.interrupt();
    listeningThread.interrupt();
    streamIn.close();
    streamOut.close();
    ConnectionProtocolPC.streamMap.remove(address);
    ScriptManager.spotsConnectedChanged();
  }

  public void onUpdate(DataSnapshot data, String spot, String name, Class clazz)
  {
    if(name.equals(LISTENER_COLLECT))
    {
      String list = (String)data.getValue();
      saveAlive = list.indexOf(SAVE_ALIVE) >= 0;
      for(int i = 0; i < 12; i++)
        storeDataMap.set(i, Boolean.FALSE);

      boolean[] lettersUnused = new boolean[NUMBER_OF_LISTENERS];
      Arrays.fill(lettersUnused, true);
      
      for(char c: list.toCharArray())
      {
        int index = CODE_LETTERS.indexOf(c);
        if(index >= 0)
        {
          storeDataMap.set(index, Boolean.TRUE);
          lettersUnused[index] = false;
        }
      }
      
      if(!saveAlive)
        FirebaseConnection.upload(null, FirebaseConnection.BRANCH_READINGS.child(spot).child(FirebaseConnection.ELEMENT_ALIVE), /*"SPOT " + spot + " Deleted data stored for " + FirebaseConnection.ELEMENT_ALIVE*/ null);
      for(int i = 0; i < NUMBER_OF_LISTENERS; i++)
        if(lettersUnused[i])
          FirebaseConnection.upload(null, FirebaseConnection.BRANCH_READINGS.child(spot).child(idToFbElementMap[i]), /*"SPOT " + spot + " Deleted data stored for " + idToFbElementMap[i]*/null);
    }
    else if(name.equals(LISTENER_UPDATE))
    {
      DataOutputStream outputStream = streamOut.getConn();
      try
      {
        outputStream.writeByte(CMD_LISTENERS);
        outputStream.writeUTF((String)data.getValue());
        outputStream.flush();
      }
      catch(IOException e)
      { Logger.log(Logger.ERROR, "Failed to send command update to SPOT " + spot, true); }
      streamOut.done();
    }
  }
}