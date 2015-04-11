package edhyah.com.qbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
    private static final String PREF_KEY_TUNNING = "edhyah.com.qbot.MobotActivity.PREF_KEY_TUNNING";
    private static final float DEFAULT_TUNNING = 0;
    private static final int POINT_THICKNESS = 2;
    private PortraitCameraView mOpenCvCameraView; // TODO add a turn off button for when not debugging
    private boolean mStatusConnected;

    // private EdgeDetection eAlgorithm = new EdgeDetection();
    private Sample_algorithm mAlgorithm = new Sample_algorithm();
    private double mAngle = 0;

    private double mTunning = 0;
    private double mSpeed = 0;
    
    /* need to be in [0,3) */
    private double mThreshold = 0.5;
    private int mSamplingPoints = 2000;

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

        addTunningBar();
        addSpeedBar();
        addThresholdBar();
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
        mAngle = mAlgorithm.Sampling(img,mThreshold,mSamplingPoints);
        //Threshold value will show at the end threshold bar.
        //updateThreshold(0,0);
        updateAngle(mAngle);

        addSelectedPoints(img);
        addFoundLine(img, mAngle);
        return img;
    }

    private void addFoundLine(Mat img, double mAngle) {
        int height = img.height();
        int width = img.width();
        Point p1 = new Point(width/2,height);
        Point p2 = new Point(width/2 + height*Math.sin(Math.toRadians(mAngle)),
                height*(1 - Math.cos(Math.toRadians(mAngle))));
        int red = Color.RED;
        Core.line(img, p1, p2, new Scalar(Color.red(red), Color.blue(red), Color.green(red)), LINE_THICKNESS);
    }

    private void addSelectedPoints(Mat img){
        List<Point> pts = mAlgorithm.getSelectedPoints();
        int blue = Color.BLUE;
        Scalar sBlue = new Scalar(Color.red(blue),Color.blue(blue),Color.green(blue));
        for (int i = 0;i < pts.size();i++){
            Core.circle(img,pts.get(i),2,sBlue,POINT_THICKNESS);
        }
    }

    //------------ Tuning Mobot -------------------------------------------

    private void addTunningBar() {
        SeekBar tunningBar = (SeekBar) findViewById(R.id.tuningParams);
        tunningBar.setProgress(loadTunning(tunningBar.getMax()));
        updateTunning(tunningBar.getProgress(), tunningBar.getMax());
        tunningBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTunning = updateTunning(progress, seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Store and save value
                mTunning = calcTunning(seekBar.getProgress(), seekBar.getMax());
                saveTunning(mTunning);
            }
        });
    }

    private double calcTunning(int value, int maxValue) {
        return (2.0 * value / maxValue) - 1; // Map to -1:1
    }

    private void saveTunning(double val) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_KEY_TUNNING, (float)val);
        editor.commit();
    }

    private int loadTunning(int maxVal) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        double tunning = sharedPref.getFloat(PREF_KEY_TUNNING, DEFAULT_TUNNING);
        return (int)Math.round(((tunning + 1) / 2) * maxVal);
    }

    private double updateTunning(int val, int maxVal) {
        final double progress = calcTunning(val, maxVal);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView msg = (TextView) findViewById(R.id.tunning_value);
                msg.setText(String.format("%.2f", progress));
            }
        });
        return progress;
    }

    //------------ Speed Bar -----------------------------------------------

    private void addSpeedBar() {
        SeekBar speedBar = (SeekBar) findViewById(R.id.speedbar);
        updateTunning(speedBar.getProgress(), speedBar.getMax());
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

    //------------ Threshold Bar -----------------------------------------------

    private void addThresholdBar() {
        SeekBar thresholdBar = (SeekBar) findViewById(R.id.threshold_bar);
        updateThreshold(thresholdBar.getProgress(), thresholdBar.getMax());
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateThreshold(seekBar.getProgress(), seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateThreshold(seekBar.getProgress(), seekBar.getMax());
            }
        });
    }



    private double updateSpeed(int val, int maxVal) {
        final double speed = 1.0 * val / maxVal;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView msg = (TextView) findViewById(R.id.speed_value);
                msg.setText(String.format("%.2f", speed));
            }
        });
        return speed;
    }

    private void updateThreshold(int val, int maxVal) {
        mThreshold = 3.0 * val / 100;
        //mThreshold = mAlgorithm.getThreshold();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView msg = (TextView) findViewById(R.id.threshold_value);
                msg.setText(String.format("%.2f", mThreshold));
            }
        });

    }

    //------------ Driving Mobot -------------------------------------------

    @Override
    public double getDriveAngle() {
        return mAngle;
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
    public double getTunning() { return mTunning; }

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
                angle.setText(String.format(getString(R.string.angle_front) + "%.2f", a));
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