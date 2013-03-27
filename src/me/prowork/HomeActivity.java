package me.prowork;

import com.google.android.gcm.GCMRegistrar;

import static me.prowork.Constants.SENDER_ID;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends Activity {
	
	AsyncTask<Void, Void, Void> mRegisterTask;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		// Layout
		setContentView(R.layout.home);
		TextView _name = (TextView) findViewById(R.id.username);
		_name.setText(ProworkNotificationsActivity.name);
		
		// Notification for just logged in
		if (getIntent().getBooleanExtra("just", false)) {
			AlertDialog.Builder builder = 
					new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.notification_alert)).create().show();
		}
		
		// Is he registered? No? Do.
		final String regId = GCMRegistrar.getRegistrationId(this);
		GCMRegistrar.register(this, SENDER_ID);
        if (regId.equals("")) {
            GCMRegistrar.register(this, SENDER_ID);
        } else {
        	if (!GCMRegistrar.isRegisteredOnServer(this)) {
                final Context context = this;
                mRegisterTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        boolean registered =
                                ServerUtilities.register(context, regId);
                        if (!registered) {
                            GCMRegistrar.unregister(context);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }

                };
                mRegisterTask.execute(null, null, null);        		
        	}
        }
		
		// Logout
		final Button logoutButton = (Button) findViewById(R.id.logout_button);
		logoutButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Logout
				GCMRegistrar.unregister(HomeActivity.this);
				ProworkNotificationsActivity.saveCreds(true);
				finish();
			}
		});
	}
	
    @Override
    protected void onDestroy() {
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        
        try {
        	GCMRegistrar.onDestroy(this);
        }
        catch(IllegalArgumentException e){}
        
        super.onDestroy();
    }
}