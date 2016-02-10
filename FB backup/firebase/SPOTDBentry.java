package org.sunspotworld.firebase;

import com.firebase.client.*;

/**
 * @author Liam Cottier
 * Adjusted a bit by Povilas Marcinkevicius
 */
public class SPOTDBentry{

    private long trigger;
    private String task;
    private String name;
    private long zone;

    public SPOTDBentry() {}

    public SPOTDBentry(String name, String task, long zone)
    {
        this.name = name;
        this.task = task;
        this.zone = zone;
    }
    
    public String getName(){
        return name;
    }

    public String getTask(){
        return task;
    }
    
    public long getTrigger(){
        return trigger;
    }
    
    public long getZone(){
      return zone;
    }
    
    public void setName(String name){
        this.name = name;
    }

    public void setTask(String task){
        this.task = task;
    }
    
    public void setZone(long zone){
      this.zone = zone;
    }
    
    public void setTrigger(long trigger){
      this.trigger = trigger;
    }
}