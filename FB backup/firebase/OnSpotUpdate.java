package org.sunspotworld.firebase;

import java.io.DataInputStream;

/**
 * @author Povilas Marcinkevicius
 * @version 1.0.1
 * 
 * Only used to pass functionality to Firebase listener
 */
public interface OnSpotUpdate
{
  public void onUpdate(SPOTDBentry spot, int variation, DataInputStream stream, String address);
}
