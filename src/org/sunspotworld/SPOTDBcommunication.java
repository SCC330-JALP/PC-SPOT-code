package org.sunspotworld;

import com.firebase.client.*;
import java.util.concurrent.*;


public class SPOTDBcommunication{

    public static Firebase ref = new Firebase("https://sunsspot.firebaseio.com/");
    public Firebase logRef = null;
    public SpotDbZoneEntry newEntry = null;

    public SPOTDBcommunication(String childName){
      logRef = ref.child(childName);
    }

    public void updateZoneData(int zoneId, double temp, double light, long timestamp) throws InterruptedException{

        final CountDownLatch done = new CountDownLatch(1);

        newEntry = new SpotDbZoneEntry(zoneId, temp, light, timestamp);

        logRef.push().setValue(newEntry, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                System.out.println("Successfully pushed to" + logRef.toString());
                done.countDown();
            }
        });
        done.await();
    }

    public void removeOldZoneEntries(final long rangeInMiliseconds){
        logRef.addValueEventListener(new ValueEventListener() {
              
              @Override
              public void onDataChange(DataSnapshot snapshot) {
                  // System.out.println("Total childs: " + snapshot.getChildrenCount());

                  for (DataSnapshot entrySnapshot: snapshot.getChildren()) {
                    SpotDbZoneEntry entry = entrySnapshot.getValue(SpotDbZoneEntry.class);

                    if(entry.getTimestamp() < (System.currentTimeMillis() - rangeInMiliseconds) )
                        entrySnapshot.getRef().removeValue();
                  }
              }

              @Override
              public void onCancelled(FirebaseError firebaseError) {
                  System.out.println("The read failed: " + firebaseError.getMessage());
              }
          });
    }
}
