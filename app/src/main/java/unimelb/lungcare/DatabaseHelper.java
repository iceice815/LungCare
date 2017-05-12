package unimelb.lungcare;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 4/6/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "locations_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "patientID";
    private static final String COL3 = "lat";
    private static final String COL4 = "lng";
    private static final String COL5="date";
    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = " CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 + " TEXT, " + COL3 + " TEXT, " +COL4 + " TEXT, "+COL5 + " TEXT)";
        db.execSQL(createTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public boolean addData(String patientID, ArrayList<Location> locations){
        SQLiteDatabase db = this.getWritableDatabase();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String dateToString =simpleDateFormat.format(date);
        long result=-1;
        for(int i=0;i<locations.size();i++){
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL2,patientID);
            contentValues.put(COL3,locations.get(i).getLatitude());
            contentValues.put(COL4,locations.get(i).getLongitude());
            contentValues.put(COL5,dateToString);
            Log.d(TAG,"addData : Adding "+patientID+" "+locations.get(i).getLatitude()+ " "+
                    locations.get(i).getLongitude()+" "+dateToString+ " to "+TABLE_NAME);
            db.insert(TABLE_NAME,null,contentValues);
            result=0;

        }
        if(result == -1){
            return false;
        }else{
            return true;
        }

    }
    public Cursor getLatLngPoints (String date){
        SQLiteDatabase db = this.getWritableDatabase();
        String query="SELECT * FROM " + TABLE_NAME + " WHERE "+COL5+" = '"+date + "'";
        Cursor data = db.rawQuery(query, null);

        return data;
    }

}