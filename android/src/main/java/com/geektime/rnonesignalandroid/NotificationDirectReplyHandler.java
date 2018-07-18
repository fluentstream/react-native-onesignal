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


/**
 * Created by zrunyan on 5/9/18.
 */

public class NotificationDirectReplyHandler extends NotificationExtenderService {

    protected boolean onNotificationProcessing(final OSNotificationReceivedResult receivedResult) {

        // Read properties from result.
        String notificationType = "";
        String contactFrom = "";


        try {
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
