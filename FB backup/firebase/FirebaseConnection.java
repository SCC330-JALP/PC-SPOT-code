package org.sunspotworld.firebase;

import com.firebase.client.*;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author Povilas Marcinkevicius, Liam Cottier, Anson Cheung
 * 
 * Contributions:
 *   Liam Cottier > Core functionality of connecting to Firebase to update and retrieve SPOT data and push SPOT readings
 *   Anson Cheung > Functionality of pushing data to zone portion of the databases (which is different by not being SPOT specific)
 *   Povilas Marcinkevicius > Taking their code and formatting it into easily usable fully static implementation with some additional functionality derived from Liams code.
 *   Povilas Marcinkevicius > Commenting
 * 
 * @version 1.1.2
 */
public class FirebaseConnection
{
  // Used for storing connections and listeners so they can be disabled later
  // Current task is used to see if the task has changed when Firebase detects an event
  public static class Spot
  {
    public Firebase firebase;
    public ValueEventListener listener;
    public String currentTask;
    
    public Spot(Firebase firebase, ValueEventListener listener, String currentTask)
    {
      this.firebase = firebase;
      this.listener = listener;
      this.currentTask = currentTask;
    }
  }
  
  public static final SPOTDBentry TIMEOUT = new SPOTDBentry(null, null, -1);
  private static final Firebase ref = new Firebase("https://sunsspot.firebaseio.com/");
  public static final Firebase BRANCH_SETTINGS = ref.child("spotSettings");
  public static final Firebase BRANCH_READINGS = ref.child("spotReadings");
  public static final String READINGS_ZONE = "zone";
  public static final String READINGS_TEMP = "temp";
  public static final String READINGS_LIGHT = "light";
  public static final String READINGS_MOTION = "motion";
  public static final String READINGS_BUTTON = "button";
  private static final HashMap<String, Spot> spots = new HashMap<String, Spot>();
  
  // Get a SPOT from the "spots" HashMap (having a value in the HashMap means it has an active listener)
  public static Spot getStoredSpot(String macAddress)
  {
    macAddress = macAddress.replace(".", " ");
    return spots.containsKey(macAddress) ? spots.get(macAddress) : null;
  }
  
  // Override a SPOT entry in firebase with the parameters provided
  public static void updateSPOT(String macAddressInitial, String name, String task, long zone) throws InterruptedException
  {
    final String macAddress = macAddressInitial.replace(".", " ");
    final CountDownLatch done = new CountDownLatch(1);
    SPOTDBentry newEntry = new SPOTDBentry(name, task, zone);
    BRANCH_SETTINGS.child(macAddress).setValue(newEntry, new Firebase.CompletionListener()
    {
      @Override
      public void onComplete(FirebaseError firebaseError, Firebase firebase)
      {
        System.out.println("Successfully pushed to spot " + macAddress);
        done.countDown();
      }
    });
    done.await();
  }

  /**
   * Create a listener for a SPOT
   * 
   * @param macAddressInitial the MAC address in format XXXX.XXXX.XXXX.XXXX of the SPOT to create a listener
   * @param variation optional arbitrary value used to select a path of execution inside of the listenerAction provided. Currently unused
   * @param listenerAction class implementing "OnSpotUpdate" which allows custom code to be inserted for execution on listener creation
   * @param stream not originally intended to be here it was added because it made handling the threads easier (allows to close the input stream on event)
   * @param currentTask the task that the SPOT is supposed to be executing
   */
  public static void addSpotListener(String macAddressInitial, final int variation, final OnSpotUpdate listenerAction, final DataInputStream stream, final String currentTask)
  {
    final String macAddress = macAddressInitial.replace(".", " ");
    if(!spots.containsKey(macAddress))
    {
      Firebase spot = BRANCH_SETTINGS.child(macAddress);

      ValueEventListener listener = new ValueEventListener()
      {
        @Override
        public void onDataChange(DataSnapshot snapshot)
        {
          SPOTDBentry entry = snapshot.getValue(SPOTDBentry.class);
          listenerAction.onUpdate(entry.getName() == null || entry.getTask() == null ? null : entry, variation, stream, macAddress);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError)
        { System.out.println("The read failed: " + firebaseError.getMessage()); }
      };

      spot.addValueEventListener(listener);
      spots.put(macAddress, new Spot(spot, listener, currentTask));
    }
          
      // todo: remove
      System.out.println("Current spots:");
      for(String spotMac: spots.keySet())
        System.out.println(spotMac + " " + spots.get(spotMac));
  }
  
