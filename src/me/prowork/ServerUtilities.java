/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.prowork;

import static me.prowork.Constants.SERVER_URL;
import static me.prowork.Constants.API_SERVER;
import static me.prowork.Constants.API_KEY;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();

    /**
     * Register this account/device pair within the server.
     *
     * @return whether the registration succeeded or not.
     */
    static boolean register(final Context context, final String regId) {
        String serverUrl = SERVER_URL + "register";
        Map<String, String> params = new HashMap<String, String>();
        params.put("reg_id", regId);
        params.put("token", ProworkNotificationsActivity.token);
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);

        // Register with server. Retry a couple of times if failed
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            try {
                post(serverUrl, params);
                GCMRegistrar.setRegisteredOnServer(context, true);
                
                return true;
            } catch (IOException e) {
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Thread.currentThread().interrupt();
                    return false;
                }
                
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     */
    static void unregister(final Context context, final String regId) {
        String serverUrl = SERVER_URL + "unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put("reg_id", regId);
        params.put("token", ProworkNotificationsActivity.token);
        try {
            GCMRegistrar.setRegisteredOnServer(context, false);
            post(serverUrl, params);
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
        }
    }

    /**
     * Login user
     */
    static void login (final String email, final String password, ResponseCallback callback) {
        String serverUrl = API_SERVER + "session/get";
        Map<String, String> params = new HashMap<String, String>();
        params.put("api_key", API_KEY);
        params.put("email", email);
        params.put("password", password);
        try {
            post(serverUrl, params, callback);
        } catch (IOException e) {
        	//Log.d("URL error", e.getMessage());
        	callback.handleResponse(0, e.getMessage());
        }
    }

    private static void post(String endpoint, Map<String, String> params) throws IOException {
    	post(endpoint, params, new ResponseCallback() {
			@Override
			public void handleResponse(int responseCode, String data) {
				// check if it is a 410 error and relogin
				// And save new token too
			}
		});
    }
      
    private static void post(String endpoint, Map<String, String> params, ResponseCallback callback)
            throws IOException {
        URL url = new URL(endpoint);
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        //Log.d("URL", body);

        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            Log.d("URL", "posted set to "+endpoint);
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();

            StringBuffer response = new StringBuffer();
			InputStream is = null;
            try {
	            is = conn.getInputStream();
            }
            catch (IOException e) {
            	//e.printStackTrace();
				// Hack for 4xx http headers
	            is = conn.getErrorStream();
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			while((line = rd.readLine()) != null) {
			  response.append(line);
			}
			rd.close();	
            
            int status = conn.getResponseCode();
            //Log.d("URL", status+" "+response.toString());
            callback.handleResponse(status, response.toString());

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
      }
}

