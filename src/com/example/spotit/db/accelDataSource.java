package com.example.spotit.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import au.com.bytecode.opencsv.CSVWriter;

public class accelDataSource {

	private static final String LOGTAG = "SPOTIT";

	SQLiteOpenHelper dbhelper;
	SQLiteDatabase database;

	private static final String[] allColumns = { accelDBOpenHelper.COLUMN_ID,
			accelDBOpenHelper.COLUMN_X, accelDBOpenHelper.COLUMN_Y,
			accelDBOpenHelper.COLUMN_Z };

	public accelDataSource(Context context) { // constructor
		dbhelper = new accelDBOpenHelper(context);
	}

	public void open() {
		Log.i(LOGTAG, "Database opened");
		database = dbhelper.getWritableDatabase(); // open the db connection
	}

	public void close() {
		Log.i(LOGTAG, "Database closed");
		dbhelper.close(); // close the database
	}

	public void create(float x, float y, float z) {

		ContentValues values = new ContentValues();

		values.put(accelDBOpenHelper.COLUMN_X, x);
		values.put(accelDBOpenHelper.COLUMN_Y, y);
		values.put(accelDBOpenHelper.COLUMN_Z, z);
		database.insert(accelDBOpenHelper.TABLE_NAME, null, values);
	}

	public void removeAll() {
		Log.i(LOGTAG, "Deleting Table");
		open();
		database.delete(accelDBOpenHelper.TABLE_NAME, null, null);
	}

	/**** write database to a spread sheet and calculate the mean values********/
	public float[] findAll() { // returns array of mean values in the .csv file
		float xT = 0;
		float yT = 0;
		float zT = 0;
		float t = 0;
		float[] accelMeans = new float[3];
		float [] confIntervals = new float[6];
		
		Cursor cursor = database.query(accelDBOpenHelper.TABLE_NAME,
				allColumns, null, null, null, null, null);

		Log.i(LOGTAG, "Returned: " + cursor.getCount() + " rows");

		File exportDir = new File(Environment.getExternalStorageDirectory(), "");
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}

		File file = new File(exportDir, "RawAccel.csv");
		try {
			file.createNewFile();
			CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
			Cursor curCSV = database.rawQuery("SELECT * FROM accelData", null);

			csvWrite.writeNext(curCSV.getColumnNames());
			
			while (curCSV.moveToNext()) {
				String arrStr[] = { curCSV.getString(0), curCSV.getString(1),
						curCSV.getString(2), curCSV.getString(3)};
				xT += Float.parseFloat(curCSV.getString(1));
				yT += Float.parseFloat(curCSV.getString(2));
				zT += Float.parseFloat(curCSV.getString(3));
				t =  Float.parseFloat(curCSV.getString(0));
				csvWrite.writeNext(arrStr);
			}
			accelMeans[0] = xT/t;
			accelMeans[1] = yT/t;
			accelMeans[2] = zT/t;
			confIntervals = calcStndrdDev(accelMeans);

			csvWrite.close();
			curCSV.close();
		} catch (SQLException sqlEx) {
			Log.i(LOGTAG, "SQL error");
		}

		catch (IOException e) {
			Log.i(LOGTAG, "SQL error");
		}
		return confIntervals;
	}
	
	/***** this take the mean values and find the confidence intervals***********/
	public float[] calcStndrdDev(float[] meanVal) {
		float sqrdiffX = 0;
		float sqrdiffY = 0;
		float sqrdiffZ = 0;

		float total = 0;
		float varianceX = 0;
		float varianceY = 0;
		float varianceZ = 0;

		float mrgnErrX = 0;
		float stndrdErrX = 0;
		
		float mrgnErrY = 0;
		float stndrdErrY = 0;
		
		float mrgnErrZ = 0;
		float stndrdErrZ = 0;

		float[] stndrdDev = new float[3];
		float[] confIntervals = new float[6];

		Cursor curCSV = database.rawQuery("SELECT * FROM accelData", null);

		while (curCSV.moveToNext()) {
			sqrdiffX = (float) Math.pow(
					(Float.parseFloat(curCSV.getString(1)) - meanVal[0]), 2);
			varianceX += sqrdiffX;

			sqrdiffY = (float) Math.pow(
					(Float.parseFloat(curCSV.getString(2)) - meanVal[1]), 2);
			varianceY += sqrdiffY;

			sqrdiffZ = (float) Math.pow(
					(Float.parseFloat(curCSV.getString(3)) - meanVal[2]), 2);
			varianceZ += sqrdiffZ;

			total = Float.parseFloat(curCSV.getString(0));
		}
		stndrdDev[0] = (float) Math.sqrt(varianceX); // standard deviation for
														// the x
		stndrdDev[1] = (float) Math.sqrt(varianceY); // standard deviation for
														// the y
		stndrdDev[2] = (float) Math.sqrt(varianceZ); // standard deviation for
														// the z

		float num = (float) Math.sqrt(total);
		stndrdErrX = stndrdDev[0] / num;
		stndrdErrY = stndrdDev[1] / num;
		stndrdErrZ = stndrdDev[2] / num;
		
		mrgnErrX = (float) (1.96 * stndrdErrX);
		mrgnErrY = (float) (1.96 * stndrdErrY);
		mrgnErrZ = (float) (1.96 * stndrdErrZ);

		Log.i(LOGTAG, "std Dev: " + stndrdDev[0] + " " + stndrdDev[1] + " "
				+ stndrdDev[2]);
		Log.i(LOGTAG, "CI for X at Conf. Level 95%: ("
				+ (meanVal[0] + mrgnErrX) + " , " + (meanVal[0] - mrgnErrX)
				+ ")");
		confIntervals[0] = meanVal[0] - mrgnErrX; //floor for x
		confIntervals[1] = meanVal[0] + mrgnErrX; //ceiling for x
		confIntervals[2] = meanVal[1] - mrgnErrY; //floor for y
		confIntervals[3] = meanVal[1] + mrgnErrY; //ceiling for y
		confIntervals[4] = meanVal[2] - mrgnErrZ; //floor for z
		confIntervals[5] = meanVal[2] + mrgnErrZ; //ceiling for z
		
		return confIntervals; // return standard dev.s in array
	}
}
