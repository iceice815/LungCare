package unimelb.lungcare;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static unimelb.lungcare.DistanceCalculationByLatLng.getDistanceFromLocations;
import static unimelb.lungcare.FlightsClimb.getAltitudeFromPressure;
/**
 * Created by Bing Xie on 4/19/2017.
 * define the second activity with for recording data and display route on google Map
 */

public class GoogleTrack extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ToggleButton toggle;
    private Button trackButton;
    private String patientID;
    private String doctorID;
    private double upAltitude;
    private double downAltitude;
    private double goalOfWeek;
    private static final String TAG = "GoogleTrackActivity";
    DatabaseHelper mDatabaseHelper;
    private DatabaseReference mDatabase;
    private SensorManager sensorManager ;
    private Sensor mPressure;
    private SensorEventListener pressureListener;
    ArrayList<UserDataContainer> containers = new ArrayList<UserDataContainer>();


    int cnt = 0;
    double tempAltitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(unimelb.lungcare.R.layout.activity_google_track);
        //create sensorManager object from system service
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //use the baromeer sensor
        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        //initilize google map
        initMap();
        //add SQLite to program
        mDatabaseHelper = new DatabaseHelper(this);
        //get data from previous activity
        Intent receivedIntent = getIntent();
        patientID = receivedIntent.getStringExtra("patientID");
        doctorID = receivedIntent.getStringExtra("doctorID");
        goalOfWeek=Double.parseDouble(receivedIntent.getStringExtra("goalOfWeek"));
        //used to retrieve patient previous data for telling user the remaining goal
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("UserDataHis").child(doctorID).child(patientID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for(DataSnapshot child: children){


                    UserDataContainer container = child.getValue(UserDataContainer.class);
                    containers.add(container);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //defined a toogle
        toggle = (ToggleButton) findViewById(unimelb.lungcare.R.id.toggleId);
        //defined a button
        trackButton = (Button) findViewById(unimelb.lungcare.R.id.summaryId);

        ToggleListener listener = new ToggleListener();
        toggle.setOnCheckedChangeListener(listener);

        trackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent();
                intent.setClass(GoogleTrack.this,Summary.class);
                intent.putExtra("patientID",patientID);
                intent.putExtra("doctorID",doctorID);
                intent.putExtra("goalOfWeek",String.valueOf(goalOfWeek));
                startActivity(intent);
            }
        });


    }

    int iniDialog = 0;
    //add a listener to toggle
    class ToggleListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            //press start and execute
            if (isChecked) {
                toggle.setChecked(true);
                //initialize location
                iniLocation();
                //display a AlertDialog to tell user the remaining walking distance

                if(iniDialog==0) {
                    double weekDis = getGoalOfWeekDialog();
                    AlertDialog.Builder builder = new AlertDialog.Builder(GoogleTrack.this)
                            .setTitle("Goal")
                            .setIcon(R.drawable.goal)
                            .setMessage("You have " + (goalOfWeek - weekDis) + " m left this week!");
                    setPositiveButtonForGoal(builder)
                            .create()
                            .show();
                }
                iniDialog++;
                getBarometerAndGPS();
                }
            //press stop and execute
            else {
                toggle.setChecked(false);
                
                //display a AlertDialog to ask user whether upload data to real-time database
                AlertDialog.Builder builder = new AlertDialog.Builder(GoogleTrack.this)
                        .setTitle("Synchronization")
                        .setIcon(unimelb.lungcare.R.drawable.submittool)
                        .setMessage("Do you submit your walking data to database?");
                setPositiveButton(builder);
                setNegativeButton(builder)
                        .create()
                        .show();
                //locations.clear();


            }

        }

        /**
         * @return the remaining walking distance
         */
        public double getGoalOfWeekDialog(){
            Summary sum =new Summary();
            ArrayList<String> dates=new ArrayList<>();
            dates=sum.getWeekList();
            ArrayList<Double> distancePerWeek = new ArrayList<>();
            distancePerWeek = sum.getWeekDis(dates,containers);
            double weekDis = 0;
            for (int i=0;i<distancePerWeek.size();i++){
                weekDis=weekDis+distancePerWeek.get(i);
            }
            return weekDis;
        }
        /**
         * invoke GPS and Barometer sensor
         */
        public void getBarometerAndGPS(){
            //define air pressure lisener
            pressureListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    //return from Barometer sensor values[0]: Atmospheric pressure in hPa (millibar)
                    float p = event.values[0];

                    double Altitude = getAltitudeFromPressure(p);
                    if (cnt == 0) {
                        tempAltitude = Altitude;
                        cnt++;
                    }
                    /**
                     * By my dozens of experiments, two meter as sample rate will have high efficiency
                     */
                    if ((Altitude - tempAltitude) >= 2) {
                        tempAltitude = Altitude;
                        upAltitude=upAltitude+2;
                    }
                    if ((Altitude - tempAltitude) <= -2) {
                        tempAltitude = Altitude;
                        downAltitude=downAltitude+2;
                    }
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

            //get locationManager object, bind lisener
            LocationManager locationManager = (LocationManager) GoogleTrack.this.getSystemService(Context.LOCATION_SERVICE);
            //define current defined Location Provider
            if (ActivityCompat.checkSelfPermission(GoogleTrack.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleTrack.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            //set return location from GPS every 2 second gap
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10000, new NewLocationListener());

        }
    }
    /**
     * the button in dialog for telling user to comfirm his goal
     */
    private AlertDialog.Builder setPositiveButtonForGoal(AlertDialog.Builder builder){
        return builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

    }
    /**
     * the button in dialog for submitting data to real-time database
     */
    private AlertDialog.Builder setPositiveButton(AlertDialog.Builder builder) {

        return builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //draw walk route on google map
                drawlines(locations);
                //sen data to real-time database of cloud server
                addDataToCloud(locations);
                //add data to SQLite for local (app) data backup
                addDataToSQLite(locations);
                Toast.makeText(GoogleTrack.this, "Submiting...", Toast.LENGTH_SHORT).show();
                locations.clear();
                upAltitude=0;
                downAltitude=0;
            }
        });

    }
    /**
     * the button in dialog for submitting data to real-time database
     */

    private AlertDialog.Builder setNegativeButton(AlertDialog.Builder builder) {
        //invoke setNegativeButton method for add “cancell" event
        return builder.setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                upAltitude=0;
                downAltitude=0;
                locations.clear();
                Toast.makeText(GoogleTrack.this, "Deleting...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * add data to cloud server
     */

    public void addDataToCloud(ArrayList<Location>locations){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String dateToString =simpleDateFormat.format(date);
        UserDataContainer container =new UserDataContainer();
        container.setDate(dateToString);
        double tempdis =getDistanceFromLocations(locations);
        container.setDistance(tempdis);
        ArrayList<GeoPoint> history = new ArrayList<>();
        for (int i = 0 ; i <locations.size();i++){
            GeoPoint geoPoint = new GeoPoint();
            geoPoint.setLatitude(Double.toString(locations.get(i).getLatitude()));
            geoPoint.setLongtitude(Double.toString(locations.get(i).getLongitude()));
            history.add(geoPoint);
        }
        container.setHistory(history);
        container.setUpAltitude(upAltitude);
        container.setDownAltitude(downAltitude);
        try {
            //get the firebase instance
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = database.getReference();
            //upload patient walking data after finding sub-root ‘doctorID’ then sub-root ‘patientID”.
            databaseReference.child("UserDataHis").child(doctorID).child(patientID).push().setValue(container);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Unable to save data to cloud", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * add data to SQLite
     */
    public void addDataToSQLite( ArrayList<Location> locations) {

        boolean insertData = mDatabaseHelper.addData(patientID, locations);
        if (insertData == true) {
            Toast.makeText(GoogleTrack.this, "Data Successfully submited!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(GoogleTrack.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    ArrayList<Location> locations = new ArrayList<Location>();
    Polyline line;

    /**
     * press stop and draw line on google map
     */
    private void drawlines(ArrayList<Location> locations) {
        PolylineOptions options = new PolylineOptions()
                .color(Color.RED)
                .width(10);
        for (int i = 0; i < locations.size(); i++) {
            LatLng ll = new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude());
            options.add(ll);
        }
        line = mMap.addPolyline(options);

    }

    /**
     * track location in google map based on GPS sensor.
     */
    private class NewLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            goTonLocationZoom(lat,lng,16);
            iniLocation();
            locations.add(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    /**
     * initialize the currently location on google map
     */
    private void iniLocation(){
        if (ActivityCompat.checkSelfPermission(GoogleTrack.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleTrack.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }
    /**
     * initialize the google map
     */
    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(unimelb.lungcare.R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }

    /**
     *  zooming the current location and update
     */
    private void goTonLocationZoom(double lat, double lng, int zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mMap.moveCamera(update);
    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(pressureListener, mPressure,
                SensorManager.SENSOR_DELAY_NORMAL);
    }
}
