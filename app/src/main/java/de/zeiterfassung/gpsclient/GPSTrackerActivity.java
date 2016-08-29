package de.zeiterfassung.gpsclient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.Date;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;

import java.net.MalformedURLException;
import java.net.URL;




public class GPSTrackerActivity extends Activity implements LocationListener {

    private final static String CONNECTIVITY = "android.net.conn.CONNECTIVITY_CHANGE";

    private LocationManager locationManager;
    private ConnectivityManager connectivityManager;

    SharedPreferences preferences;
    private EditText edit_url;
    private TextView text_gps_status;
    private TextView text_network_status;
    private ToggleButton button_toggle;
    private TextView text_running_since;
    private TextView last_server_response;

    private TextView text_login_status;

    private TextView username_field;
    private TextView password_field;
    private Button login_buton;

    public static final int CONNECTION_TIMEOUT=10000;
    public static final int READ_TIMEOUT=15000;
    private String logged_in_username;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GPSTrackerService.NOTIFICATION)) {
                String extra = intent.getStringExtra(GPSTrackerService.NOTIFICATION);
                if (extra != null) {
                    updateServerResponse();
                } else {
                    updateServiceStatus();
                }
            }
            if (action.equals(CONNECTIVITY)) {
                updateNetworkStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_hosted_gpstracker);

        edit_url = (EditText)findViewById(R.id.edit_url);
        text_gps_status = (TextView)findViewById(R.id.text_gps_status);
        text_network_status = (TextView)findViewById(R.id.text_network_status);
        button_toggle = (ToggleButton)findViewById(R.id.button_toggle);
        text_running_since = (TextView)findViewById(R.id.text_running_since);
        last_server_response = (TextView)findViewById(R.id.last_server_response);

        username_field = (TextView)findViewById(R.id.editText);
        password_field = (TextView)findViewById(R.id.editText2);
        login_buton =  (Button)findViewById(R.id.button);
        text_login_status = (TextView)findViewById(R.id.text_login_status);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("URL", "http://zeiterfassung.globalmediadesigns.de/gps/gps.php");
        editor.commit();

        if (preferences.contains("URL") && ! preferences.getString("URL", "").equals("")) {
            edit_url.setText(preferences.getString("URL", getString(R.string.hint_url)));
            edit_url.clearFocus();
        } else {
            edit_url.requestFocus();
        }

        edit_url.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("URL", s.toString().trim());
                editor.commit();
            }
        });


        registerReceiver(receiver, new IntentFilter(GPSTrackerService.NOTIFICATION));
        registerReceiver(receiver, new IntentFilter(GPSTrackerActivity.CONNECTIVITY));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        int pref_gps_updates = Integer.parseInt(preferences.getString("pref_gps_updates", "30")); // seconds
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, pref_gps_updates * 1000, 1, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            onProviderDisabled(LocationManager.GPS_PROVIDER);
        }

        updateNetworkStatus();

        updateServiceStatus();

        updateServerResponse();

        if (GPSTrackerService.isRunning) {
            edit_url.setEnabled(false);
        } else {
            edit_url.setEnabled(true);
        }



    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_self_hosted_gpstracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                i = new Intent(this, GPSTrackerPrefs.class);
                startActivity(i);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public void onToggleClicked(View view) {
        String logged_in_user = preferences.getString("logged_in_user", "error");
        if (!logged_in_user.equals("error") && !logged_in_user.equals("")) {
            Intent intent = new Intent(this, GPSTrackerService.class);
            if (((ToggleButton) view).isChecked()) {
                startService(intent);
                edit_url.setEnabled(false);
            } else {
                stopService(intent);
                edit_url.setEnabled(true);
            }
        }else{
            Toast.makeText(GPSTrackerActivity.this, "Nicht Eigenloggt, bitte einloggen", Toast.LENGTH_LONG).show();
            button_toggle.setChecked(false);
        }

    }

    public void onLoginButtonClicked(View view) {

        // Get text from email and passord field
        final String email = username_field.getText().toString();
        final String password = password_field.getText().toString();

        // Initialize  AsyncLogin() class with email and password
        new AsyncLogin().execute(email,password);


    }

    private class AsyncLogin extends AsyncTask<String, String, String>
    {
        ProgressDialog pdLoading = new ProgressDialog(GPSTrackerActivity.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tLoading...");
            pdLoading.setCancelable(false);
            pdLoading.show();

        }
        @Override
        protected String doInBackground(String... params) {
            try {

                // Enter URL address where your php file resides
                url = new URL("http://zeiterfassung.globalmediadesigns.de/gps/index.php");

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("username", params[0])
                        .appendQueryParameter("password", params[1]);
                String query = builder.build().getEncodedQuery();

                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("logged_in_user", params[0]);
                    editor.commit();

                    // Pass data to onPostExecute method
                    return(result.toString());

                }else{

                    return("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread

            pdLoading.dismiss();

            if(result.equalsIgnoreCase("true"))
            {
                Toast.makeText(GPSTrackerActivity.this, "Eingeloggt.", Toast.LENGTH_LONG).show();
                text_login_status.setText(username_field.getText());

            }else if (result.equalsIgnoreCase("false")){

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("logged_in_user", "");
                editor.commit();

                // If username and password does not match display a error message
                Toast.makeText(GPSTrackerActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();

            } else if (result.equalsIgnoreCase("exception") || result.equalsIgnoreCase("unsuccessful")) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("logged_in_user", "");
                editor.commit();
                Toast.makeText(GPSTrackerActivity.this, "OOPs! Something went wrong. Connection Problem.", Toast.LENGTH_LONG).show();

            }else{
                Toast.makeText(GPSTrackerActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
            }
        }

    }

    /* -------------- GPS stuff -------------- */

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        text_gps_status.setText(getString(R.string.text_gps_status_disabled));
        text_gps_status.setTextColor(Color.RED);
    }

    @Override
    public void onProviderEnabled(String provider) {
        text_gps_status.setText(getString(R.string.text_gps_status_enabled));
        text_gps_status.setTextColor(Color.BLACK);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /* ----------- utility methods -------------- */
    private void updateServiceStatus() {

        if (GPSTrackerService.isRunning) {
            Toast.makeText(this, getString(R.string.toast_service_running), Toast.LENGTH_SHORT).show();
            button_toggle.setChecked(true);
            text_running_since.setText(getString(R.string.text_running_since) + " "
                    + DateFormat.getDateTimeInstance().format(GPSTrackerService.runningSince.getTime()));
        } else {
            Toast.makeText(this, getString(R.string.toast_service_stopped), Toast.LENGTH_SHORT).show();
            button_toggle.setChecked(false);
            if (preferences.contains("stoppedOn")) {
                long stoppedOn = preferences.getLong("stoppedOn", 0);
                if (stoppedOn > 0) {
                    text_running_since.setText(getString(R.string.text_stopped_on) + " "
                            + DateFormat.getDateTimeInstance().format(new Date(stoppedOn)));
                } else {
                    text_running_since.setText(getText(R.string.text_killed));
                }
            }
        }
    }

    private void updateNetworkStatus() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            text_network_status.setText(getString(R.string.text_network_status_enabled));
            text_network_status.setTextColor(Color.BLACK);
        } else {
            text_network_status.setText(getString(R.string.text_network_status_disabled));
            text_network_status.setTextColor(Color.RED);
        }
    }

    private void updateServerResponse() {
        if (GPSTrackerService.lastServerResponse != null) {
            last_server_response.setText(
                    Html.fromHtml(GPSTrackerService.lastServerResponse)
            );
        }
    }
}
