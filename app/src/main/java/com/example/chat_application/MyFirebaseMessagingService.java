package com.example.chat_application;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "chat_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Data: " + remoteMessage.getData().toString());

        if (!remoteMessage.getData().isEmpty()) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("body");
            String sender = remoteMessage.getData().get("sender");

            Log.d(TAG, "Data message received: " + message);

            if (isAppInForeground()) {
                Log.d(TAG, "App in foreground — marking as read");
                markMessageAsRead(sender);  // ✅ Add this line
            } else {
                showNotification(title != null ? title : "New Message", message != null ? message : "");
            }
        }
    }
    private void markMessageAsRead(String senderName) {
        if (senderName == null || senderName.equals("")) return;

        FirebaseFirestore.getInstance().collection("chats")
                .whereEqualTo("read", false)
                .whereEqualTo("name", senderName)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "Marked " + query.getDocuments().size() + " messages as read for " + senderName);

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        doc.getReference().update("read", true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark read", e));
    }


    // Helper method to check if app is in foreground
    private boolean isAppInForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }




    private void showNotification(String title, String message) {
        createNotificationChannel();

        // Retrieve saved username
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("name", null);

        Intent intent;
        if (savedName != null) {
            intent = new Intent(this, ChattingSpace.class);
            intent.putExtra("name", savedName);
        } else {
            // fallback to login screen
            intent = new Intent(this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);


        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify(1, builder.build());
        } catch (SecurityException e) {
            Log.e("NotificationError", "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chat Notifications";
            String description = "Notifications for new chat messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "FCM Token: " + token);
    }

}
