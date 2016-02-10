package org.sunspotworld.firebase;

import com.firebase.client.DataSnapshot;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.2
 * 
 * Only used to pass functionality to Firebase listener
 * PSP LOC: 0
 */
public interface OnSpotUpdate
{
  public void onUpdate(DataSnapshot data, String spot, String name, Class clazz);
}
