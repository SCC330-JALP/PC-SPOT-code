package org.sunspotworld.external;

import com.sun.spot.util.Utils;
import java.io.IOException;
import java.net.*;
import org.sunspotworld.Logger;

public final class EasyBulb
{
  static DatagramSocket datagramSocket;
  static InetAddress address;
  static EasyBulb bulb;
  
  static
  {
    try
    {
      datagramSocket = new DatagramSocket(8899);
      address = InetAddress.getByName("192.168.0.255");
    }
    catch(IOException e)
    { e.printStackTrace(); }
  }
  
  public static void processString(String cmd)
  { 
    if(cmd != null)
    {
      try
      {
        String[] taskAndValue = cmd.split(" ");
        String task = taskAndValue[0];
        String value = "0";
        if(taskAndValue.length > 1)
          value = taskAndValue[1];

        if(task.equals("colour"))
        {
          datagramSocket.send(new DatagramPacket(new byte[]{0x4E, 0x19, 0x55}, 3, address, 8899)); // thing to do with changing clr?
          Utils.sleep(10);
          datagramSocket.send(new DatagramPacket(new byte[]{0x47, 0x00, 0x55}, 3, address, 8899)); // turn on
          Utils.sleep(10);
          datagramSocket.send(new DatagramPacket(new byte[]{0x40, (byte) Byte.parseByte(value), 0x55}, 3, address, 8899)); // change clr
        }

        else if(task.equals("on"))
          datagramSocket.send(new DatagramPacket(new byte[]{0x47, 0x00, 0x55}, 3, address, 8899));

        else if(task.equals("off"))
          datagramSocket.send(new DatagramPacket(new byte[]{0x48, 0x00, 0x55}, 3, address, 8899)); // turn off

        if(task.equals("brightness"))// range -127 to -99
          datagramSocket.send(new DatagramPacket(new byte[]{0x4E, (byte) Math.min(Byte.parseByte(value), (byte)-99), 0x55}, 3, address, 8899)); // turn off

        if(task.equals("white"))
          datagramSocket.send(new DatagramPacket(new byte[]{(byte)0xC7, 0x00, 0x55}, 3, address, 8899));
      }
      catch(IOException e)
      { Logger.log(Logger.ERROR, "Error while ssending EasyBuld command " + cmd, true); }
    }
  }
}