package hanqis.com.qbot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
    private List<Integer> selectedValue = new ArrayList<>();
    private static int scale = 2;
    private double mStd = 0.0;
    private double mThreshold = -1.0;
    private double mMean = 90.0;
    //Cut off value, 1/n of the screen will not be selected.
    private int mCut = 3;


    public double Sampling(Mat vidRgb,double threshold,int sample) {
        int vidHeight = vidRgb.height();
        int vidWidth = vidRgb.width();
        setAmount(sample);
        selectedPoints.clear();

        //Initialize threshold value
        if (Math.abs(mThreshold - (0.5)) < 0.00001) {
            mThreshold = threshold;
        }

        int adjHeight = vidHeight / scale;
        int adjWidth = vidWidth / scale;
        Mat vidHsv = new Mat();
        Mat vidRgbAdj = new Mat();
        Size sz = new Size(adjWidth, adjHeight);
        Imgproc.resize(vidRgb, vidRgbAdj, sz);
        Imgproc.cvtColor(vidRgbAdj, vidHsv, Imgproc.COLOR_RGB2HSV);

        List<Mat> hsv_planes = new ArrayList<Mat>();
        Core.split(vidHsv, hsv_planes);

        Mat vidValue = hsv_planes.get(2);

        int newHeight = adjHeight - adjHeight / mCut;
        int numOfPts = adjWidth * newHeight;
        Rect rect = new Rect(0,adjHeight / mCut,adjWidth,newHeight);
        Mat cropped = new Mat(vidValue,rect);
        MatOfDouble mStdMat = new MatOfDouble();
        MatOfDouble mMeanMat = new MatOfDouble();
        Core.meanStdDev(cropped,mMeanMat,mStdMat);
        mMean = mMeanMat.get(0,0)[0];
        mStd = mStdMat.get(0,0)[0];
        int[] randompoints = new int[amount];
        for (int i = 0; i < amount; i++) {
            Random rn = new Random();
            randompoints[i] = rn.nextInt(numOfPts);
        }

        int[] xsub = ind2subx(adjWidth, amount, randompoints);
        int[] ysub = ind2suby(adjWidth, amount, randompoints);

        if(!dynamicSelection(adjHeight,adjWidth,randompoints,xsub,ysub,cropped)){
           return 0.0;
        }
        int[] spoints = convertIntegers(getSelectedValue());
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

        setSelectedPoints(nxsub1,nysub1,adjHeight / mCut);

        Mat res1 = mRegress.Regression(nxsub1,nysub1,1,true);
        Mat res2 = mRegress.Regression(nxsub2,nysub2,2, true);

        double angle1 = Math.atan(res1.get(1, 0)[0]) * 180.0 / Math.PI;
        double angle2 = calcAngleQuad(res2,adjWidth,adjHeight);

        return angle2;

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

    private boolean dynamicSelection(int wid,int hei,int[] randPts,int[] x, int[]y, Mat V){

        List<Integer> selectedLoc = new ArrayList<Integer>();

        int mainxa = wid / 3;
        int mainxb = 2 * wid / 3;
        int mainya = 2 * hei / 3;
        int mainyb = hei;
        int count = 5;

        selectedLoc = selectionHelp(mainxa,mainxb,mainya,mainyb,randPts,x,y,V,count,0);
        /* If not enough points is selected, return value 0.0 */
        if (selectedLoc.size() < 10) {
            return false;
        }
        setSelectedValue(selectedLoc);
        return true;
    }

    private List<Integer> selectionHelp(int mxa, int mxb, int mya, int myb,
                                        int[] rand, int[] x, int[] y, Mat V,int max,int count){
        List <Integer> res = new ArrayList<>();
        List <Integer> main = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            if ((V.get(y[i],x[i])[0] - mMean) > mThreshold * mStd) {
                res.add(rand[i]);
               if (mya <= y[i] && y[i] <= myb &&
                   mxa <= x[i] && x[i] <= mxb){
                   main.add(rand[i]);
               }
            }
        }
        while(count < max){
            count++;
            if (res.size() >= amount / 4){
                if (mThreshold < 0.2) mThreshold = 0.2;
                else mThreshold = mThreshold * 1.25 * (1- (double) count * 0.01);
                selectionHelp(mxa,mxb,mya,myb,rand,x,y,V,max,count);
            } else if (main.size() <= 20){
                if (mThreshold > 1) mThreshold = 1;
                else mThreshold = mThreshold * 0.8 * (1+ (double) count * 0.01);
                selectionHelp(mxa,mxb,mya,myb,rand,x,y,V,max,count);
            }
        }
        return res;
    }

    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] res = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < res.length; i++)
        {
            res[i] = iterator.next().intValue();
        }
        return res;
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
        return Math.atan(k) * 180.0 / Math.PI;
    }

    public List<Point> getSelectedPoints() {
        return selectedPoints;
    }

    public void setSelectedPoints(int[] xsub,int[] ysub,int yoff){
        selectedPoints.clear();
        for (int i = 0; i < xsub.length; i++){
            Point p = new Point(xsub[i] * scale,(ysub[i] + yoff) * scale);
            selectedPoints.add(p);
        }
    }

    private List<Integer> getSelectedValue() { return selectedValue;}

    private void setSelectedValue(List<Integer> input){ selectedValue = input;}

    public double getThreshold() {return mThreshold;}

    private void setAmount(int sample){
        amount = sample;
    }
}
