package hanqis.com.qbot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by LaserSH on 3/22/15.
 */
public class Sample_algorithm {

    private static int amount;
    private Regression_algorithm mRegress = new Regression_algorithm();
    private List<Point> selectedPoints = new ArrayList<Point>();
    private static int scale = 2;


    public double Sampling(Mat vidRgb,double threshold,int sample) {
        int vidHeight = vidRgb.height();
        int vidWidth = vidRgb.width();
        setAmount(sample);

        int adjHeight = vidHeight / scale;
        int adjWidth = vidWidth / scale;
        Mat vidHsv = new Mat();
        Mat vidRgbAdj = new Mat();
        Size sz = new Size(adjWidth, adjHeight);
        Imgproc.resize(vidRgb, vidRgbAdj, sz);
        Imgproc.cvtColor(vidRgbAdj, vidHsv, Imgproc.COLOR_RGB2HSV);

        List<Mat> hsv_planes = new ArrayList<Mat>();
        Core.split(vidHsv, hsv_planes);

        Mat vidHue = hsv_planes.get(0);
        int[] randompoints = new int[amount];
        for (int i = 0; i < amount; i++) {
            Random rn = new Random();
            randompoints[i] = rn.nextInt(adjHeight * adjWidth);
        }

        int[] xsub = ind2subx(adjWidth, amount, randompoints);
        int[] ysub = ind2suby(adjWidth, amount, randompoints);

        ArrayList selectedpts = new ArrayList<Integer>();
        for (int i = 0; i < amount; i++) {
            if (vidHue.get(ysub[i],xsub[i])[0] > threshold) {
                selectedpts.add(randompoints[i]);
            }
        }
        int[] spoints = convertIntegers(selectedpts);
        int[] sxsub = ind2subx(adjWidth, spoints.length, spoints);
        int[] sysub = ind2suby(adjWidth, spoints.length, spoints);

        Mat err1 = mRegress.Regression(sxsub, sysub,1,false);
        Mat err2 = mRegress.Regression(sxsub, sysub, 2, false);
        double std1 = stdofcol(err1,sxsub.length);
        double std2 = stdofcol(err2,sxsub.length);

        int[] newpoints1 = removeoutliers(spoints, err1,std1,sxsub.length, 2);
        int[] newpoints2 = removeoutliers(spoints, err2,std2,sxsub.length, 2);

        int[] nxsub1 = ind2subx(adjWidth, newpoints1.length, newpoints1);
        int[] nysub1 = ind2suby(adjWidth, newpoints1.length, newpoints1);
        int[] nxsub2 = ind2subx(adjWidth, newpoints2.length, newpoints2);
        int[] nysub2 = ind2suby(adjWidth, newpoints2.length, newpoints2);

        setSelectedPoints(nxsub1,nysub1);

        Mat res1 = mRegress.Regression(nxsub1,nysub1,1,true);
        Mat res2 = mRegress.Regression(nxsub2, nysub2, 2, true);

        double angle1 = Math.tan(res1.get(1, 0)[0]);
        double angle2 = calcAngleQuad(res2,adjWidth,adjHeight);

        return angle1;

    }

    private int[] ind2subx(int width, int length, int[] loc){
        int[] res = new int[length];
        for (int i = 0; i < length; i++){
            res[i] = loc[i] % width;
        }
        return res;
    }

    private int[] ind2suby(int width, int length, int[] loc){
        int[] res = new int[length];
        for (int i = 0; i < length; i++){
            res[i] = loc[i] / width;
        }
        return res;
    }

    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }
    private double stdofcol(Mat col, int length){
        float total = 0;
        for (int i = 0; i < length; i++){
            total += col.get(i,0)[0];
        }
        double mean = total / length;
        double t = 0;
        for (int i = 0; i < length; i++){
            double err = Math.pow(col.get(i,0)[0] - mean,2.0);
            t += err;
        }
        return Math.sqrt(t / (length - 1));
    }

    private int[] removeoutliers(int[] data, Mat err,
                                 double std, int size, int k){
       ArrayList res = new ArrayList<Integer>();
       for (int i = 0; i < size; i++){
           if (err.get(i,0)[0] <= k * std){
               res.add(data[i]);
           }
       }
       return convertIntegers(res);
    }



    private double calcAngleQuad(Mat res, int width,int height){
        double c = res.get(0,0)[0];
        double b = res.get(1,0)[0];
        double a = res.get(2,0)[0];
        double yd = height/2;
        double xd = a*yd*yd + b*yd + c;
        double k = (xd - width/2) / yd;
        return Math.tan(k);
    }

    public List<Point> getSelectedPoints() {
        return selectedPoints;
    }

    public void setSelectedPoints(int[] xsub,int[] ysub){
        for (int i = 0; i < xsub.length; i++){
            Point p = new Point(xsub[i] * scale,ysub[i] * scale);
            selectedPoints.add(p);
        }
    }

    private void setAmount(int sample){
        amount = sample;
    }
}
