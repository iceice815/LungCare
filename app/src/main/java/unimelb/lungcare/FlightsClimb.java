package unimelb.lungcare;


import android.icu.text.DecimalFormat;


/**
 * Created by Administrator on 4/24/2017.
 */

public class FlightsClimb {
    public static double getAltitudeFromPressure(float p) {

        DecimalFormat df = new DecimalFormat("0.00");
        df.getRoundingMode();
        //计算海拔高度
        double Altitude = 44330000 * (1 - (Math.pow((Double.parseDouble(df.format(p)) / 1013.25), (float) 1.0 / 5255.0)));

        return Altitude;
    }
}
