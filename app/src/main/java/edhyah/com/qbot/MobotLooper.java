package edhyah.com.qbot;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

/**
 * Created by Spencer on 3/29/2015.
 */
// TODO use update instead of pull
class MobotLooper extends BaseIOIOLooper {
    private static final String TAG = "MobotLooper";

    private final static int RIGHT_MOTOR_PIN = 2;
    private final static int LEFT_MOTOR_PIN = 3;
    private final static int LED_TOGGLE_DELAY = 100;


    // PWM
    private final int PWM_MIN_VAL_DEFAULT = 1000;
    private final int PWM_MAX_VAL_DEFAULT = 2000;
    private final int PWM_OFF_VAL = 0;
    private final int PWM_FREQUENCY_IN_HZ = 50;
    
    // DRIVING
    private static final double MAX_SPEED = 1; // inchec/sec
    private static final double DELTA_T = .01; // sec TODO pass in as variable
    private static final double MOBOT_DRIVETRAIN_WIDTH = 5.5; // inches

    public int mPwmDriveRightVal = PWM_OFF_VAL;
    public int mPwmDriveLeftVal = PWM_OFF_VAL;

    private Context mContext;
    private MobotDriver mMobotDriver;
    private PwmOutput mRightMotor;
    private PwmOutput mLeftMotor;
    private DigitalOutput mStatusLed;
    private int mLedToggleCounter = LED_TOGGLE_DELAY;

    public MobotLooper(Context context, MobotDriver mobotDriver) {
        mContext = context;
        mMobotDriver = mobotDriver;
    }

    @Override
    public void setup() {
        try {
            mStatusLed = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
            mRightMotor = ioio_.openPwmOutput(RIGHT_MOTOR_PIN, PWM_FREQUENCY_IN_HZ);
            mLeftMotor = ioio_.openPwmOutput(LEFT_MOTOR_PIN, PWM_FREQUENCY_IN_HZ);
        } catch (ConnectionLostException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(mContext, TAG + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loop() {
        try {
            driveMobot();
            toggleStatusLed();
        } catch (ConnectionLostException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(mContext, TAG + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void driveMobot() throws ConnectionLostException {
        double angle = mMobotDriver.getDriveAngle();
        double speed = mMobotDriver.getDriveSpeed(); // Percent, negative signifies going backwards

        Log.i(TAG, "Angle " + angle + " Speed" + speed);
        Toast.makeText(mContext, TAG + "Angle " + angle + " Speed" + speed, Toast.LENGTH_LONG).show();


        double turnDist = Math.abs(speed) * MAX_SPEED * DELTA_T;
        double turnRadius = turnDist / Math.tan(Math.toRadians(angle/2));
        double speedRatio = (turnRadius - MOBOT_DRIVETRAIN_WIDTH) / 
                (turnRadius + MOBOT_DRIVETRAIN_WIDTH); // Percentage

        mPwmDriveRightVal = speedToPwm(speed);
        mPwmDriveLeftVal = speedToPwm(speed*speedRatio);                

        mRightMotor.setPulseWidth(mPwmDriveRightVal);
        mLeftMotor.setPulseWidth(mPwmDriveLeftVal);

    }

    private int speedToPwm(double speed) {
        // Speed is a percentage, negative signifies going backwards
        double speedMag = (speed + 1) / 2; // Map to 0-1
        double mag = (PWM_MAX_VAL_DEFAULT - PWM_MIN_VAL_DEFAULT) * speedMag; // Map to range
        return (int)Math.round(mag + PWM_MIN_VAL_DEFAULT); // Offset and convert to int
    }

    private void toggleStatusLed() throws ConnectionLostException {
        mLedToggleCounter--;
        if (mLedToggleCounter < 0) {
            mStatusLed.write(true);
            mLedToggleCounter = LED_TOGGLE_DELAY;
        } else if (mLedToggleCounter < LED_TOGGLE_DELAY/2) {
            mStatusLed.write(false);
        }
    }

    public interface MobotDriver {
        public double getDriveAngle();
        public double getDriveSpeed();
    }

}