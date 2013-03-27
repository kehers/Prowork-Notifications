package me.prowork;

import org.json.JSONException;
import org.json.JSONObject;

import static me.prowork.Constants.RECOVER_URL;
import static me.prowork.Constants.SIGNUP_URL;

import com.google.android.gcm.GCMRegistrar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ProworkNotificationsActivity extends Activity {
	
	static String name, email, password, token;
	static int userId;
	static SharedPreferences db;

	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check login status
        db = getPreferences(MODE_PRIVATE);
        
		name = db.getString("name", "");
		email = db.getString("email", "");
		password = db.getString("password", "");
		token = db.getString("token", "");
		userId = db.getInt("user_id", 0);
		
		try {
	        GCMRegistrar.checkDevice(this);
	        GCMRegistrar.checkManifest(this);
		}
		catch (UnsupportedOperationException u){
			Log.d("Error", "Unsupported device");
			AlertDialog.Builder builder = 
					new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.unsupported_alert)).create().show();
			return;
		}

        // Logged in? 
		if (email.equals("") || password.equals("")) {
	        setContentView(R.layout.login);
	        
			// Nope
	        final EditText emailText = (EditText) findViewById(R.id.email);
	        final EditText passwordText = (EditText) findViewById(R.id.password);
	        
	        final Button loginButton = (Button) findViewById(R.id.login_button);
	        loginButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// Get email and password
					email = emailText.getText().toString();
					password = passwordText.getText().toString();
					
					InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); 
			        inputManager.hideSoftInputFromWindow(passwordText.getWindowToken(), 
			        		InputMethodManager.HIDE_NOT_ALWAYS); 

					// Login
					final ProgressDialog dialog = ProgressDialog.show(ProworkNotificationsActivity.this, "", 
	                        getString(R.string.login_wait), true);
					dialog.setCancelable(true);

				    new Thread(new Runnable() {
				        public void run() {
							ServerUtilities.login(email, password, new ResponseCallback() {
								@Override
								public void handleResponse(final int responseCode, final String data) {

									 findViewById(R.id.login_layout).post(new Runnable() {
						                public void run() {
											dialog.dismiss();
											String error = null;
											
											try {
												JSONObject json = new JSONObject(data);
												
												if (responseCode == 200) {
													name = json.getString("name");
													token = json.getString("token");
													userId = json.getInt("user_id");
													
													saveCreds(false);
				
													// Start Logged in intent
													Intent intent = new Intent(ProworkNotificationsActivity.this,
															HomeActivity.class);
													intent.putExtra("just", true);
													startActivity(intent);
													
													finish();
												}
												else {
													error = json.getString("error");
												}
											} catch (JSONException e) {
												error = getString(R.string.internal_error);
											}
											
											if (error != null) {
												AlertDialog.Builder builder = 
														new AlertDialog.Builder(ProworkNotificationsActivity.this);
												builder.setMessage(error).create().show();
											}
											
						                }
						            });
								}
							});
				        }
				    }).start();
				}
			});
		}
		else {
			// Already logged in
			// Show logged in page
			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
			finish();
		}
    }
    
    public void recoverClicked(View v) {
    	handleClick(RECOVER_URL);
    }
    
    public void signupClicked(View v) {
    	handleClick(SIGNUP_URL);
    }
    
    private void handleClick (String url) {
    	 Uri uri = Uri.parse(url);
    	 Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    	 startActivity(intent);
    }
    
    static public void saveCreds(Boolean reset) {
    	if (reset) {
			userId = 0;
			name = "";
			email = "";
			password = "";
			token = "";
    	}
    	
		// Save credentials
		SharedPreferences.Editor editor = db.edit();
		editor.putString("name", name);
		editor.putString("email", email);
		editor.putString("password", password);
		editor.putString("token", token);
		editor.putInt("user_id", userId);
		editor.commit();
    }
}