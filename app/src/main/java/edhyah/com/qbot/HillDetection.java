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
    private static final int UPDATE_DELAY_MS = 100;
    private static final float HILL_THRESHOLD = (float)6.5;
    private float[] mPastVals;
    private int mPastValsInd = 0;
    private boolean onHill = false;
    private int numHillsPassed = 0;
    private long lastUpdate = 0;

    public HillDetection(Context mContext) {
        senSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mPastVals = new float[NUM_AVG_VALS];
        for (int i = 0; i < mPastVals.length; i++) {
            mPastVals[i] = HILL_THRESHOLD;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float y = getAvg(sensorEvent.values[1]);

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > UPDATE_DELAY_MS) {
                lastUpdate = curTime;

                if (!onHill && y < HILL_THRESHOLD) {
                    onHill = true;
                } else if (onHill && y > HILL_THRESHOLD) {
                    onHill = false;
                    numHillsPassed++;
                }

                Log.i(TAG, Arrays.toString(sensorEvent.values) + " " + onHill);
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
