package edhyah.com.qbot;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParameterFiltering {

    private double PID_P = 1;
    private double PID_I = 0;
    private double PID_D = 0;
    private static final double[] LP_FLT = {.3, .3, .2, .1, .1 , .1, .1};
    private static final double TARGET_VAL = 0;
    private static final String TAG = "ParameterFiltering";
    private List<Double> mPastVals;
    private double mPidErr;
    private double mPrevVal = 0;

    public ParameterFiltering(double P, double I, double D) {
        this();
        PID_P = P;
        PID_I = I;
        PID_D = D;
    }

    public ParameterFiltering() {
        Double[] a = new Double[LP_FLT.length];
        Arrays.fill(a, 0.0);
        mPastVals = new ArrayList<Double>(Arrays.asList(a));
    }

    public double filter(double val) {

        // Low pass
        double lpVal = filterLp(val);

        // PID
        double rtrnVal = 0;
        double err = TARGET_VAL - lpVal;
        mPidErr += err;
        double diff = (lpVal - mPrevVal) / 2;
        rtrnVal = PID_P * lpVal + PID_I * mPidErr + PID_D * diff;
        mPrevVal = rtrnVal;

        Log.i(TAG, "Err " + mPidErr + " " + err + " " + rtrnVal);

        return rtrnVal;

    }

    private double filterLp(double val) {
        // Most recent first
        mPastVals.add(0,val);
        mPastVals.remove(mPastVals.size()-1);

        double fltVal = 0;
        for (int i = 0; i < mPastVals.size(); i++) {
            fltVal += LP_FLT[i]*mPastVals.get(i);
        }

        return fltVal;
    }

}
