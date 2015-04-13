package edhyah.com.qbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
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

    // private EdgeDetection eAlgorithm = new EdgeDetection();
    private Sample_algorithm mAlgorithm = new Sample_algorithm();
    private ParameterFiltering mFilt;
    private double[] mAngle = new double[2];
    private int mLineChoice = 0;
    private double mAngleFinal = 0.0;
    private boolean mSplit = false;

    private double mTunning = 0;
    private double mSpeed = 0;
    private double mMaxSpeed = MobotLooper.MAX_SPEED;
    private ParameterBar mTestBar;
    
    /* need to be in [0,3) */
    private double mThreshold = 0.5;
    private int mSamplingPoints = 2000;
    private int mStdThreshold = 25;
    private int mDimension = 2;
    private int counter = 0;

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
        mDimension = (int) mSharedPref.getFloat(MainActivity.PREF_DIMENSION, 2);

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
        return new MobotLooper(this, this);
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    //------------ Img Processing -------------------------------------------

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img = inputFrame.rgba();
        // mAngle = eAlgorithm.findAngle(img);
        mAngle = mAlgorithm.Sampling(img,mDimension,mThreshold,mSamplingPoints,mStdThreshold);
        mSplit = mAlgorithm.getSplit();

        if (!mSplit) {mAngleFinal = mAngle[0];}
        else {mAngleFinal = mAngle[mLineChoice];}


        // Filtering
        mAngleFinal = mFilt.filter(mAngleFinal);

        //(Outdated) Threshold value will show at the end threshold bar.
        //(Outdated) updateThreshold(0,0);
        updateAngle(mAngleFinal);

        updateStd(mAlgorithm.getResStd());

        addSelectedPoints(img,mSplit);
        addFoundLine(img,mAngle,mSplit);
        return img;
    }

    private void addFoundLine(Mat img, double[] angle, boolean split) {
        int height = img.height();
        int width = img.width();
        Point p1 = new Point(width / 2, height);
        Point p2 = new Point(width / 2 + height * Math.sin(Math.toRadians(angle[0])),
                       height * (1 - Math.cos(Math.toRadians(angle[0]))));
        int red = Color.RED;
        int blue = Color.CYAN;
        if (!split) {
            Core.line(img, p1, p2, new Scalar(Color.red(red), Color.blue(red), Color.green(red)), LINE_THICKNESS);
        } else {
            Point p3 = new Point(width / 2 + height * Math.sin(Math.toRadians(angle[1])),
                       height * (1 - Math.cos(Math.toRadians(angle[1]))));
            if (mLineChoice == LEFT) {
                Core.line(img, p1, p2, new Scalar(Color.red(red), Color.blue(red), Color.green(red)), LINE_THICKNESS);
                Core.line(img, p1, p3, new Scalar(Color.red(blue), Color.blue(blue), Color.green(blue)), LINE_THICKNESS);
            } else {
                Core.line(img, p1, p2, new Scalar(Color.red(blue), Color.blue(blue), Color.green(blue)), LINE_THICKNESS);
                Core.line(img, p1, p3, new Scalar(Color.red(red), Color.blue(red), Color.green(red)), LINE_THICKNESS);
            }
        }
    }

    private void addSelectedPoints(Mat img,boolean split){
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

          if (mLineChoice == LEFT) {
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

    /*(This code contains error) */
    /* private void updateAngle(String s){
        TextView angle = (TextView) findViewById(R.id.angle_test);
        angle.setText(s);
    }*/
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
                angle.setText(String.format(getString(R.string.angle_front) + "%.2f  ", a));
            }
        });
    }

    private void updateStd(final double st){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView angle = (TextView) findViewById(R.id.std_test);
                angle.setText(String.format(getString(R.string.std_front)
                                             + "%.2f.",st));
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

}