package edhyah.com.qbot;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParameterFiltering {

    private double PID_P = 1;
    private double PID_I = 0;
    private double PID_D = 0;
    private static final double[] LP_FLT = {-0.000467261256851604, -0.000260599120789982, 3.45950906169090e-19, 0.000338180362705885, 0.000767746391302084, 0.00127974492289457, 0.00183223311447475, 0.00234562058315943, 0.00270576869572814, 0.00277585050968151, 0.00241648570616055, 0.00151209260457448, -1.41733428997505e-18, -0.00210211912200477, -0.00467529622511199, -0.00748924240273710, -0.0102066020353605, -0.0124031194153741, -0.0136032198240601, -0.0133282438318955, -0.0111525041276046, -0.00676077166879273, 3.09803363940106e-18, 0.00908177560795142, 0.0202153368110202, 0.0329129948353904, 0.0464955340258655, 0.0601418821790831, 0.0729571379918162, 0.0840521579560541, 0.0926262483196677, 0.0980438603082945, 0.0998966562095168, 0.0980438603082945, 0.0926262483196677, 0.0840521579560541, 0.0729571379918162, 0.0601418821790831, 0.0464955340258655, 0.0329129948353904, 0.0202153368110202, 0.00908177560795142, 3.09803363940106e-18, -0.00676077166879273, -0.0111525041276046, -0.0133282438318955, -0.0136032198240601, -0.0124031194153741, -0.0102066020353605, -0.00748924240273710, -0.00467529622511199, -0.00210211912200477, -1.41733428997505e-18, 0.00151209260457448, 0.00241648570616055, 0.00277585050968151, 0.00270576869572814, 0.00234562058315943, 0.00183223311447475, 0.00127974492289457, 0.000767746391302084, 0.000338180362705885, 3.45950906169090e-19, -0.000260599120789982, -0.000467261256851604};
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
        double err = lpVal - TARGET_VAL;
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
