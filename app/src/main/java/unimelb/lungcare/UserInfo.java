package unimelb.lungcare;

/**
 * Created by Administrator on 4/19/2017.
 */

public class UserInfo {
    private String patientID;
    private double goalOfWeek;
    private String updateTime;
    private String doctorID;

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }



    public String getpatientID() {

        return patientID;
    }

    public void setpatientID(String patientID) {

        this.patientID = patientID;
    }

    public double getgoalOfWeek() {

        return goalOfWeek;
    }

    public void setgoalOfWeek(double goalOfWeek) {

        this.goalOfWeek = goalOfWeek;
    }

    public String getUpdateTime() {

        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }


}
