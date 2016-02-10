package org.sunspotworld.firebase;

import java.util.Date;
import java.util.HashMap;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.1
 * 
 * Used to upload new Spots in one go so no glitches appear on the front end
 * PSP LOC: 0
 */
public class FirebaseSpotData
{
  private Integer battery = 50;
  private Integer compass = 0;
  private Boolean infrared = false;
  private Boolean sound = false;
  private Double accel = 20.0;
  private Double light = 20.0;
  private Double temp = 20.0;
  private Boolean btn_l = false;
  private Boolean btn_r = false;
  private Boolean D2 = false;
  private Boolean D3 = false;
  private Double A2 = 1.0;
  private Double A3 = 1.0;
  private String name;
  private String storedData = "";
  private String liveData = "";
  private Boolean alive = true;
  
  public FirebaseSpotData()
  { this.name = "SPOT added at " + new Date(); }
  
  public Integer getBattery()
  { return battery; }

  public void setBattery(Integer battery)
  { this.battery = battery; }

  public Integer getCompass()
  { return compass; }

  public void setCompass(Integer compass)
  { this.compass = compass; }

  public Boolean getInfrared()
  { return infrared; }

  public void setInfrared(Boolean infrared)
  { this.infrared = infrared; }

  public Boolean getSound()
  { return sound; }

  public void setSound(Boolean sound)
  { this.sound = sound; }

  public Double getAccel()
  { return accel; }

  public void setAccel(Double accel)
  { this.accel = accel; }

  public Double getLight()
  { return light; }

  public void setLight(Double light)
  { this.light = light; }

  public Double getTemp()
  { return temp; }

  public void setTemp(Double temp)
  { this.temp = temp; }

  public Boolean getD2()
  { return D2; }

  public void setD2(Boolean D2)
  { this.D2 = D2; }

  public Boolean getD3()
  { return D3; }

  public void setD3(Boolean D3)
  { this.D3 = D3; }

  public Double getA2()
  { return A2; }

  public void setA2(Double A2)
  { this.A2 = A2; }

  public Double getA3()
  { return A3; }

  public void setA3(Double A3)
  { this.A3 = A3; }
  
  public String getName()
  { return name; }

  public void setName(String name)
  { this.name = name; }

  public Boolean getAlive()
  { return alive; }

  public void setAlive(Boolean alive)
  { this.alive = alive; }

  public String getStoredData()
  { return storedData; }

  public void setStoredData(String storedData)
  { this.storedData = storedData; }

  public String getLiveData()
  { return liveData; }

  public void setLiveData(String liveData)
  { this.liveData = liveData; }

  public Boolean getBtn_l()
  { return btn_l; }

  public void setBtn_l(Boolean btn_l)
  { this.btn_l = btn_l; }

  public Boolean getBtn_r()
  { return btn_r; }

  public void setBtn_r(Boolean btn_r)
  { this.btn_r = btn_r; }
}
