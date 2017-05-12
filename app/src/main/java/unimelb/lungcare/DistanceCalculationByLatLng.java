package unimelb.lungcare;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Administrator on 4/25/2017.
 */

public class DistanceCalculationByLatLng {
    static final Double EARTH_RADIUS = 6378.137;
    private static double rad(double d)
    {
        return d * Math.PI / 180.0;
    }
    public static double getRealDistance(double lat1, double lng1,
                                         double lat2, double lng2) {

        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10;
        return s;
    }

    public static double getDistanceFromLocations(ArrayList<Location> locations){
                double distance =0;
                for(int i =0; i <locations.size()-1; i++){
                    distance = distance+getRealDistance(locations.get(i).getLatitude(),locations.get(i).getLongitude(),
                            locations.get(i+1).getLatitude(),locations.get(i+1).getLongitude());
                }
        return distance;
    }
}
