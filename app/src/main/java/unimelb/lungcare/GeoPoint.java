package unimelb.lungcare;

/**
 * Created by Bing Xie on 4/19/2017.
 * define a container for location points
 */

public class GeoPoint {
    private String latitude;
    private String longtitude;

    public String getLatitude() {

        return latitude;
    }

    public void setLatitude(String latitude) {

        this.latitude = latitude;
    }

    public String getLongtitude() {

        return longtitude;
    }

    public void setLongtitude(String longtitude) {

        this.longtitude = longtitude;
    }
}
