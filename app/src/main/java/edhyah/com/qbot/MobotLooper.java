package edhyah.com.qbot;

import android.content.Context;
import android.util.Log;

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
    private static final String TAG = "MobotIOIOLooper";

    private final static int RIGHT_MOTOR_PIN = 3;
    private final static int LEFT_MOTOR_PIN = 2;
    private final static int LED_TOGGLE_DELAY = 10;

    // PWM
    private final int PWM_MIN_VAL_DEFAULT = 1000;
    private final int PWM_MAX_VAL_DEFAULT = 2000;
    private final int PWM_OFF_VAL = 0;
    private final int PWM_FREQUENCY_IN_HZ = 50;
    
    // DRIVING
    private static final double DRIVE_STRAIGHT_ANGLE = .05; //deg
    public static final double MAX_SPEED = 20; // inchec/sec
    private static final double DELTA_T = 1; // sec TODO pass in as variable
    private static final double MOBOT_DRIVETRAIN_WIDTH = 5.5; // inches

    public int mPwmDriveRightVal = PWM_OFF_VAL;
    public int mPwmDriveLeftVal = PWM_OFF_VAL;

    private Context mContext;
    private MobotDriver mMobotDriver;
    private PwmOutput mRightMotor;
    private PwmOutput mLeftMotor;
    private DigitalOutput mStatusLed;
    private int mLedToggleCounter = LED_TOGGLE_DELAY;

    // Hill detection
    public boolean onHill = false;
    public int numHillsPassed = 0;
    private HillDetection mHillDetect;

    public MobotLooper(Context context, MobotDriver mobotDriver) {
        mContext = context;
        mMobotDriver = mobotDriver;
        mHillDetect = new HillDetection(mContext);
    }

    @Override
    public void setup() {
        try {
            mStatusLed = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
            mRightMotor = ioio_.openPwmOutput(RIGHT_MOTOR_PIN, PWM_FREQUENCY_IN_HZ);
            mLeftMotor = ioio_.openPwmOutput(LEFT_MOTOR_PIN, PWM_FREQUENCY_IN_HZ);
        } catch (ConnectionLostException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void loop() {
        try {
            driveMobot();
            toggleStatusLed();
            mMobotDriver.setStatusOnline(true);
            onHill = mHillDetect.isOnHill();
            numHillsPassed = mHillDetect.getNumHillsPassed();
        } catch (ConnectionLostException e) {
            Log.e(TAG, e.getMessage());
            mMobotDriver.setStatusOnline(false);
        }
    }

    private void driveMobot() throws ConnectionLostException {
        double angle = mMobotDriver.getDriveAngle();
        double speed = mMobotDriver.getDriveSpeed(); // Percent, negative signifies going backwards
        double tunning = mMobotDriver.getTunning();
        double maxSpeed = mMobotDriver.getMaxSpeed();

        Log.i(TAG, "Angle " + angle + " Speed " + speed);

        // Calc SpeedRatio
        double turnDist = Math.abs(speed) * maxSpeed * DELTA_T;
        double speedRatio = 1;
        if (Math.abs(angle) > DRIVE_STRAIGHT_ANGLE) {
            // Only calculate if turning angle is significant enough
            double turnRadius = Math.abs(turnDist / 2 / Math.tan(Math.toRadians(angle / 2)));
            speedRatio = (turnRadius - MOBOT_DRIVETRAIN_WIDTH) /
                    (turnRadius + MOBOT_DRIVETRAIN_WIDTH); // Percentage
        }

        // Update right and left speeds with tunning and speed turning ratio
        double rightSpeed = speed;
        double leftSpeed = speed;
        if (angle < 0) {
            leftSpeed *= speedRatio;
        } else {
            rightSpeed *= speedRatio;
        }

        // Tunning reduces speed of opposite motor
        if (tunning < 0) {
            rightSpeed *= 1 + tunning;
        } else {
            leftSpeed *= 1 - tunning;
        }

        mPwmDriveRightVal = speedToPwm(-rightSpeed);
        mPwmDriveLeftVal = speedToPwm(leftSpeed); // Reversed motor

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
        public double getTunning();
        public double getMaxSpeed();
        public double getP();
        public double getI();
        public double getD();
        public void setStatusOnline(boolean status);
    }

}