  // Gets an instance of SPOTDBentry from Firebase where the macAddress matches.
  // Returns null if the macAddress does not exist or retrieval times out.
  public static SPOTDBentry getSpotData(String macAddress)
  {
    macAddress = macAddress.replace(".", " ");
    final SPOTDBentry returnValue[] = {null};
    Firebase spot = BRANCH_SETTINGS.child(macAddress);
    
    ValueEventListener listener = new ValueEventListener()
    {
      @Override
      public void onDataChange(DataSnapshot snapshot)
      { returnValue[0] = snapshot.getValue(SPOTDBentry.class); }

      @Override
      public void onCancelled(FirebaseError firebaseError)
      { System.out.println("The read failed: " + firebaseError.getMessage()); }
    };
    
    spot.addListenerForSingleValueEvent(listener);
    spot.removeEventListener(listener);

    for(int i = 0; returnValue[0] == null; i++)
    {
      try { Thread.sleep(100); }
      catch(InterruptedException e)
      { e.printStackTrace(); }
      
      if(i > 150)
        return TIMEOUT;
    }
    
    return returnValue[0].getName() == null || returnValue[0].getTask() == null ? null : returnValue[0];
  }

  // stops the listener for selected macAddress and removes it from the spots list
  public static void removeSpotListener(String macAddress)
  {
    macAddress = macAddress.replace(".", " ");
    if(spots.containsKey(macAddress))
    {
      Spot spot = spots.get(macAddress);
      spot.firebase.removeEventListener(spot.listener);
      spots.remove(macAddress);
    }
    // todo: remove
      System.out.println("Current spots:");
      for(String spotMac: spots.keySet())
        System.out.println(spotMac + " " + spots.get(spotMac));
  }
  
  // A class created to allow pushing a set of two values to Firebase as a sensor reading. Can't get around it.
  private static class SensorReading
  {
    public float newVal;
    public long timestamp;
    
    public SensorReading(long timestamp, float newVal)
    {
      this.newVal = newVal;
      this.timestamp = timestamp;
    }
  }
  
  // Only function using SensorReading. Pushes time and value of readings to the Readings section of the macAddress selected.
  public static void pushReading(Firebase branch, String subBranch, String macAddress, float newVal, long timeStamp)
  {
    macAddress = macAddress.replace(".", " ");
    if(branch == BRANCH_SETTINGS)
    {
      final Firebase spot = branch.child(macAddress);
      spot.push().setValue(new SensorReading(timeStamp, newVal), new Firebase.CompletionListener()
      {
        @Override
        public void onComplete(FirebaseError firebaseError, Firebase firebase)
        { System.out.println("Successfully pushed SPOT data"); }
      });
    }
    else
    {
      final Firebase location = branch.child(macAddress).child(subBranch);
      location.push().setValue(new SensorReading(timeStamp, newVal), new Firebase.CompletionListener()
      {
        @Override
        public void onComplete(FirebaseError firebaseError, Firebase firebase)
        { System.out.println("Successfully pushed readings data"); }
      });
    }
  }
  
  ///////////////////////////////////////////////
  //    IMPORTED FROM ANSONS IMPLEMENTATION    //
  ///////////////////////////////////////////////
  
  // Push temperature and light readings along with the timestamp of when they were taken.
  // Zone selected by in which zone the macAddress is.
  public static void updateZoneData(String macAddressInitial, double temp, double light, long timestamp) throws InterruptedException
  {
    final String macAddress = macAddressInitial.replace(".", " ");
    final CountDownLatch done = new CountDownLatch(1);
    SpotDbZoneEntry newEntry = new SpotDbZoneEntry(temp, light, timestamp);

    ref.child(macAddress).push().setValue(newEntry, new Firebase.CompletionListener() {
      @Override
      public void onComplete(FirebaseError firebaseError, Firebase firebase) {
        System.out.println("Successfully pushed to " + macAddress);
        done.countDown();
      }
    });
    done.await();
  }

  // Removes entries older than rangeInMiliseconds from the zone data set specified
  public static void removeOldZoneEntries(String zonenameInitial, final long rangeInMiliseconds)
  {
    final String zoneName = zonenameInitial.replace(".", " ");
    ref.child(zoneName).addValueEventListener(new ValueEventListener()
    {
      @Override
      public void onDataChange(DataSnapshot snapshot)
      {
        for (DataSnapshot entrySnapshot: snapshot.getChildren())
        {
          SpotDbZoneEntry entry = entrySnapshot.getValue(SpotDbZoneEntry.class);

          if(entry.getTimestamp() < (System.currentTimeMillis() - rangeInMiliseconds))
            entrySnapshot.getRef().removeValue();
        }
      }

      @Override
      public void onCancelled(FirebaseError firebaseError)
      { System.out.println("The read failed: " + firebaseError.getMessage()); }
    });
  }
}