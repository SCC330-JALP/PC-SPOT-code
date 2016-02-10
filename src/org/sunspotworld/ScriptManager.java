package org.sunspotworld;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import java.util.HashMap;
import org.sunspotworld.firebase.FirebaseConnection;

/**
 * @author Povilas marcinkevicius
 * @version 1.0.0
 */
public final class ScriptManager
{
  public static final HashMap<String, Script> scripts = new HashMap<String, Script>(); // TODO static once finished with testing
  
  public static void spotsConnectedChanged()
  {
    for(Script script : scripts.values())
      script.resetMySpots();
  }
  
  public static void startManager()
  {
    FirebaseConnection.BRANCH_SCRIPTS.addChildEventListener(new ChildEventListener()
    {
      public void onChildAdded(DataSnapshot data, String string)
      {
        System.out.println(">>>> Added " + data.getKey());
        Script script = new Script(data.getValue(Script.class), data.getKey());
        script.resetMySpots();
        scripts.put(data.getKey(), script);
      }

      public void onChildChanged(DataSnapshot data, String string)
      {
        onChildRemoved(data);
        onChildAdded(data, string);
      }

      public void onChildRemoved(DataSnapshot data)
      {
        System.out.println(">>>> Removed " + data.getKey());
        scripts.get(data.getKey()).destroy();
        scripts.remove(data.getKey());
      }

      public void onChildMoved(DataSnapshot data, String string)
      { Logger.log(Logger.WARN, "onChildMoved() was triggered. Investigate how in ScriptManager.java", true); }

      public void onCancelled(FirebaseError fe)
      { Logger.log(Logger.ERROR, "Script Manager remote data access failed", true); }
    });
  }
}
