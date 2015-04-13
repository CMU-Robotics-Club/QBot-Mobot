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
    private List<Point> selectedPointsA = new ArrayList<Point>();
    private List<Point> selectedPointsB = new ArrayList<>();

    private List<Integer> selectedValue = new ArrayList<>();
    private static int scale = 2;
    private double mStd = 0.0;
    private double mThreshold = -1.0;
    private double mMean = 90.0;


    private double resStd = 0.0;
    //Cut off value, 1/n of the screen will not be selected.
    //private int mCut = 3;
    //private double[] std = new double[2];

    private boolean Split = false;


    public double[] Sampling(Mat vidRgb,int dimension,double threshold,int sample,int stdThreshold) {

        double[] angleRes = new double[2];

        int vidHeight = vidRgb.height();
        int vidWidth = vidRgb.width();
        setAmount(sample);
        selectedPointsA = new ArrayList<>();
        selectedPointsB = new ArrayList<>();

        //Initialize threshold value
        //if (Math.abs(mThreshold - (0.5)) < 0.00001) {
        mThreshold = threshold;
        //}

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

        /*
        int newHeight = adjHeight - adjHeight / mCut;
        int numOfPts = adjWidth * newHeight;
        Rect rect = new Rect(0,adjHeight / mCut,adjWidth,newHeight);
        Mat cropped = new Mat(vidValue,rect);
        */

        MatOfDouble mStdMat = new MatOfDouble();
        MatOfDouble mMeanMat = new MatOfDouble();
        Core.meanStdDev(vidValue,mMeanMat,mStdMat);
        mMean = mMeanMat.get(0,0)[0];
        mStd = mStdMat.get(0,0)[0];
        int[] randompoints = new int[amount];
        for (int i = 0; i < amount; i++) {
            Random rn = new Random();
            randompoints[i] = rn.nextInt(adjHeight * adjWidth);
        }

        /*
        if(!dynamicSelection(adjHeight,adjWidth,randompoints,xsub,ysub,cropped)){
           return 0.0;
        }*/

        int[] xsub = ind2subx(adjWidth, amount, randompoints);
        int[] ysub = ind2suby(adjWidth, amount, randompoints);

        List <Integer> res = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            if ((vidValue.get(ysub[i],xsub[i])[0] - mMean) > mThreshold * mStd) {
                res.add(randompoints[i]);
               }
        }

        if (res.size() < 10) {
            angleRes[0] = 0.0;
            angleRes[1] = 0.0;
            return angleRes;
        }
        setSelectedValue(res);

        int[] spoints = convertIntegers(res);
        Sample_help regressRes = new Sample_help(spoints,adjWidth,dimension,scale);
        setResStd(regressRes.getStandardDeviation());

        if ((int) regressRes.getStandardDeviation() >= stdThreshold) {
           Split = true;
           int[][] Npts = regressRes.splitIntoTwo(regressRes.getNpts(),
                   regressRes.getErr2(),
                   regressRes.getNpts().length, adjWidth);
            Sample_help regressU = new Sample_help(Npts[0],adjWidth,dimension,scale);
            Sample_help regressD = new Sample_help(Npts[1],adjWidth,dimension,scale);
            selectedPointsA = regressU.getSelectedPoints();
            selectedPointsB = regressD.getSelectedPoints();

            angleRes[0] = calcAngle(regressU.getRes(),dimension,adjWidth,adjHeight);
            angleRes[1] = calcAngle(regressD.getRes(),dimension,adjWidth,adjHeight);
        } else {
            double angle = calcAngle(regressRes.getRes(), dimension, adjWidth, adjHeight);
            selectedPointsA = regressRes.getSelectedPoints();
            angleRes[0] = angle;
            angleRes[1] = 0.0;
        }

        return angleRes;
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
        int[] res = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < res.length; i++)
        {
            res[i] = iterator.next().intValue();
        }
        return res;
    }




    private double calcAngle(double[] res,int dimension, int width,int height){
        double yd = height/2;
        double xd = 0.0;
        for(int i = 0; i <= dimension; i++){
            xd += Math.pow(yd,i) * res[i];
        }
        double k = (xd - width/2) / yd;
        double angle = Math.atan(k) * 180.0 / Math.PI;
        return angle;
    }

    public List<List<Point>> getSelectedPoints() {
        List<List<Point>> res = new ArrayList<>();
        res.add(selectedPointsA);
        res.add(selectedPointsB);
        return res;
    }

    private List<Integer> getSelectedValue() { return selectedValue;}

    private void setSelectedValue(List<Integer> input){ selectedValue = input;}

    public double getThreshold() {return mThreshold;}

    private void setAmount(int sample){ amount = sample;}

    public boolean getSplit(){ return Split;}

    public double getResStd() {
        return resStd;
    }

    public void setResStd(double resStd) {
        this.resStd = resStd;
    }
/*
    private boolean dynamicSelection(int wid,int hei,int[] randPts,int[] x, int[]y, Mat V){

        List<Integer> selectedLoc = new ArrayList<Integer>();

        int mainxa = wid / 3;
        int mainxb = 2 * wid / 3;
        int mainya = 2 * hei / 3;
        int mainyb = hei;
        int count = 5;

        selectedLoc = selectionHelp(mainxa,mainxb,mainya,mainyb,randPts,x,y,V,count,0);
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
    */

}
