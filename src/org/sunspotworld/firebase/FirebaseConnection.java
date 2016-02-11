package org.sunspotworld.firebase;

import com.firebase.client.*;
import com.firebase.client.Firebase.AuthResultHandler;
import com.sun.spot.util.Utils;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * @author Povilas Marcinkevicius
 * @version 2.1.0 Tested
 * 
 * Provides general functions to interface with Firebase easily.
 * PSP LOC: 43
 */
public final class FirebaseConnection
{
  public static final int TIMEOUT_TIMES_100MS = 150; // 15 seconds
  
  public static final Firebase FIREBASE = new Firebase("https://sunsspot.firebaseio.com/users/" + setup() + "/data/");
  public static final Firebase BRANCH_READINGS = FIREBASE.child("readings");
  public static final Firebase BRANCH_SPOTS = FIREBASE.child("spots");
  public static final Firebase BRANCH_BASES = FIREBASE.child("bases");
  public static final Firebase BRANCH_SCRIPTS = FIREBASE.child("scripts");
  public static final Firebase BRANCH_REFS = new Firebase("https://sunsspot.firebaseio.com/usersRef/");
  //public static final Firebase BRANCH_BUZZ = FIREBASE.child("buzz");
  //public static final Firebase BRANCH_ALARMS = FIREBASE.child("alarms");
  
  public static final String ELEMENT_TEMP = "temp";
  public static final String ELEMENT_LIGHT = "light";
  public static final String ELEMENT_MOTION = "accel";
  public static final String ELEMENT_BUTTON_LEFT = "btn_l";
  public static final String ELEMENT_BUTTON_RIGHT = "btn_r";
  public static final String ELEMENT_BATTERY = "battery";
  public static final String ELEMENT_COMPASS = "compass";
  public static final String ELEMENT_SOUND = "sound";
  public static final String ELEMENT_INFRARED = "infrared";
  public static final String ELEMENT_D2 = "d2";
  public static final String ELEMENT_D3 = "d3";
  public static final String ELEMENT_A2 = "a2";
  public static final String ELEMENT_A3 = "a3";
  public static final String ELEMENT_H2 = "h2";
  public static final String ELEMENT_H3 = "h3";
  public static final String ELEMENT_ALIVE = "alive";
  public static final String ELEMENT_DATA_STORED = "storedData";
  public static final String ELEMENT_DATA_LIVE = "liveData";
  public static final String ELEMENT_NAME = "name";
  
  ////// SETUP AND AUTHENTICATION //////
  
  private static class Authentication implements AuthResultHandler
  {
    private int[] auth = {0};
    
    public void onAuthenticated(AuthData a)
    {
      System.out.println("Authenticated successfully!");
      auth[0] = 1;
    }

    public void onAuthenticationError(FirebaseError e)
    {
      System.out.println("Authentication failed.");
      auth[0] = -1;
    }
    
    public boolean getResult()
    {
      while(auth[0] == 0)
        Utils.sleep(50);
      return auth[0] == 1;
    }
  }
  
  public static String setup()
  {
    String email = null;
    boolean emailOk = false;
    boolean passwordOk = false;
    String message = "Zeus Login";
    DataSnapshot uid = null;

    JTextField emailField = new JTextField(20);
    JPasswordField passwordField = new JPasswordField(20);

    JPanel myPanel = new JPanel();
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
    myPanel.add(new JLabel("email:"));
    myPanel.add(emailField);
    myPanel.add(new JLabel("password:"));
    myPanel.add(passwordField);

    while(true)
    {
      int result = JOptionPane.showConfirmDialog(null, myPanel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon("lock.png"));
      if (result == JOptionPane.OK_OPTION)
      {
        email = emailField.getText();
        if(email.contains(" "))
          message = "Zeus Login: you cannot have email with spaces";
        else
        {
          Firebase firebase = new Firebase("https://sunsspot.firebaseio.com/").child("usersRef").child(email.replace(".", " ").replace("@", " "));
          uid = download(firebase);

          if(uid != null)
          {
            Authentication auth = new Authentication();
            new Firebase("https://sunsspot.firebaseio.com/").child((String)(uid.getValue())).authWithPassword(email, new String(passwordField.getPassword()), auth);
            if(auth.getResult())
            {
              JOptionPane.showMessageDialog(null, "Login successful!");
              return (String)(uid.getValue());
            }
            else
              message = "Zeus Login: wrong password";
          }
          else
            message = "Zeus Login: such email is not registered";
        }
      }
      else
        System.exit(0);
    }
  }
  
    ////// SETUP AND AUTHENTICATION END //////
  
  public static DataSnapshot download(Firebase location)
  {
    final DataSnapshot returnValue[] = {null};
    
    ValueEventListener listener = new ValueEventListener()
    {
      @Override
      public void onDataChange(DataSnapshot snapshot)
      { returnValue[0] = snapshot; }

      @Override
      public void onCancelled(FirebaseError firebaseError)
      { System.out.println("The read failed: " + firebaseError.getMessage()); }
    };
    
    location.addListenerForSingleValueEvent(listener);
    location.removeEventListener(listener);

    for(int i = 0; returnValue[0] == null; i++)
    {
      try { Thread.sleep(100); }
      catch(InterruptedException e)
      { e.printStackTrace(); }
      
      if(i > TIMEOUT_TIMES_100MS)
        return null;
    }

    if(returnValue[0].exists())
      return returnValue[0];
    return null;
  }
  
  public static <Type> void upload(Type data, Firebase location, final String successMsg)
  {
    location.setValue(data, new Firebase.CompletionListener()
    {
      @Override
      public void onComplete(FirebaseError firebaseError, Firebase firebase)
      {
        if(successMsg != null)
          System.out.println(successMsg);
      }
    });
  }
  
  public static <Value> void updateSpotElement(String macAddress, String element, Value value)
  { upload(value, BRANCH_SPOTS.child(macAddress).child(element), "SPOT " + macAddress + " " + element + " updated."); }
  
  public static <Value> void pushReading(String subBranch, String macAddress, Value newVal)
  { upload(newVal, BRANCH_READINGS.child(macAddress).child(subBranch).child(System.currentTimeMillis() + ""), "SPOT " + macAddress + " " + subBranch + " reading pushed."); }
}