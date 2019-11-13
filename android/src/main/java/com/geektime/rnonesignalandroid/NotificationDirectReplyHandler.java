package com.geektime.rnonesignalandroid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.R.drawable;

import com.onesignal.OSNotificationDisplayedResult;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;

import java.math.BigInteger;
import android.app.NotificationManager;
import android.app.Service;
import android.provider.Settings;
import android.net.Uri;
import android.os.PowerManager;
import android.app.KeyguardManager;

/**
 * Created by zrunyan on 5/9/18.
 */

public class NotificationDirectReplyHandler extends NotificationExtenderService {

    protected boolean onNotificationProcessing(final OSNotificationReceivedResult receivedResult) {

        // Read properties from result.
        String notificationType = "";
        String contactFrom = "";
        String silentNotification = "";
        String silentNotificationCallerDescription = "";

        try {
            silentNotification = receivedResult.payload.additionalData.get("hidden").toString();
            if (receivedResult.payload.additionalData.opt("caller") != null) {
                silentNotificationCallerDescription = receivedResult.payload.additionalData.opt("caller").toString();
            }

            if (silentNotification.equals("true")) {

                Context context;
                context = getApplicationContext();

                // do anything here you like before running the app

                // create pending intent
                Intent dialogIntent = new Intent();
                dialogIntent.setClassName("com.fluentcloud", "com.fluentcloud.MainActivity");
                dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, dialogIntent, 0);

                // accuire an wake lock
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock;
                wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WakeLock");
                wakeLock.acquire(30000);

                // check if screen is locked
                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                Boolean isScreenLocked = false;
                if(myKM.isKeyguardLocked() || myKM.isDeviceLocked()) {
                    isScreenLocked = true;
                }

                 NotificationManager notificationManager = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);

                if (isScreenLocked) {
                    // if screen is locked then always show a notification
                    NotificationCompat.Builder lockedScreenNotification = new NotificationCompat.Builder(context, "fcm_silent_notification_channel")
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle("Incoming call. Open the app to answer.")
                    .setContentText(silentNotificationCallerDescription)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setFullScreenIntent(pendingIntent, true);
                    notificationManager.notify(358369, lockedScreenNotification.build());
                }

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                  // run the app if its on Android P or less
                  pendingIntent.send();
                } else {
                    // check if we have overlay permission in Android O(or later)
                    if (!Settings.canDrawOverlays(this)) {
                        // if no, show it as a push notification (if screen not locked)
                        if (!isScreenLocked) {
                            NotificationCompat.Builder noti = new NotificationCompat.Builder(context, "fcm_silent_notification_channel")
                            .setSmallIcon(android.R.drawable.stat_notify_chat)
                            .setContentTitle("Incoming call. Open the app to answer.")
                            .setContentText(silentNotificationCallerDescription)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setFullScreenIntent(pendingIntent, true);

                            notificationManager.notify(358369, noti.build());
                        }
                    } else {
                        // if yes, open the app
                       pendingIntent.send();
                    }
                }

                // dont show the notification
                return true;
            }

            notificationType = receivedResult.payload.additionalData.get("notificationType").toString();
            contactFrom = receivedResult.payload.additionalData.get("from").toString();
        } catch (Throwable t) {
            Log.d("Custom Direct Reply", "notificationType key is nonexistent from notification passed to NotificationExtenderService: " + receivedResult.payload.toJSONObject().toString());
            return false;
        }

        if (!notificationType.equals("sms")) {

            return false;
        }

        final OverrideSettings overrideSettings = new OverrideSettings();
        final String finalContactFrom = contactFrom;
        overrideSettings.extender = new NotificationCompat.Extender() {

            public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {

                // Sets the background notification color to Green on Android 5.0+ devices.
                builder.setColor(new BigInteger("3E5BB9", 16).intValue());
                builder.setSmallIcon(android.R.drawable.stat_notify_chat);

                String replyLabel = "Enter your reply here";
                String KEY_REPLY = "key_reply";

                //Initialise RemoteInput
                RemoteInput remoteInput = new RemoteInput.Builder(KEY_REPLY)
                        .setLabel(replyLabel)
                        .build();

                Intent newIntent;
                newIntent = new Intent().setAction("com.onesignal.NotificationExtender");

                //PendingIntent that restarts the current activity instance.
                PendingIntent replyPendingIntent = getReplyPendingIntent(newIntent);



                //Notification Action with RemoteInput instance added.
                NotificationCompat.Action replyAction =
                        new NotificationCompat.Action.Builder(drawable.ic_menu_send,
                                "Reply", replyPendingIntent)
                                .addRemoteInput(remoteInput)
                                .build();

                //Notification.Action instance added to Notification Builder.
                builder.addAction(replyAction);

                return builder;
            }

            private CharSequence getReplyMessage(Intent intent) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null) {

                    return remoteInput.getCharSequence("key_reply");
                }

                return null;
            }

            private PendingIntent getReplyPendingIntent(Intent intent) {

                Context context;
                context = getApplicationContext();

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {


                    // start your activity for Android M and below
                    intent = new Intent().setAction("com.onesignal.NotificationExtender");
                    intent.setAction("direct_reply");
                    intent.putExtra("message_id", receivedResult.payload.notificationID);
                    intent.putExtra("notification_id", receivedResult.payload.notificationID.hashCode());
                    intent.putExtra("contact_from", finalContactFrom);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


                    return PendingIntent.getActivity(context, 100, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                } else {

                    // start a
                    // (i)  broadcast receiver which runs on the UI thread or
                    // (ii) service for a background task to b executed , but for the purpose of
                    // this codelab, will be doing a broadcast receiver
                    intent = new Intent().setAction("com.onesignal.NotificationExtender");
                    intent.setAction("direct_reply");
                    intent.putExtra("notification_id", receivedResult.payload.notificationID);
                    intent.putExtra("message_id", receivedResult.payload.notificationID.hashCode());
                    intent.putExtra("contact_from", finalContactFrom);

                    return PendingIntent.getBroadcast(getApplicationContext(), 100, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }
            }
        };

        overrideSettings.androidNotificationId = receivedResult.payload.notificationID.hashCode();

        OSNotificationDisplayedResult displayedResult = displayNotification(overrideSettings);

        return false;
    }
}
