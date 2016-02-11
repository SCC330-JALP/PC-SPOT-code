package org.sunspotworld;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import org.sunspotworld.firebase.FirebaseConnection;

/**
 * @author Povilas marcinkevicius
 * @version 1.1.3
 */
public final class Logger
{
  public static final int INFO = 0;
  public static final int WARN = 1;
  public static final int ERROR = 2;
  public static final int CRITICAL = 3;
  
  public static final String[] toString =
  {
    "INFO",
    "WARNING",
    "ERROR",
    "CRITICAL"
  };
  
  private static final String BRANCH_LOG = "log";
  
  private static final DefaultListModel<String> listModel = new DefaultListModel<String>();
  private static final JScrollPane scrollPane = new JScrollPane();
  private static final JList<String> logList = new JList<String>(listModel);
  private static final SimpleDateFormat date = new SimpleDateFormat("MMM dd HH:mm:ss");
  private static String baseMac = "";
  private static JFrame frame = new JFrame();
  private static int logStatus = INFO;
  
  private static class DataSet
  {
    private int lvl;
    private String msg;
    
    public DataSet(int lvl, String msg)
    {
      this.lvl = lvl;
      this.msg = msg;
    }

    public int getLvl()
    { return lvl; }

    public void setLvl(int lvl)
    { this.lvl = lvl; }
    
    public String getMsg()
    { return msg; }
    
    public void setMsg(String msg)
    { this.msg = msg; }
  }
  
  public static void log(int level, String message, boolean baseError)
  {
    if(level > CRITICAL)
      throw new IllegalArgumentException();
    if(level > logStatus)
    {
      logStatus = level;
      try
      { frame.setIconImage(ImageIO.read(new File(toString[level] + ".png"))); }
      catch(IOException e)
      { e.printStackTrace(); }
    }
    listModel.addElement(date.format(System.currentTimeMillis()) + " > " + toString[level] + ": " + (baseError ? baseMac : "") + message);
    FirebaseConnection.upload(new DataSet(level, (baseError ? baseMac : "") + message), FirebaseConnection.FIREBASE.child(BRANCH_LOG).child(String.valueOf(System.currentTimeMillis())), null);
  }

  public static void openWindow(String mac)
  {
    baseMac = "Base " + mac + ": ";
    try
    { frame.setIconImage(ImageIO.read(new File("INFO.png"))); }
    catch(IOException e)
    { e.printStackTrace(); }
    
    scrollPane.setViewportView(logList);
    scrollPane.setPreferredSize(new Dimension(200, 1000));
    frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
    frame.add(scrollPane);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("Zeus");
    frame.setSize(300,350);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
