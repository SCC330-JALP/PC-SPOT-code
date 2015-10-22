package org.sunspotworld;

import com.firebase.client.*;
import java.util.concurrent.*;


public class SPOTDBcommunication{

    public static Firebase ref = new Firebase("https://sunsspot.firebaseio.com/");
    public Firebase logRef = null;
    public SPOTDBentry newEntry = null;

    public SPOTDBcommunication(String childName){
      logRef = ref.child(childName);
    }

    public void update(int zoneId, double temp, double light, long timestamp) throws InterruptedException{

        final CountDownLatch done = new CountDownLatch(1);

        newEntry = new SPOTDBentry(zoneId, temp, light, timestamp);

        logRef.push().setValue(newEntry, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                System.out.println("Successfully pushed to" + logRef.toString());
                done.countDown();
            }
        });
        done.await();
    }

    public void remove(final long rangeInMiliseconds){
        logRef.addValueEventListener(new ValueEventListener() {
              
              @Override
              public void onDataChange(DataSnapshot snapshot) {
                  // System.out.println("Total childs: " + snapshot.getChildrenCount());

                  for (DataSnapshot entrySnapshot: snapshot.getChildren()) {
                    SPOTDBentry entry = entrySnapshot.getValue(SPOTDBentry.class);

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
