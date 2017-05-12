package unimelb.lungcare;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.abs;


public class Summary extends AppCompatActivity {
    //private TextView patientID;
    private TextView dayClimbNum;
    private TextView weekClimbNum;
    private Button update;
    private TextView dayDistance;
    private TextView weekDistance;
    private BarChart barChart;
    private PieChart pieChart;
    private PieChart pieChart2;

    private DatabaseReference mDatabase;
    ArrayList<String> dates=new ArrayList<>();
    ArrayList<Double> distancePerWeek = new ArrayList<>();

    ArrayList<Double> upAltitudePerWeek = new ArrayList<>();

    ArrayList<Double> downAltitudePerWeek = new ArrayList<>();
    ArrayList<BarEntry>barEntries=new ArrayList<>();
    ArrayList<BarEntry>barEntries2=new ArrayList<>();
    DatabaseHelper mDatabaseHelper;
    String patientIDString;
    String doctorIDString;
    double goalOfWeek;

    ArrayList<UserDataContainer> containers = new ArrayList<UserDataContainer>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(unimelb.lungcare.R.layout.activity_summary2);

        Intent receivedIntent = getIntent();
        patientIDString = receivedIntent.getStringExtra("patientID");
        doctorIDString = receivedIntent.getStringExtra("doctorID");
        goalOfWeek=Double.parseDouble(receivedIntent.getStringExtra("goalOfWeek"));
        mDatabaseHelper=new DatabaseHelper(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("UserDataHis").child(doctorIDString).child(patientIDString).addValueEventListener(new ValueEventListener() {
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
        Toast.makeText(Summary.this, ""+containers.size(), Toast.LENGTH_SHORT).show();


        barChart=(BarChart)findViewById(unimelb.lungcare.R.id.bargraphID);
        pieChart=(PieChart)findViewById(R.id.piechartID) ;
        pieChart2=(PieChart)findViewById(R.id.piechartID2) ;

        //得到一周日期
        dates=getWeekList();
        distancePerWeek = getWeekDis(dates,containers);
        upAltitudePerWeek=getWeekUpAltitude(dates,containers);
        downAltitudePerWeek=getWeekDownAltitude(dates,containers);
        //得到对应的每天距离
        barEntries=getBarEntries(dates,distancePerWeek);
        populateBarChart(barChart,barEntries,dates);


        setPieChartProperty(pieChart);
        populatePieChart(pieChart);

        setPieChartProperty(pieChart2);
        populatePieChart2(pieChart2);


        update =(Button)findViewById(R.id.update);
        //传入patientID
        //patientID = (TextView)findViewById(unimelb.lungcare.R.id.patientID);
        //populatepatientID(patientID);
        //得出每天爬的楼层数
        dayClimbNum=(TextView)findViewById(unimelb.lungcare.R.id.climbNumID);
        weekClimbNum =(TextView)findViewById(R.id.weekfloorID);



        dayDistance=(TextView)findViewById(unimelb.lungcare.R.id.dayDistanceID);

        weekDistance=(TextView)findViewById(unimelb.lungcare.R.id.weekDistanceID);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //得到一周日期
                dates=getWeekList();
                distancePerWeek = getWeekDis(dates,containers);
                upAltitudePerWeek=getWeekUpAltitude(dates,containers);
                downAltitudePerWeek=getWeekDownAltitude(dates,containers);
                //得到对应的每天距离
                barEntries=getBarEntries(dates,distancePerWeek);
                barChart.setActivated(true);
                //生成barchart
                populateBarChart(barChart,barEntries,dates);

                setPieChartProperty(pieChart);
                setPieChartProperty(pieChart2);
                populatePieChart(pieChart);

                populatePieChart2(pieChart2);

                populateDayClimbFloor(dayClimbNum);
                //得出每天走的距离
                populateDayDistance(dayDistance);
                populateweekClumbFloor(weekClimbNum);
                //计算此周走的距离
                populateweekDistance(weekDistance);
            }
        });

    }
    public ArrayList<Double> getWeekDis(ArrayList<String>dates, ArrayList<UserDataContainer> containers){
        ArrayList<Double> weekdis = new ArrayList<>();
        for(int i=0; i<dates.size(); i++){

            double dis = 0;
            for(int j=0;j<containers.size();j++){

                if(containers.get(j).getDate().equals(dates.get(i))){
                     dis = dis+containers.get(j).getDistance();

                }
            }
            weekdis.add(dis);
        }
        return weekdis;
    }
    public ArrayList<Double> getWeekUpAltitude(ArrayList<String>dates, ArrayList<UserDataContainer> containers){
        ArrayList<Double> weekUpAltitude = new ArrayList<>();
        for(int i=0; i<dates.size(); i++){

            double dis = 0;
            for(int j=0;j<containers.size();j++){

                if(containers.get(j).getDate().equals(dates.get(i))){
                    dis = dis+containers.get(j).getUpAltitude();

                }
            }
            weekUpAltitude.add(dis);
        }
        return weekUpAltitude;
    }
    public ArrayList<Double> getWeekDownAltitude(ArrayList<String>dates, ArrayList<UserDataContainer> containers){
        ArrayList<Double> weekDownAltitude = new ArrayList<>();
        for(int i=0; i<dates.size(); i++){

            double dis = 0;
            for(int j=0;j<containers.size();j++){

                if(containers.get(j).getDate().equals(dates.get(i))){
                    dis = dis+containers.get(j).getDownAltitude();

                }
            }
            weekDownAltitude.add(dis);
        }
        return weekDownAltitude;
    }


    public void populateBarChart(BarChart barChart,ArrayList<BarEntry>barEntries,ArrayList<String>dates){
        BarDataSet barDataSet = new BarDataSet(barEntries,"unit/m");

        barDataSet.setColor(Color.RED);;

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        XAxis xAxis = barChart.getXAxis();


        ArrayList<String>datesForBarchart=new ArrayList<>();
        for(int i = 0 ; i <dates.size();i++){
            datesForBarchart.add(dates.get(i).substring(5));
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(datesForBarchart));
        barChart.setDescription(null);
    }
    public ArrayList<BarEntry>getBarEntries(ArrayList<String>dates,ArrayList<Double>distancePerWeek){
        ArrayList<BarEntry> barEntriesTemp=new ArrayList<>();
        for (int i =0; i <dates.size(); i++){
            double value = distancePerWeek.get(i);
            barEntriesTemp.add(new BarEntry(i,(float)value));
        }
        return barEntriesTemp;
    }


    public ArrayList<String> getWeekList(){
        ArrayList<String> list = new ArrayList<>();
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(date);
        switch (cal.get(Calendar.DAY_OF_WEEK)){
            //星期天
            case 1:
//                for(int i=0;i<7;i++) {
//                    list.add(getDate(cal));
//                    cal.add(Calendar.DAY_OF_MONTH,-1);
//
//                }
//                Collections.reverse(list);
                cal.add(Calendar.DAY_OF_MONTH,-6);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }
                break;
            //星期一
            case 2:
                for(int i=0;i<7;i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }

                break;
            //星期二
            case 3:
                cal.add(Calendar.DAY_OF_MONTH,-1);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }
                break;
            //星期三
            case 4:
                cal.add(Calendar.DAY_OF_MONTH,-2);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }

                break;
            //星期四
            case 5:
                cal.add(Calendar.DAY_OF_MONTH,-3);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }

                break;
            //星期五
            case 6:
                cal.add(Calendar.DAY_OF_MONTH,-4);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }


                break;
            //星期六
            case 7:
                cal.add(Calendar.DAY_OF_MONTH,-5);
                for (int i = 0 ; i <7 ; i++){
                    list.add(getDate(cal));
                    cal.add(Calendar.DAY_OF_MONTH,1);
                }

                break;

        }
        return list;
    }
    public String getDate(Calendar cld){
        String curDate = cld.get(Calendar.YEAR)+"/"+(cld.get(Calendar.MONTH)+1)+"/"
                +cld.get(Calendar.DAY_OF_MONTH);
        try{
            Date date = new SimpleDateFormat("yyyy/MM/dd").parse(curDate);
            curDate = new SimpleDateFormat("yyyy/MM/dd").format(date);
        }catch (ParseException e){
            e.printStackTrace();
        }
        return curDate;
    }
    public void populateDayClimbFloor(TextView climbFloor){
        String dateToString =getTodayDate();
        double upAltitude =0;
        double downAltitude = 0;
        for(int i =0;i<containers.size();i++){

            if(containers.get(i).getDate().equals(dateToString)){
                upAltitude = upAltitude+containers.get(i).getUpAltitude();
                downAltitude=downAltitude+containers.get(i).getDownAltitude();
            }
;
        }
        dayClimbNum.setText("UP:         "+(int)(upAltitude/3.3)+" floors\n"+"DOWN:  "+(int)(downAltitude/3.3)+" floors");

    }
    public void populateDayDistance(TextView dayDistance){
        String dateToString =getTodayDate();
        double dis =0;
        for(int i =0;i<containers.size();i++){

            if(containers.get(i).getDate().equals(dateToString)){
                dis = dis+containers.get(i).getDistance();
            }
        }
        dayDistance.setText(dis+" m");

    }
    public void populateweekClumbFloor(TextView weekClimbNum){
        double up = 0;
        double down = 0;
        for(int i = 0; i <upAltitudePerWeek.size();i++){
            up=up+upAltitudePerWeek.get(i);
        }
        for(int i = 0; i <downAltitudePerWeek.size();i++){
            down=down+downAltitudePerWeek.get(i);
        }
        weekClimbNum.setText("UP:         "+(int)(up/3.3)+" floors\n"+"DOWN:  "+(int)(down/3.3)+" floors");

    }

    public void populateweekDistance(TextView weekDistance){
        double weekDis = 0;
        for (int i=0;i<distancePerWeek.size();i++){
            weekDis=weekDis+distancePerWeek.get(i);

        }
        weekDistance.setText(weekDis+ " m");

    }
    public String getTodayDate(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String dateToString =simpleDateFormat.format(date);
        return dateToString;
    }
    public void setPieChartProperty(PieChart pieChart){
        pieChart.setDescription(null);
        pieChart.setRotationEnabled(true);
        pieChart.setUsePercentValues(true);
        //pieChart.setHoleColor(Color.BLUE);
        //pieChart.setCenterTextColor(Color.BLACK);
        pieChart.setHoleRadius(5f);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setCenterTextSize(10);
        //pieChart.setDrawEntryLabels(true);
        //pieChart.setEntryLabelTextSize(20);
    }
    public void populatePieChart(PieChart pieChart){
        ArrayList<PieEntry> yEntrys = new ArrayList<>();
        ArrayList<String> xEntrys =new ArrayList<>();
        double weekDis = 0;
        for (int i=0;i<distancePerWeek.size();i++){
            weekDis=weekDis+distancePerWeek.get(i);

        }
        if (weekDis<=goalOfWeek) {
            yEntrys.add(new PieEntry((float) weekDis, 0));
            yEntrys.add(new PieEntry((float) (goalOfWeek - weekDis), 1));
        }else {

            yEntrys.add(new PieEntry((float) goalOfWeek, 0));
            yEntrys.add(new PieEntry((float) goalOfWeek , 1));
        }
        xEntrys.add("Finished");
        xEntrys.add("Unfinished");
        PieDataSet pieDataSet = new PieDataSet(yEntrys,"Week Progress");
        pieDataSet.setSliceSpace(2);
        pieDataSet.setValueTextSize(12);
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.YELLOW);
        colors.add(Color.GREEN);
        pieDataSet.setColors(colors);
        Legend legend = pieChart.getLegend();
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setPosition(Legend.LegendPosition.LEFT_OF_CHART);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
    public void populatePieChart2(PieChart pieChart){
        ArrayList<PieEntry> yEntrys = new ArrayList<>();
        ArrayList<String> xEntrys =new ArrayList<>();
        String dateToString =getTodayDate();
        double dis =0;
        for(int i =0;i<containers.size();i++){

            if(containers.get(i).getDate().equals(dateToString)){
                dis = dis+containers.get(i).getDistance();
            }
        }
        if (dis<=goalOfWeek/7) {
            yEntrys.add(new PieEntry((float) dis, 0));
            yEntrys.add(new PieEntry((float) (goalOfWeek/7 - dis), 1));
        }else {

            yEntrys.add(new PieEntry((float) goalOfWeek, 0));
            yEntrys.add(new PieEntry((float) goalOfWeek , 1));
        }
        xEntrys.add("Finished");
        xEntrys.add("Unfinished");
        PieDataSet pieDataSet = new PieDataSet(yEntrys,"Day Progress");
        pieDataSet.setSliceSpace(2);
        pieDataSet.setValueTextSize(12);
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.RED);
        colors.add(Color.GREEN);
        pieDataSet.setColors(colors);
        Legend legend = pieChart.getLegend();
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setPosition(Legend.LegendPosition.LEFT_OF_CHART);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

}

