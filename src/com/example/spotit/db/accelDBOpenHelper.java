package com.example.spotit.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class accelDBOpenHelper extends SQLiteOpenHelper {

	private static final String LOGTAG = "SPOTIT";
	private static final String DATABASE_NAME = "SpotIt.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_NAME = "accelData";
	public static final String COLUMN_ID = "accelId";
	public static final String COLUMN_X = "xVal";
	public static final String COLUMN_Y = "yVal";
	public static final String COLUMN_Z = "zVal";

	private static final String TABLE_CREATE = 
			"CREATE TABLE " + TABLE_NAME + "(" + 
			COLUMN_ID + " INTEGER PRIMARY KEY," +
			COLUMN_X + " NUMERIC, " +
			COLUMN_Y + " NUMERIC, " + 
			COLUMN_Z + " NUMERIC " +
			")";

	public accelDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
		Log.i(LOGTAG, "Table has been created");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

}
