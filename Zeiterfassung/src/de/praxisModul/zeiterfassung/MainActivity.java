package de.praxisModul.zeiterfassung;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, LocationListener {
	
	private Button myButton;
	private Button myButton2;
	TextView mDataTxt;
    //private LocationManager mLocationManager;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
		//Intent intent = new 
		setContentView(R.layout.activity_main);
		myButton = (Button) findViewById(R.id.btn_start);
		myButton.setOnClickListener(this);
		myButton2 = (Button) findViewById(R.id.btn_stop);
		myButton2.setOnClickListener(this);
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v == myButton) {
			setContentView(R.layout.activity_main);
			TextView tv = new TextView(this);
			tv.setText("Hello again");
			setContentView(tv);
		}
		else if (v == myButton2) {
			setContentView(R.layout.activity_main);
			TextView tv = new TextView(this);
			tv.setText("Hello again2");
			setContentView(tv);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		 StringBuilder dataStrBuilder = new StringBuilder();
	     dataStrBuilder.append(String.format("Latitude: %.3f,   Longitude%.3fn", location.getLatitude(), location.getLongitude()));
	     mDataTxt.setText(dataStrBuilder.toString());
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
}
