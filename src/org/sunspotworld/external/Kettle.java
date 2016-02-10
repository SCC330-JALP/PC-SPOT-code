package org.sunspotworld.external;

import com.sun.spot.util.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import org.sunspotworld.Logger;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.0
 * 
 * Took Liam Cottiers code, fixed and improved it.
 */
public class Kettle
{
  public static interface Listener
  { public void onKettleTrigger(String event); }
  
  private static final String HOST_IP = "192.168.0.100";
  private static final int HOST_PORT = 2000;  
  private static final String OUTPUT_PREFIX = "set sys output 0x";
  private static final String INPUT_PREFIX = "sys status 0x";
  private static final HashMap<Integer, String> mapOut = new HashMap<Integer, String>();
  private static final HashMap<String, String> mapIn = new HashMap<String, String>();
  private static final ArrayList<Listener> subscribers = new ArrayList<Listener>();
  
  private static Socket echoSocket;
  private static PrintWriter kettleOut;
  private static BufferedReader kettleIn;
  private static boolean exists = true;
  
  public static final int TEMP_100 = 100;
  public static final int TEMP_95 = 95;
  public static final int TEMP_80 = 80;
  public static final int TEMP_65 = 65;
  
  public static final String TIME_NONE = "8";
  public static final String TIME_5_MINS = "8005";
  public static final String TIME_10_MINS = "8010";
  public static final String TIME_20_MINS = "8020";
  
  static
  {
    mapOut.put(TEMP_65, "200");
    mapOut.put(TEMP_80, "4000");
    mapOut.put(TEMP_95, "2");
    mapOut.put(TEMP_100, "80");
    
    mapIn.put("100", "100_Celsius_Set");
    mapIn.put("95", "95_Celsius_Set");
    mapIn.put("80", "80_Celsius_Set");
    mapIn.put("65", "65_Celsius_Set");
    mapIn.put("11", "Warm_Selected");
    mapIn.put("10", "Warm_Ended");
    mapIn.put("8005", "Warm_5_Min");
    mapIn.put("8010", "Warm_10_Min");
    mapIn.put("8020", "Warm_20_Min");
    mapIn.put("5", "Turned_On");
    mapIn.put("0", "Turned_Off");
    mapIn.put("3", "Reached_Temp");
    mapIn.put("2", "Problem");
    mapIn.put("1", "Kettle_Removed_While_On");
    
    try
    {
      echoSocket = new Socket(HOST_IP, HOST_PORT);
      kettleOut = new PrintWriter(echoSocket.getOutputStream(), true);
      kettleIn = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
      
      kettleOut.println("HELLOKETTLE\\n");
      exists = kettleIn.readLine().equals("HELLOAPP");
      
      startListener();
    }
    catch (IOException e)
    { exists = false; }
  }
   
  private static void startListener()
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        while(true)
        {
          try
          {
            String data = kettleIn.readLine();
            data = data.substring(INPUT_PREFIX.length());
            if(mapIn.containsKey(data))
            {
              String event = mapIn.get(data);
              for(Listener listener: subscribers)
                listener.onKettleTrigger(event);
            }
          }
          catch(IOException e)
          { Logger.log(Logger.ERROR, "Kettle listener encountered an issue", true); }
        }
      }
    }, "Kettle listener thread").start();
  }
  
  public static void boilKettle(int temperature)
  {
    if(exists)
    {
      if(!mapOut.containsKey(temperature))
        throw new IllegalArgumentException("Provided temperature value is not one of the TEMP_XXX values");

      kettleOut.println(OUTPUT_PREFIX + "4");
      Utils.sleep(50);
      kettleOut.println(OUTPUT_PREFIX + mapOut.get(temperature));
    }
  }

  public static void setWarmOn(String time)
  {
    kettleOut.println(OUTPUT_PREFIX + "4");
    Utils.sleep(50);
    kettleOut.println(OUTPUT_PREFIX + time);
  }
  
  public static void turnOffKettle()
  {
    if(exists)
      kettleOut.println(OUTPUT_PREFIX + "0");
  }

  public static boolean exists()
  { return exists; }
  
  public static void subscribe(Listener subscriber)
  {
    if(!subscribers.contains(subscriber) && exists)
      subscribers.add(subscriber);
  }
  
  public static void unsubscribe(Listener subscriber)
  {
    if(subscribers.contains(subscriber) && exists)
      subscribers.remove(subscriber);
  }
}