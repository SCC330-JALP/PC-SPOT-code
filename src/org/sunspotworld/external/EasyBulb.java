package org.sunspotworld.external;

import com.sun.spot.util.Utils;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import org.sunspotworld.Logger;

public final class EasyBulb
{
  static DatagramSocket datagramSocket;
  static InetAddress address;
  static EasyBulb bulb;
  
  private static final byte ON = 0;
  private static final byte OFF = 1;
  private static final byte WHITE = 2;
  private static final HashMap<String, byte[]> codes = new HashMap<String, byte[]>();
  
  static
  {
    codes.put("1", new byte[]{(byte)0x45, (byte)0x46, (byte)0xC5});
    codes.put("2", new byte[]{(byte)0x47, (byte)0x48, (byte)0xC7});
    codes.put("3", new byte[]{(byte)0x49, (byte)0x4A, (byte)0xC9});
    codes.put("4", new byte[]{(byte)0x4B, (byte)0x4C, (byte)0xCB});
    codes.put("ALL", new byte[]{(byte)0x42, (byte)0x41, (byte)0xC2});
    
    try
    {
      datagramSocket = new DatagramSocket(8899);
      address = InetAddress.getByName("192.168.0.255");
    }
    catch(IOException e)
    { Logger.log(Logger.ERROR, "Failed to create EasyBulb socket", true); }
  }
  
  public static void processScript(String[] taskGroupValue)
  {
    if(!codes.containsKey(taskGroupValue[1]))
    {
      Logger.log(Logger.ERROR, "EasyBulb group provided: " + taskGroupValue[1] + "; Valid values: 1, 2, 3, 4, ALL", true);
      return;
    }
    
    try
    {
      String task = taskGroupValue[0];
      String value = "0";
      if(taskGroupValue.length > 2)
        value = taskGroupValue[2];

      if(task.equals("COLOUR"))
      {
        datagramSocket.send(new DatagramPacket(new byte[]{0x4E, 0x19, 0x55}, 3, address, 8899)); // thing to do with changing clr?
        Utils.sleep(10);
        datagramSocket.send(new DatagramPacket(new byte[]{codes.get(taskGroupValue[1])[ON], 0x00, 0x55}, 3, address, 8899)); // turn on
        Utils.sleep(10);
        datagramSocket.send(new DatagramPacket(new byte[]{0x40, (byte) Byte.parseByte(value), 0x55}, 3, address, 8899)); // change clr
      }

      else if(task.equals("ON"))
        datagramSocket.send(new DatagramPacket(new byte[]{codes.get(taskGroupValue[1])[ON], 0x00, 0x55}, 3, address, 8899));

      else if(task.equals("OFF"))
        datagramSocket.send(new DatagramPacket(new byte[]{codes.get(taskGroupValue[1])[OFF], 0x00, 0x55}, 3, address, 8899)); // turn off

      if(task.equals("BRIGHTNESS"))// range -127 to -99
      {
        datagramSocket.send(new DatagramPacket(new byte[]{codes.get(taskGroupValue[1])[ON], 0x00, 0x55}, 3, address, 8899));
        Utils.sleep(10);
        datagramSocket.send(new DatagramPacket(new byte[]{0x4E, (byte) Math.min(Byte.parseByte(value), (byte)-99), 0x55}, 3, address, 8899));
      }

      if(task.equals("WHITE"))
        datagramSocket.send(new DatagramPacket(new byte[]{(byte)codes.get(taskGroupValue[1])[WHITE], 0x00, 0x55}, 3, address, 8899));
    }
    catch(IOException e)
    { Logger.log(Logger.ERROR, "Error while sending EasyBuld command " + taskGroupValue[0] + taskGroupValue[1] + (taskGroupValue.length > 2 ? " " + taskGroupValue[2] : ""), true); }
  }
}