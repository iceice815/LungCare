package unimelb.lungcare;

import java.util.ArrayList;

/**
 * Created by Bing Xie on 4/19/2017.
 * used to defined a container for upload data to real-time database
 */

public class UserDataContainer {
    private String date;
    private ArrayList<GeoPoint>history;
    private double distance;
    private double upAltitude;
    private double downAltitude;

    public double getUpAltitude() {
        return upAltitude;
    }

    public void setUpAltitude(double upAltitude) {
        this.upAltitude = upAltitude;
    }

    public double getDownAltitude() {
        return downAltitude;
    }

    public void setDownAltitude(double downAltitude) {
        this.downAltitude = downAltitude;
    }


    public double getDistance() {
        return distance;

    }

    public void setDistance(double distance) {
        this.distance = distance;
    }



    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public ArrayList<GeoPoint> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<GeoPoint> history) {
        this.history = history;
    }

//    public String getpatientID() {
//        return patientID;
//    }
//
//    public void setpatientID(String patientID) {
//        this.patientID = patientID;
//    }
//
//
    }
