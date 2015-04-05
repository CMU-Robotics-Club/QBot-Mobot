package edhyah.com.qbot;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
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
    private PortraitCameraView mOpenCvCameraView; // TODO add a turn off button for when not debugging

    private Sample_algorithm mAlgorithm = new Sample_algorithm();
    private double mAngle = 0;
    /* need to be in [0,180) */
    private double mThreshold = 20;
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
        mOpenCvCameraView = (PortraitCameraView) findViewById(R.id.video_surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
        mAngle = mAlgorithm.Sampling(img,mThreshold,mSamplingPoints);
        updateAngle(mAngle);
        updateMsg("Online");

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
        return 1.0;
    }

    private void updateAngle(final Double a){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView angle = (TextView) findViewById(R.id.angle_test);
                angle.setText(getString(R.string.angle_front) + a);
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