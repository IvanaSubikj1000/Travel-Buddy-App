package com.travelbuddy;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.travelbuddy.sync.SyncManager;

public class TravelBuddyApp extends Application {

    public static final String NOTIFICATION_CHANNEL_ID = "travelbuddy_general";
    private static final String TAG = "TravelBuddyApp";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> Log.d(TAG, "FCM token: " + token));

        FirebaseMessaging.getInstance().subscribeToTopic("general")
                .addOnSuccessListener(unused -> Log.d(TAG, "Subscribed to FCM topic: general"));

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }
}