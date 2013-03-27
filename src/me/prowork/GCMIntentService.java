package me.prowork;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import static me.prowork.Constants.SENDER_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
    	Log.i(TAG, "Device registered: regId = " + registrationId);
        ServerUtilities.register(context, registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
    	Log.i(TAG, "Device unregistered");
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            ServerUtilities.unregister(context, registrationId);
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
    	Log.i(TAG, "Received message");
		// Extract the message and title params
		// as sent from our proxy server
        String message = intent.getExtras().getString("message");
        String title = intent.getExtras().getString("title");

        generateNotification(context, title, message);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
    	Log.i(TAG, "Received recoverable error: " + errorId);
        return super.onRecoverableError(context, errorId);
    }

	@Override
	protected void onError(Context arg0, String arg1) {
		// TODO Auto-generated method stub
	}
	
    private static void generateNotification(Context context, String title, String message) {
        int icon = R.drawable.ic_notification;
        long when = System.currentTimeMillis();
        
		// Show notification
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification notification = new Notification(icon, message, when);
        
        Intent notificationIntent = new Intent(context, ProworkNotificationsActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
}