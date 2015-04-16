package edhyah.com.qbot;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Arrays;

import ioio.lib.spi.Log;

/**
 * Created by edwardahn on 12/4/15.
 */
public class HillDetection implements SensorEventListener {
    private static final String TAG = "HillDetection";
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private static final int NUM_AVG_VALS = 10;
    private static final int UPDATE_DELAY_MS = 10;
    private static final int DELAY_UNTIL_NEXT_HILL = 20*1000;
    private static final int COUNTS_ON_HILL = 40;
    private float mCountsOnHill = 0;
    private float mHillThreshold = (float)6.5;
    private float[] mPastVals;
    private int mPastValsInd = 0;
    private boolean onHill = false;
    private int numHillsPassed = 0;
    private long lastUpdate = 0;
    private long timeOfLastHill = -DELAY_UNTIL_NEXT_HILL;

    public HillDetection(Context mContext, float hillThresh) {
        senSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        mHillThreshold = hillThresh;
        mPastVals = new float[NUM_AVG_VALS];
        for (int i = 0; i < mPastVals.length; i++) {
            mPastVals[i] = mHillThreshold;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long curTime = System.currentTimeMillis();
            long timeDiff = curTime - lastUpdate;

            if (timeDiff > UPDATE_DELAY_MS) {
                float y = getAvg(sensorEvent.values[1]);
                lastUpdate = curTime;

                if (y < mHillThreshold) {
                    onHill = true;
                    mCountsOnHill++;
                } else {
                    // Update count if was on hill before and time since last hill is large enough
                    if (onHill && ((curTime - timeOfLastHill) > DELAY_UNTIL_NEXT_HILL) &&
                            (mCountsOnHill > COUNTS_ON_HILL)) {
                        timeOfLastHill = curTime;
                        numHillsPassed++;
                    }
                    mCountsOnHill = 0;
                    onHill = false;
                }

                Log.i(TAG, Arrays.toString(sensorEvent.values) + "(" + y + ")\t" + onHill + "\t"
                    + mCountsOnHill);
            }
        }
    }

    private float getAvg(float val) {
        mPastVals[mPastValsInd] = val;
        mPastValsInd = (mPastValsInd+1) % mPastVals.length;

        float avg = 0;
        for(int i = 0; i < mPastVals.length; i++) {
            avg += mPastVals[i];
        }
        return avg / mPastVals.length;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }

    public boolean isOnHill() {
        return onHill;
    }

    public int getNumHillsPassed() {
        return numHillsPassed;
    }
}
