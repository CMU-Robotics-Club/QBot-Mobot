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

import hanqis.com.qbot.Sample_algorithm;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MobotActivity extends IOIOActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MobotActivity";
    private PortraitCameraView mOpenCvCameraView; // TODO add a turn off button for when not debugging

    private Sample_algorithm algo = new Sample_algorithm();
    private double angle;

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
        return null; // TODO create custom looper object that takes theta input and drives the mobot, also stops, calibrates
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    //------------ Img Processing -------------------------------------------

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img = inputFrame.rgba();
        int height = img.height();
        int width = img.width();
        angle = algo.Sampling(img);
        updateAngle(Double.toString(angle));


        Point p1 = new Point(height,width/2);
        Point p2 = new Point(height / 2, Math.atan(angle) * (height / 2));
        int red = Color.RED;
        Core.line(img,p1,p2,new Scalar(Color.red(red),Color.blue(red),Color.green(red)));
        // TODO update driving directions
        return inputFrame.rgba();
    }

    private void updateAngle(String s){
        TextView angle = (TextView) findViewById(R.id.angle_test);
        angle.setText(s);
    }
}