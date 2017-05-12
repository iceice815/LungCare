package unimelb.lungcare;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private EditText patientID;
    private Button login;
    private DatabaseReference mDatabse;
    private NotificationManager nm;
    static final int NOTIFICATION_ID=0x123;
    int cnt = 0;
    ArrayList<UserInfo> userInfos = new ArrayList<UserInfo>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //final ArrayList<UserInfo> userInfos = new ArrayList<UserInfo>();
        patientID = (EditText) findViewById(R.id.patientID);
        login = (Button)findViewById(R.id.btnID);
        nm =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mDatabse = FirebaseDatabase.getInstance().getReference();

        mDatabse.child("Registed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for(DataSnapshot child: children){
                    UserInfo userInfo = child.getValue(UserInfo.class);
                    userInfos.add(userInfo);

                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


       // Toast.makeText(MainActivity.this, ""+userInfos.size(), Toast.LENGTH_SHORT).show();

        //给button绑定监听器
        //获取服务器或本地数据，判断unitcode是否正确

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean loginTag =false;

                //Toast.makeText(MainActivity.this, ""+userInfos.size(), Toast.LENGTH_SHORT).show();
                for(int i = 0; i<userInfos.size();i++){
                    if(userInfos.get(i).getpatientID().equals(patientID.getText().toString())){
                        //notification
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
                        Date date = new Date();
                        String dateToString =simpleDateFormat.format(date);

                        if(userInfos.get(i).getUpdateTime().equals(dateToString)&&cnt==0) {
                            Notification notify = new Notification.Builder(MainActivity.this)
                                    .setAutoCancel(true)
                                    .setTicker("A new message")
                                    .setSmallIcon(R.drawable.goal)
                                    .setContentTitle("Goal Reset by doctorID: "+userInfos.get(i).getDoctorID())
                                    .setContentText("Your doctor resets " +
                                            "your goal as " + (int)userInfos.get(i).getgoalOfWeek() + "m this week！")
                                    .setDefaults(Notification.DEFAULT_SOUND)
                                    .setWhen(System.currentTimeMillis())
                                    .build();
                            nm.notify(NOTIFICATION_ID,notify);
                            cnt++;
                        }
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this,GoogleTrack.class);
                        intent.putExtra("patientID",patientID.getText().toString());
                        intent.putExtra("goalOfWeek",userInfos.get(i).getgoalOfWeek()+"");
                        intent.putExtra("doctorID",userInfos.get(i).getDoctorID());
                        startActivity(intent);
                        loginTag =true;
                    }
                }
                if(loginTag==false){
                    patientID.setText("");
                    Toast toast =Toast.makeText(MainActivity.this, "Wrong patient ID,please input again!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL,0,0);
                    toast.show();


                }
            }
        });


    }
}
