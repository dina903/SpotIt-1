//Dina Najeeb
package com.example.spotit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.example.spotit.db.accelDataSource;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends Activity {

	private static final String LOGTAG = "SPOTIT";
	float xTotal, yTotal, zTotal;
	float windowTotal = 0;
	float time;
	float [] threeMeans;
	File fileMean;
	SensorManager mgr;
	Sensor accel;
	accelDataSource datasource;
	ImageView imgViewR, imgViewL, imgViewP;
	TextView tv1, tv2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		threeMeans = new float[3];
		
		imgViewL = (ImageView) findViewById(R.id.Left);
		imgViewR = (ImageView) findViewById(R.id.Right);
		imgViewP = (ImageView) findViewById(R.id.purse);
		tv2 = (TextView) findViewById(R.id.textView1);
		tv1 = (TextView) findViewById(R.id.textView2);
		
		File exportDir = new File(Environment.getExternalStorageDirectory(), "");
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		fileMean = new File(exportDir, "MeansAccel.csv");

		xTotal = 0;
		yTotal = 0;
		zTotal = 0;
		time = 0;

		mgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		datasource = new accelDataSource(this);

		// check accelerometer
		if (accel == null) {
			Log.i(LOGTAG, "No Sensor Found.");
		} else {
			Log.i(LOGTAG, "Sensor Found.");
			mgr.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*** accelerometer sensor ***/

	SensorEventListener listener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent evt) {
			float x = evt.values[0];
			float y = evt.values[1];
			float z = evt.values[2];
			time += 0.06; // rate 60000 microsecond = 0.06 seconds
			highPass(x, y, z); // filter the data
		}

		private void highPass(float x, float y, float z) {
			float[] filteredValues = new float[3];
			float[] gravity = new float[3];
			float[] meanVals = new float[3];
			final float ALPHA = (float) 0.8;

			gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
			gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
			gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

			filteredValues[0] = x - gravity[0];
			filteredValues[1] = y - gravity[1];
			filteredValues[2] = z - gravity[2];
			datasource.create(filteredValues[0], filteredValues[1],
					filteredValues[2]);

			xTotal += filteredValues[0];
			yTotal += filteredValues[1];
			zTotal += filteredValues[2];

			windowTotal++;
			if (windowTotal == 20) {//find the mean for each 10 readings
				meanVals = means(xTotal, yTotal, zTotal, windowTotal);
				try {
					fileMean.createNewFile();
					CSVWriter csvWrite = new CSVWriter(new FileWriter(fileMean,
							true));
					String arrStr[] = { String.valueOf(meanVals[0]),
							String.valueOf(meanVals[1]),
							String.valueOf(meanVals[2]), String.valueOf(time) };
					csvWrite.writeNext(arrStr);
					csvWrite.close();
				} catch (IOException e) {
					Log.i(LOGTAG, "File Error");
				}
				windowTotal = 0;
				xTotal = 0;
				yTotal = 0;
				zTotal = 0;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	/*************** find mean values ********************/
	public float[] means(float X_Total, float Y_Total, float Z_Total,
			float total) {
		float[] meanValues = new float[3];

		meanValues[0] = X_Total / total;
		meanValues[1] = Y_Total / total;
		meanValues[2] = Z_Total / total;
		recognizeFeature(meanValues);
		threeMeans[0] = meanValues[0];
		threeMeans[1] = meanValues[1];
		threeMeans[2] = meanValues[2];
				
		return meanValues;
	}
	/********** Recognize Positions ************/
	
	public void recognizeFeature(float[] m) {
		if (m[1] <= -5 && m[1] >= -7) { //Jacket Pocket - Standing
			if (m[2] < 0) {
				if (m[0] >= 2 && m[0] < 3) { //right x pos z neg
					imgViewR.setVisibility(View.VISIBLE);
					tv2.setText("Right Pocket\nScreen To User\nHead First");
				} else if(m[0] >= -4.5 && m[0] < -3){//left x neg z neg
					imgViewL.setVisibility(View.VISIBLE);
					tv2.setText("Left Pocket\nScreen To User\nHead First");
				}
			} else if (m[2] > 0) {
				if(m[0] >= 3 && m[0] <= 4){//left x pos z pos
					imgViewL.setVisibility(View.VISIBLE);
					tv2.setText("Left Pocket\nScreen away from User\nHead First");
				} else if (m[0] >= -3 && m[0] <= -2.5) { //right x neg z pos
					imgViewR.setVisibility(View.VISIBLE);
					tv2.setText("Right Pocket\nScreen away from User\nHead First");
				}
			}
		} else if ((m[1] <= 2.5 && m[1] >= 0.5) && (m[0] >= 5.5 && m[0] <= 6.5) ){ //purse
			tv2.setText("In Purse\nScreen to User\nHorizontal");
			imgViewP.setVisibility(View.VISIBLE);
		} else if ((m[1] <= -0.5 && m[1] >= -2) && (m[0] >= -0.5 && m[0] <= -1.5)){
			tv2.setText("In Purse\nScreen away from User\nHorizontal");
			imgViewP.setVisibility(View.VISIBLE);
		}

	}

	/***** others ******/

	public void onConfident(View v) {
		Log.i(LOGTAG, "Checking accuracy");
		datasource.findAll(); //will have the confidence intervals
		if ((threeMeans[0] >= datasource.findAll()[0] && threeMeans[0] <= datasource.findAll()[1])
				&& (threeMeans[1] >= datasource.findAll()[2] && threeMeans[1] <= datasource.findAll()[3]) 
				&&(threeMeans[2] >= datasource.findAll()[4] && threeMeans[2] <= datasource.findAll()[5])) {
			tv1.setText("95% confident");
		} else {
			tv1.setText("Less than 95% confident");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(LOGTAG, "app stopped");
		mgr.unregisterListener(listener);
		datasource.findAll();
		datasource.close();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(LOGTAG, "app started");
		datasource.open();
		datasource.removeAll();
		// check if the output files already exists
		if (fileMean.exists()) {
			fileMean.delete();
			Log.i(LOGTAG, "Delete existing file");
		}
	}
}