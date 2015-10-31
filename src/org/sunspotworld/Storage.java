package org.sunspotworld;

import com.sun.spot.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.0
 */
public class Storage
{
  private static final Path path = Paths.get("storage");
  
  private static Map<String, String> entries = new HashMap<String, String>();
  
  // Loads up the stored data
  public Storage()
  {
    try
    {
      File file = new File("storage");
      if(!file.exists())
      { file.createNewFile(); } 

      Utils.sleep(500);
      List<String> lines = Files.readAllLines(path);
      for(String line: lines)
      {
        String lineParts[] = line.split(" = ");
        entries.put(lineParts[0], lineParts[1]);
      }
    }
    catch(IOException e)
    {
      System.err.print("Failed to create/load storage");
      e.printStackTrace();
    }
    
    System.out.println("Storage loaded");
  }
  
  public String getOrCreateEntry(String key)
  {
    // get zone number
    String value = entries.get(key);
    if (value == null)
    {
      System.out.println("Please enter a value for '" + key + "': ");
      value = new Scanner(System.in).next();
      addEntry(key, value);
    }
    return value;
  }
  
  public void addEntry(String key, String value)
  {
    entries.put(key, value);
    saveData();
  }
  
  private void saveData()
  {
    ArrayList<String> lines = new ArrayList<String>();
    for(String key: entries.keySet())
      lines.add(key + " = " + (String)entries.get(key));
    
    try { Files.write(path, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE); }
    catch(IOException e)
    {
      System.err.println("Failed to write to storage");
      e.printStackTrace();
    }
  }
}