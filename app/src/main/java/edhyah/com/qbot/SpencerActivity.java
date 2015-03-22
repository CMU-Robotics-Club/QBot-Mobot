package edhyah.com.qbot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


public class SpencerActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "SpencerActivity";
    private static final double HOUGH_RHO = 1;
    private static final double HOUGH_THETA = Math.PI / 180;
    private static final int HOUGH_THRESH = 10;
    private static final double HOUGH_MIN_LEN = 15;
    private static final double HOUGH_MAX_LINE_GAP = 90;
    private static final Scalar HOUGH_LN_CLR = new Scalar(0);
    private PortraitCameraView mOpenCvCameraView;

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
        setContentView(R.layout.activity_spencer);
        mOpenCvCameraView = (PortraitCameraView) findViewById(R.id.HelloOpenCvView);
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

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Find lines
        Mat lines = new Mat();
        Mat out = inputFrame.gray();
        Imgproc.HoughLinesP(out, lines, HOUGH_RHO, HOUGH_THETA, HOUGH_THRESH,
                HOUGH_MIN_LEN, HOUGH_MAX_LINE_GAP);

        // Draw lines
        Log.i(TAG, "Size:" + lines.size().width + ", " + lines.size().height);
        double[] cords;
        for (int i = 0; i < lines.size().width; i++) {
            cords = lines.get(1,i);
            Log.i(TAG, cords.toString());
            //Point p1 = new Point(cords[0], cords[1]);
            //Point p2 = new Point(cords[2], cords[3]);
            //Core.line(out, p1, p2, HOUGH_LN_CLR);
        }

        return out;
    }
}
