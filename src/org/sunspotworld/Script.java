package org.sunspotworld;

import com.firebase.client.DataSnapshot;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.StringUtils;
import static org.sunspotworld.ActiveSpotConnection.CMD_SCRIPT;
import static org.sunspotworld.ConnectionProtocolPC.streamMap;
import org.sunspotworld.external.EasyBulb;
import org.sunspotworld.external.Kettle;
import org.sunspotworld.firebase.FirebaseConnection;
import org.sunspotworld.firebase.OnSpotUpdate;
import org.sunspotworld.firebase.SpotListeners;

/**
* @author Povilas Marcinkevicius
* @version 2.0.0
* 
* TODO: Probably could improve performance
 */
public class Script implements OnSpotUpdate
{
  private static final String BASE_CMD_KETTLE = "KETTLE";
  private static final String BASE_CMD_EASYBULB = "EASYBULB";
  
  private static final ScriptEngineManager manager = new ScriptEngineManager();
  private static final ScriptEngine engine = manager.getEngineByName("js");
  
  private static final HashMap<String, String> valueToFirebaseNumber = new HashMap<String, String>();
  private static final HashMap<String, String> valueToFirebaseBoolean = new HashMap<String, String>();
  private static final HashMap<String, String> valueToFirebaseAny     = new HashMap<String, String>();
  private static final ArrayList<String> VALID_BASE_COMMANDS = new ArrayList<String>();
  static
  {
    valueToFirebaseNumber.put("MOTION", FirebaseConnection.ELEMENT_MOTION);
    valueToFirebaseNumber.put("BRIGHTNESS", FirebaseConnection.ELEMENT_LIGHT);
    valueToFirebaseNumber.put("TEMPERATURE", FirebaseConnection.ELEMENT_TEMP);
    valueToFirebaseNumber.put("A2", FirebaseConnection.ELEMENT_A2);
    valueToFirebaseNumber.put("A3", FirebaseConnection.ELEMENT_A3);
    valueToFirebaseNumber.put("COMPASS", FirebaseConnection.ELEMENT_COMPASS);
    valueToFirebaseBoolean.put("D2", FirebaseConnection.ELEMENT_D2);
    valueToFirebaseBoolean.put("D3", FirebaseConnection.ELEMENT_D3);
    valueToFirebaseBoolean.put("INFRARED", FirebaseConnection.ELEMENT_INFRARED);
    valueToFirebaseBoolean.put("SOUND", FirebaseConnection.ELEMENT_SOUND);
    valueToFirebaseBoolean.put("ALIVE", FirebaseConnection.ELEMENT_ALIVE);
    valueToFirebaseBoolean.put("BUTTON_LEFT", FirebaseConnection.ELEMENT_BUTTON_LEFT);
    valueToFirebaseBoolean.put("BUTTON_RIGHT", FirebaseConnection.ELEMENT_BUTTON_RIGHT);
    valueToFirebaseAny.putAll(valueToFirebaseNumber);
    valueToFirebaseAny.putAll(valueToFirebaseBoolean);
    
    VALID_BASE_COMMANDS.add(BASE_CMD_KETTLE);
    VALID_BASE_COMMANDS.add(BASE_CMD_EASYBULB);
  }
  
  public static boolean processCondition(String condition)
  {
    try
    { return (Boolean)engine.eval(StringUtils.replaceEach(condition, new String[]{"AND", "OR", "NOT"}, new String[]{"&&", "||", "!"})); }
    catch (ScriptException e)
    { Logger.log(Logger.ERROR, "Failed to parse condition '" + condition + "'", true); }
    return false;
  }
  ///////////////////////
  // STATIC FINAL END  //
  // OBJECT DATA START //
  ///////////////////////

  private HashMap<String, HashMap<String, Object>> spots = new HashMap<String, HashMap<String, Object>>();
  private ArrayList<String> connectedSpotCommands = new  ArrayList<String>();
  private ArrayList<String> baseCommands = new  ArrayList<String>();
  private String scriptName;
  private long timeout;
  private long lastTriggerTime = 0;
  private String condition;
  private String action;
  
  public Script(){}
  public Script(Script script, String scriptName)
  {
    this.timeout = script.getTimeout();
    this.condition = script.getCondition().trim();
    this.action = script.getAction().trim();
    this.scriptName = scriptName.trim();
    
    addListeners();
    resetMySpots();
  }

  private HashMap<String, Object> createSpotDataSet()
  {    
    HashMap<String, Object> values = new HashMap<String, Object>();
    values.put(FirebaseConnection.ELEMENT_BATTERY, 50.0);
    values.put(FirebaseConnection.ELEMENT_COMPASS, 0);
    values.put(FirebaseConnection.ELEMENT_INFRARED, false);
    values.put(FirebaseConnection.ELEMENT_SOUND, false);
    values.put(FirebaseConnection.ELEMENT_MOTION, 20.0);
    values.put(FirebaseConnection.ELEMENT_LIGHT, 20.0);
    values.put(FirebaseConnection.ELEMENT_TEMP, 20.0);
    values.put(FirebaseConnection.ELEMENT_BUTTON_LEFT, false);
    values.put(FirebaseConnection.ELEMENT_BUTTON_RIGHT, false);
    values.put(FirebaseConnection.ELEMENT_D2, false);
    values.put(FirebaseConnection.ELEMENT_D3, false);
    values.put(FirebaseConnection.ELEMENT_A2, 1.0);
    values.put(FirebaseConnection.ELEMENT_A3, 1.0);
    values.put(FirebaseConnection.ELEMENT_ALIVE, true);
    return values;
  }
  
