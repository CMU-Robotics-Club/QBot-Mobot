package edhyah.com.qbot;

// import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;

import java.util.ArrayList;

/**
 * Created by edwardahn on 30/3/15.
 */
public class EdgeDetection {

    int[] slices = {1,50,100};

    // changes vid in RGB to gray
    private Mat adjustVid(Mat vidRGB) {
        // scale
        int vidHeight = vidRGB.height();
        int vidWidth = vidRGB.width();
        int scale = 2;
        int adjHeight = vidHeight / scale;
        int adjWidth = vidWidth / scale;
        Mat vidRgbAdj = new Mat();
        Size sz = new Size(adjWidth, adjHeight);
        Imgproc.resize(vidRGB, vidRgbAdj, sz);
        // convert
        Mat vidG = new Mat();
        Imgproc.cvtColor(vidRGB,vidG,Imgproc.COLOR_RGB2GRAY);
        return vidG;
    }

    // returns array of points from arraylist of points
    private Mat arrayListToMat(ArrayList<Point> maxes) {
        Mat points = new Mat(maxes.size(),1,CvType.CV_32FC1);
        Point p;
        double[] vals;
        for (int i = 0; i < maxes.size(); i++) {
            p = maxes.get(i);
            vals = new double[2];
            vals[0] = p.x;
            vals[1] = p.y;
            points.put(i,1,vals);
        }
        return points;
    }

    // calculates angle
    public double findAngle(Mat vidRGB) {
        Mat vidG = adjustVid(vidRGB);
        ArrayList<Point> maxes = new ArrayList<>();
        Mat slice;
        Core.MinMaxLocResult mmr;
        for (int i = 0; i < slices.length; i++) {
            slice = vidG.row(slices[i]);
            mmr = Core.minMaxLoc(slice);
            maxes.add(mmr.maxLoc);
        }
        Mat points = arrayListToMat(maxes);
        Mat line = new Mat(4,1,CvType.CV_32FC1);
        Imgproc.fitLine(points,line,Imgproc.CV_DIST_L2,0,0.01,0.01);
        double vx = (line.get(0,0))[0];
        double vy = (line.get(1,0))[0];
        double angleDueEast = Math.tan(vy/vx);
        return 90 - angleDueEast;
    }
}
