package edhyah.com.qbot;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by edwardahn on 12/4/15.
 */
public class HillDetection implements SensorEventListener {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private static final int HILL_THRESHOLD = 5;
    private boolean onHill = false;
    private int numHillsPassed = 0;
    private long lastUpdate = 0;

    public HillDetection(Context mContext) {
        senSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float y = sensorEvent.values[1];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                if (!onHill && y < HILL_THRESHOLD) {
                    onHill = true;
                } else if (onHill && y > HILL_THRESHOLD) {
                    onHill = false;
                    numHillsPassed++;
                }
            }
        }
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
