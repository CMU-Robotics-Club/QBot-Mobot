package edhyah.com.qbot;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.List;

import hanqis.com.qbot.Sample_algorithm;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MobotActivity extends IOIOActivity implements CameraBridgeViewBase.CvCameraViewListener2,
        MobotLooper.MobotDriver {

    private static final String TAG = "MobotActivity";
    private static final int LINE_THICKNESS = 5;
    private static final int POINT_THICKNESS = 2;
    private static final int LEFT = 1;
    private static final int RIGHT = 0;
    private SharedPreferences mSharedPref;
    private PortraitCameraView mOpenCvCameraView;
    private boolean mStatusConnected;

    private MobotLooper mobotLooper;
    private Sample_algorithm mAlgorithm = new Sample_algorithm();
    private ParameterFiltering mFilt;
    private TurnDetector mTurnDetector = new TurnDetector();

    private double[] mAngle = new double[2];
    private int mLineChoice = RIGHT;
    private double mAngleFinal = 0.0;
    private boolean mSplit = false;
    private int mTurnRight = 0;

    private double mTunning = 0;
    private double mSpeed = 0;
    private double mMaxSpeed = MobotLooper.MAX_SPEED;
    private ParameterBar mTestBar;
    
    /* need to be in [0,3) */
    private double mThreshold = 0.5;
    private int mSamplingPoints = 2000;
    private int mStdThreshold = 25;
    private int mDimension = 2;
    private int mCounter1 = 0;
    private int mCounter2 = 0;
    private int mSplitTimeCounter = 0;
    private int mSplitThreshold = 5;

    private int[] Turns2L = new int[]{LEFT,RIGHT,LEFT,LEFT,RIGHT,RIGHT,LEFT,LEFT,RIGHT,RIGHT,LEFT,LEFT};
    private int[] Turns2R = new int[]{RIGHT,LEFT,RIGHT,RIGHT,LEFT,LEFT,RIGHT,RIGHT,LEFT,LEFT,RIGHT,RIGHT};
    private int[] mTurn1 = new int[]{RIGHT,LEFT,RIGHT,LEFT,RIGHT,LEFT,RIGHT,LEFT,RIGHT,LEFT,RIGHT,LEFT};
    private int[] mTurn2 = Turns2L;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    //------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_mobot);

        // OpenCV
        mOpenCvCameraView = (PortraitCameraView) findViewById(R.id.video_surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        addSpeedBar();
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mThreshold = mSharedPref.getFloat(MainActivity.PREF_THRESHOLD, -1);
        Log.i(TAG, "Thresh " + mThreshold);

        mFilt = new ParameterFiltering(getP(), getI(), getD());
        mStdThreshold = (int) mSharedPref.getFloat(MainActivity.PREF_STD_THRESHOLD,50);
        mDimension = (int) mSharedPref.getFloat(MainActivity.PREF_DIMENSION, 2);
        mTurnRight = (int) mSharedPref.getFloat(MainActivity.PREF_TUNNING,0);
        mTurn2 = mTurnRight == 1 ? Turns2R : Turns2L;
        mSplitThreshold = (int) mSharedPref.getFloat(MainActivity.PREF_STD_SPLITTH,2);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        Log.i(TAG, "Created Looper");
        mobotLooper = new MobotLooper(this,this);
        return mobotLooper;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    //------------ Img Processing -------------------------------------------

    @Override public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img = inputFrame.rgba();
        // mAngle = eAlgorithm.findAngle(img);
        mAngle = mAlgorithm.Sampling(img,mDimension,mThreshold,mSamplingPoints,mStdThreshold);
        // store the previous value
        boolean splitPrev = mSplit;
        mSplit = mAlgorithm.getSplit();
        int numHills = 1;//getNumHillPassed();

        //mSplitTimeCounter needs to be at least 5 for the two lines to become available
        if (mSplit) {
            mSplitTimeCounter++;
        } else {
            //Reset mSplitTimeCounter when there is no two lines appear.
            mSplitTimeCounter = 0;
            //And be ready for the next turn.
            if (splitPrev && !mSplit && numHills == 2) mCounter2++;
            if (splitPrev && !mSplit && numHills == 1) mCounter1++;
        }
        boolean splitAtHill1 = mSplit && numHills == 1 && mSplitTimeCounter >= mSplitThreshold;
        boolean splitAtHill2 = mSplit && numHills == 2 && mSplitTimeCounter >= mSplitThreshold;

        if (splitAtHill2) {
            // Splits after hill 2, choice section
            if (mCounter2 >= mTurn2.length) {
                mLineChoice = mTurnRight;
            } else {
                mLineChoice = mTurn2[mCounter2];
            }
            mAngleFinal = mAngle[mLineChoice];
        } else if (splitAtHill1) {
            // Splits after hill 1 for error correction
            if (mCounter1 >= mTurn1.length)
                mLineChoice = LEFT;
            else {
                mLineChoice = mTurn1[mCounter1];
            }
            mAngleFinal = mAngle[mLineChoice];
        } else {
            // No split, only one line
            mAngleFinal = mAngle[0];
        }

        // Filtering
        mAngleFinal = mFilt.filter(mAngleFinal);

        // Detect turn
        TurnDetector.Turn turn = mTurnDetector.updateTurn(mAngleFinal);

        // Update UI
        updateAngle(mAngleFinal);

        if (numHills == 2) {
            updateAlgParams(mAlgorithm.getResStd(), numHills, mCounter2, turn);
        } else {
            updateAlgParams(mAlgorithm.getResStd(), numHills, mCounter1, turn);
        }

        addSelectedPoints(img,(splitAtHill1 || splitAtHill2));
        addFoundLines(img, mAngleFinal, mAngle, (splitAtHill1 || splitAtHill2));
        return img;
    }

    private void addFoundLines(Mat img, double angleFinal, double[] anglesFound, boolean split) {
        int height = img.height();
        int width = img.width();
        Point centerPt = new Point(width / 2, height);
        Point p0 = getEndPoint(angleFinal,width, height, height);
        Point p1 = getEndPoint(anglesFound[0],width, height, height/3);
        Point p2 = getEndPoint(anglesFound[1],width, height, height/3);
        int red = Color.RED;
        int blue = Color.CYAN;
        int green = Color.GREEN;

        // Display green is choice line
        if (mLineChoice == RIGHT) {
            Core.line(img, centerPt, p1, new Scalar(Color.red(green), Color.blue(green), Color.green(green)), LINE_THICKNESS);
            Core.line(img, centerPt, p2, new Scalar(Color.red(blue), Color.blue(blue), Color.green(blue)), LINE_THICKNESS);
        } else {
            Core.line(img, centerPt, p2, new Scalar(Color.red(green), Color.blue(green), Color.green(green)), LINE_THICKNESS);
            Core.line(img, centerPt, p1, new Scalar(Color.red(blue), Color.blue(blue), Color.green(blue)), LINE_THICKNESS);
        }

        // Display filtered angle line
        Core.line(img, centerPt, p0, new Scalar(Color.red(red), Color.blue(red), Color.green(red)), LINE_THICKNESS);
    }

    private Point getEndPoint(double angle, int w, int h, int len) {
        return new Point(w / 2 + len * Math.sin(Math.toRadians(angle)),
                h - len*Math.cos(Math.toRadians(angle)));
    }

    private void addSelectedPoints(Mat img, boolean split){
        int green = Color.GREEN;
        int yellow = Color.YELLOW;
        Scalar sGreen = new Scalar(Color.red(green),Color.green(green),Color.blue(green));
        Scalar sYellow = new Scalar(Color.red(yellow),Color.green(yellow),Color.blue(yellow));

        if (!split) {
          List<Point> pts = mAlgorithm.getSelectedPoints().get(0);
          for (int i = 0;i < pts.size();i++){
              Core.circle(img,pts.get(i),2,sYellow,POINT_THICKNESS);
          }
        } else {
          List<Point> ptsA = mAlgorithm.getSelectedPoints().get(0);
          List<Point> ptsB = mAlgorithm.getSelectedPoints().get(1);

          if (mLineChoice == RIGHT) {
              for (int i = 0; i < ptsA.size(); i++) {
                  Core.circle(img, ptsA.get(i), 2, sYellow, POINT_THICKNESS);
              }
              for (int i = 0; i < ptsB.size(); i++) {
                  Core.circle(img, ptsB.get(i), 2, sGreen, POINT_THICKNESS);
              }
          } else {
              for (int i = 0; i < ptsA.size(); i++) {
                  Core.circle(img, ptsA.get(i), 2, sGreen, POINT_THICKNESS);
              }
              for (int i = 0; i < ptsB.size(); i++) {
                  Core.circle(img, ptsB.get(i), 2, sYellow, POINT_THICKNESS);
              }
          }

        }
    }

    //------------ Speed Bar -----------------------------------------------

    private void addSpeedBar() {
        SeekBar speedBar = (SeekBar) findViewById(R.id.speed_bar);
        mSpeed = updateSpeed(speedBar.getProgress(), speedBar.getMax());
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSpeed = updateSpeed(seekBar.getProgress(), seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSpeed = updateSpeed(seekBar.getProgress(), seekBar.getMax());
            }
        });
    }

    private double updateSpeed(int val, int maxVal) {
        final double speed = 1.0 * val / maxVal;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView msg = (TextView) findViewById(R.id.speed_val);
                msg.setText(String.format("%.2f", speed));
            }
        });
        return speed;
    }

    //------------ Driving Mobot -------------------------------------------

    @Override
    public double getDriveAngle() {
        return mAngleFinal;
    }

    @Override
    public double getDriveSpeed() {
        return mSpeed;
    }

    @Override
    public double getTunning() { return mSharedPref.getFloat(MainActivity.PREF_TUNNING, 0); }

    @Override
    public double getMaxSpeed() { return mSharedPref.getFloat(MainActivity.PREF_MAX_SPEED, 0); }

    public double getP() { return mSharedPref.getFloat(MainActivity.PREF_PID_P, 0); }
    public double getI() { return mSharedPref.getFloat(MainActivity.PREF_PID_I, 0); }
    public double getD() { return mSharedPref.getFloat(MainActivity.PREF_PID_D, 0); }

    @Override
    public void setStatusOnline(boolean status) {
        mStatusConnected = status;
        if (status) {
            updateMsg(getString(R.string.connected_msg));
        } else {
            updateMsg(getString(R.string.not_connected_msg));
        }
    }

    //------------ Updating View -------------------------------------------

    private void updateAngle(final Double a){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView angle = (TextView) findViewById(R.id.angle_test);
                angle.setText(String.format("Angle: %.2f", a));
            }
        });
    }

    private void updateAlgParams(final double st, final int hills, final int count,
                                 final TurnDetector.Turn turn){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView angle = (TextView) findViewById(R.id.std_test);
                angle.setText(String.format("Std:%.2f. Hills:%d C:%d %s",st,hills,count,turn));
            }
        });
    }

    private void updateMsg(final String m) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView msg = (TextView) findViewById(R.id.mobot_messages);
                msg.setText(m);
            }
        });
    }

    // hill detection
    private int getNumHillPassed() {
        return mobotLooper.numHillsPassed;
    }

    private boolean isOnHill() {
        return mobotLooper.onHill;
    }

}