  public Long getTimeout()
  { return timeout; }

  public void setTimeout(Long timeout)
  { this.timeout = timeout; }

  public String getCondition()
  { return condition; }

  public void setCondition(String condition)
  { this.condition = condition; }

  public String getAction()
  { return action; }

  public void setAction(String action)
  { this.action = action; }
  
  ////////////////////////////////
  //       OBJECT DATA END      //
  // CONDITION PROCESSING START //
  ////////////////////////////////
  
  private void addListeners()
  {
    String[] words = condition.split(" ");
    for(int i = 0; i < words.length; i++)
      if(!addListener(valueToFirebaseNumber, words, i, Double.class)) // if does not add for the first one, tries second
        addListener(valueToFirebaseBoolean, words, i, Boolean.class);
  }

  private boolean addListener(HashMap<String, String> map, String[] words, int i, Class returnType)
  {
    if(map.containsKey(words[i]))
    {
      SpotListeners.addListener(words[i - 1], scriptName, FirebaseConnection.BRANCH_SPOTS.child(words[i - 1]).child(map.get(words[i])), this, returnType);
      if(!spots.containsKey(words[i - 1]))
        spots.put(words[i - 1], createSpotDataSet());
      return true;
    }
    return false;
  }
  
  private void removeListeners()
  {
    String[] words = condition.split(" ");
    for(int i = 0; i < words.length; i++)
      if(valueToFirebaseAny.containsKey(words[i]))
      {
        System.out.println("DEBUG SCRIPT Removing '" + words[i - 1] + "' '" + scriptName + "'");
        SpotListeners.removeListener(words[i - 1], scriptName);
      }
  }
  
  public void onUpdate(DataSnapshot data, String spot, String name, Class clazz)
  {
    spots.get(spot).put(data.getKey(), data.getValue(clazz));
    String replaced = insertValues(condition);
    boolean sendCommands = processCondition(replaced);
    System.out.println("Result for " + condition + " is '" + replaced + "' which is " + sendCommands);
    if(sendCommands)
      executeAction();
  }
  
  private String insertValues(String condition)
  {
    StringBuilder builder = new StringBuilder();
    String[] words = condition.split(" ");
    for(int i = 0; i < words.length; i++)
      if(valueToFirebaseAny.containsKey(words[i]))
      {
        builder.delete(builder.length() - 5, builder.length()); // remove last string which is spot mac and a space
        builder.append(spots.get(words[i - 1]).get(valueToFirebaseAny.get(words[i]))).append(" ");
      }
      else
        builder.append(words[i]).append(" ");
    
    builder.deleteCharAt(builder.length() - 1); // remove last space
    return builder.toString();
  }
  
  //////////////////////////////
  // CONDITION PROCESSING END //
  //  ACTION PROCESSING START //
  //////////////////////////////
  
  // TODO: launch on changed script result
  // TODO: launch on spots (dis)connecting
  public void resetMySpots()
  {
    connectedSpotCommands.clear();
    baseCommands.clear();
    System.out.println(">>> reseting spots for script " + scriptName);
    for(String fullCommand: action.split("; "))
      if(VALID_BASE_COMMANDS.contains(fullCommand.split(" ")[1]))
      {
        if(Base331.BASE_MAC.equals(fullCommand.split(" ")[0]))
        {
          baseCommands.add(fullCommand.substring(5)); // removes base address
          System.out.println(">>> added base cmd " + fullCommand.substring(5)); // TODO: remove
        }
      }
      else
      {
        if(ConnectionProtocolPC.streamMap.containsKey(fullCommand.split(" ")[0])) // if the spot is connected
        {
          connectedSpotCommands.add(fullCommand);
          System.out.println(">>> added spot cmd " + fullCommand); // TODO: remove
        }
      }
  }
  
  private void executeAction()
  {
    long now = System.currentTimeMillis();
    if(lastTriggerTime + timeout < now)
    {
      lastTriggerTime = now;
      sendCommandsToSpots();
      executeBaseCommands();
    }
  }
  
  private void executeBaseCommands()
  {
    for(String command: baseCommands)
    {
      System.out.println("Executing Base Command: " + command);
      String[] words = command.split(" ");
      String commandCut = command.substring(words[0].length() + 1);
      if(words[0].equals(BASE_CMD_KETTLE))
        Kettle.processScript(commandCut.split(" "));
      else if(words[0].equals(BASE_CMD_EASYBULB))
        EasyBulb.processScript(commandCut.split(" "));
    }
  }
  
  private void sendCommandsToSpots()
  {
    for(String fullCommand : connectedSpotCommands)
    {
      String spotName = fullCommand.substring(0, 4);
      String command = fullCommand.substring(5);

      System.out.println(spotName + "< name | cmd >" + command);

      DataOutputStream outputStream = streamMap.get(spotName).streamOut.getConn();
      try
      {
        outputStream.writeByte(CMD_SCRIPT);
        outputStream.writeUTF(command);
        outputStream.flush();
      }
      catch(IOException e)
      { Logger.log(Logger.ERROR, "Failed to send script to SPOT " + spotName, true); }
      streamMap.get(spotName).streamOut.done();
    }
  }
  
  // 1) extract a set of (command + argumets) that apply to spots connected to this base
  // 2) for each process which spot it belongs to, what the command is and the arguments
  
  public void destroy()
  {
    removeListeners();
    spots = null;
  }
  
  public boolean equals(Object o)
  {
    if(o instanceof Script)
    {
      Script script = (Script)o;
      if(script.scriptName.equals(scriptName) && script.action.equals(action) && script.condition.equals(condition))
        return true;
    }
    return false;
  }
}