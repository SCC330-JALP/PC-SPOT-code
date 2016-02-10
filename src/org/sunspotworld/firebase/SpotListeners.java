package org.sunspotworld.firebase;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.2 Tested
 *
 * Used for easily handling listeners: inserting, deleting and storing properly
 * PSP LOC: 14
**/
public final class SpotListeners
{
  private static class SpotListener
  {
    public Firebase firebase;
    public ValueEventListener listener;
    public String macAddress;
    public String task;
    
    public SpotListener(Firebase firebase, ValueEventListener listener, String macAddress, String task)
    {
      this.firebase = firebase;
      this.listener = listener;
      this.macAddress = macAddress;
      this.task = task;
    }
  }
  
  private static HashMap<String, ArrayList<SpotListener>> listeners = new HashMap<String, ArrayList<SpotListener>>();
  
  // Must make sure 'spot' and 'firebase' correspond to the same spot
  public static void addListener(final String spot, final String task, final Firebase firebase, final OnSpotUpdate listenerAction, final Class clazz)
  {
    ValueEventListener listener = new ValueEventListener()
    {
      @Override
      public void onDataChange(DataSnapshot snapshot)
      { listenerAction.onUpdate(snapshot, spot, task, clazz); }

      @Override
      public void onCancelled(FirebaseError firebaseError)
      { System.out.println("Failed to create listener: " + firebaseError.getMessage()); }
    };
    firebase.addValueEventListener(listener);

    ArrayList<SpotListener> spotListenerList = listeners.get(spot);
    if(spotListenerList == null)
    {
      listeners.put(spot, new ArrayList<SpotListener>());
      spotListenerList = listeners.get(spot);
    }
    spotListenerList.add(new SpotListener(firebase, listener, spot, task));
    
    print();
  }

  public static void removeSpot(String spot)
  {
    ArrayList<SpotListener> spotListenerList = listeners.get(spot);
    if(spotListenerList != null)
      for(SpotListener listener: spotListenerList)
        listener.firebase.removeEventListener(listener.listener);
    
    listeners.remove(spot);
    
    print();
  }
  
  public static void removeListener(String spot, String task)
  {
    ArrayList<SpotListener> spotListenerList = listeners.get(spot);
    if(spotListenerList != null)
      for(int i = 0; i < spotListenerList.size(); i++)
      {
        System.out.println("<><><> " + spotListenerList.get(i).task);
        if(spotListenerList.get(i).task.equals(task))
        {
          spotListenerList.get(i).firebase.removeEventListener(spotListenerList.get(i).listener);
          spotListenerList.remove(i);
        }
      }
    print();
  }
  
  private static void print()
  {
    System.out.println("> SPOT Listeners:");
    for(String spot: listeners.keySet())
    {
      System.out.println(">> " + spot);
      for(SpotListener listns: listeners.get(spot))
        System.out.println(">>> " + listns.task);
    }
  }
}
