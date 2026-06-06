package com.travelbuddy;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.travelbuddy.sync.SyncManager;

public class TravelBuddyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Drive SyncManager lifecycle from auth state.
        // addAuthStateListener fires immediately with the current state, so
        // sessions that survive app restart are caught here without extra wiring.
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            SyncManager syncManager = SyncManager.getInstance(this);
            if (user != null) {
                syncManager.startListening(user.getUid());
            } else {
                syncManager.stopListening();
            }
        });
    }
}
