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

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    
        initMap();



        //这句话必须写，不然程序bug
        mDatabaseHelper = new DatabaseHelper(this);
        //get data from previous activity
        Intent receivedIntent = getIntent();
        patientID = receivedIntent.getStringExtra("patientID");
        doctorID = receivedIntent.getStringExtra("doctorID");
        goalOfWeek=Double.parseDouble(receivedIntent.getStringExtra("goalOfWeek"));


        mDatabaseHelper=new DatabaseHelper(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("UserDataHis").child(doctorID).child(patientID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for(DataSnapshot child: children){


                    UserDataContainer container = child.getValue(UserDataContainer.class);
                    //Toast.makeText(Summary.this, ""+container.getDistance(), Toast.LENGTH_SHORT).show();
                    containers.add(container);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        toggle = (ToggleButton) findViewById(unimelb.lungcare.R.id.toggleId);
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

    //点击后，开启新线程计算走路距离
    int iniDialog = 0;
    class ToggleListener implements CompoundButton.OnCheckedChangeListener {


        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                toggle.setChecked(true);
                iniLocation();
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
            //点击 stop按钮后执行
            else {
                toggle.setChecked(false);
                
                //在此UI界面显示AlertDialog
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

        public void getBarometerAndGPS(){

            pressureListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    float p = event.values[0];//values[0]: Atmospheric pressure in hPa (millibar)

                    double Altitude = getAltitudeFromPressure(p);
                    if (cnt == 0) {
                        tempAltitude = Altitude;
                        cnt++;
                    }
                    //一层楼标准3.3米，每变化3.3米，重新赋值，楼层数+1
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

            //得到locationManager对象，绑定监听器
            LocationManager locationManager = (LocationManager) GoogleTrack.this.getSystemService(Context.LOCATION_SERVICE);
            //1.定义当前所使用的Location Provider
            //设置最短更新时间是10000毫秒，最短更新距离10米
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, new NewLocationListener());

        }
    }

    private AlertDialog.Builder setPositiveButtonForGoal(AlertDialog.Builder builder){
        return builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

    }
    private AlertDialog.Builder setPositiveButton(AlertDialog.Builder builder) {

        return builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //谷歌地图上画图
                drawlines(locations);
                //将数据发送至云端和本地数据库
                addDataToCloud(locations);
                //Log.i(TAG,patientID);#123
                addDataToSQLite(locations);
                Toast.makeText(GoogleTrack.this, "Submiting...", Toast.LENGTH_SHORT).show();
                locations.clear();
                upAltitude=0;
                downAltitude=0;
            }
        });

    }

    private AlertDialog.Builder setNegativeButton(AlertDialog.Builder builder) {
        //调用 setNegativeButton方法添加“cancell"事件
        return builder.setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //如果delete，清除arraylist数据
                upAltitude=0;
                downAltitude=0;
                locations.clear();
                Toast.makeText(GoogleTrack.this, "Deleting...", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //将所有数据信息发送至firebase
    public void addDataToCloud(ArrayList<Location>locations){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String dateToString =simpleDateFormat.format(date);
        UserDataContainer container =new UserDataContainer();
        container.setDate(dateToString);
        //container.setpatientID(patientID);
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
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = database.getReference();
            databaseReference.child("UserDataHis").child(doctorID).child(patientID).push().setValue(container);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Unable to save data to cloud", Toast.LENGTH_LONG).show();
        }

    }

    //将所有坐标信息插入到数据库
    public void addDataToSQLite( ArrayList<Location> locations) {

        boolean insertData = mDatabaseHelper.addData(patientID, locations);
        Log.i(TAG," 1"+insertData);
        if (insertData == true) {
            Toast.makeText(GoogleTrack.this, "Data Successfully submited!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(GoogleTrack.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    ArrayList<Location> locations = new ArrayList<Location>();
    Polyline line;

    //draw line after press stop
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

    //track location in google map based on GPS sensor.
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
    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(unimelb.lungcare.R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }
    //update the googlemap
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
