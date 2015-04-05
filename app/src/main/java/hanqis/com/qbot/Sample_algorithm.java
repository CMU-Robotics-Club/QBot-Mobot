package hanqis.com.qbot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

    private static int amount = 2000;

    public double Sampling(Mat vidRgb) {
        int vidHeight = vidRgb.height();
        int vidWidth = vidRgb.width();

        int scale = 2;
        // The hue value of opencv is [0,180)
        double threshold = 0;

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

        ArrayList selectedpoints = new ArrayList<Integer>();
        for (int i = 0; i < amount; i++) {
            if (vidHue.get(ysub[i],xsub[i])[0] > threshold) {
                selectedpoints.add(randompoints[i]);
            }
        }
        int[] spoints = convertIntegers(selectedpoints);
        int[] sxsub = ind2subx(adjWidth, spoints.length, spoints);
        int[] sysub = ind2suby(adjWidth, spoints.length, spoints);

        Mat err1 = linearRegression(sxsub, sysub, false);
        Mat err2 = quadRegression(sxsub, sysub, false);
        double std1 = stdofcol(err1,sxsub.length);
        double std2 = stdofcol(err2,sxsub.length);

        int[] newpoints1 = removeoutliers(spoints, err1,std1,sxsub.length, 2);
        int[] newpoints2 = removeoutliers(spoints, err2,std2,sxsub.length, 2);

        int[] nxsub1 = ind2subx(adjWidth, newpoints1.length, newpoints1);
        int[] nysub1 = ind2suby(adjWidth, newpoints1.length, newpoints1);
        int[] nxsub2 = ind2subx(adjWidth, newpoints2.length, newpoints2);
        int[] nysub2 = ind2suby(adjWidth, newpoints2.length, newpoints2);

        Mat res1 = linearRegression(nxsub1, nysub1, true);
        Mat res2 = quadRegression(nxsub2, nysub2, true);

        double angle1 = Math.tan(res1.get(1, 0)[0]);
        double angle2 = calcAngleQuad(res2,adjWidth,adjHeight);

        return (angle1 + angle2) / 2;

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

    private Mat linearRegression (int[] xdata,int[] ydata,boolean fin){
        Mat res = new Mat(ydata.length,2,CvType.CV_32FC1);
        for (int i = 0; i < ydata.length; i++){
            res.put(i,1,new float[]{ydata[i]});
            res.put(i,0,new float[]{1});
        }
        Mat temp = new Mat(2,2,CvType.CV_32FC1);
        /* Mat B = t.mmul(convert1dtocol(xdata,xdata.length)); */
        Mat B = new Mat(2,1,CvType.CV_32FC1);
        Mat X = convert1dtocol(xdata,xdata.length);
        Core.gemm(res.t(),X,1,Mat.zeros(2,1,CvType.CV_32FC1),0,B,0);
        Core.gemm(res.t(),res,1,Mat.zeros(2,2,CvType.CV_32FC1),0,temp,0);
        Mat para = new Mat(2,1,CvType.CV_32FC1);
        /*Core.solve.solve(temp,B); */
        Core.solve(temp,B,para);
        if (fin) return para;
        Mat X1 = new Mat(xdata.length,1,CvType.CV_32FC1);
        Core.gemm(res,para,1,Mat.zeros(ydata.length,1,CvType.CV_32FC1),0,X1,0);
        Mat err = Mat.zeros(xdata.length,1,CvType.CV_32FC1);
        Core.subtract(X,X1,err);
        return err;
        /* return res.sub(res.mmul(para)); */
    }

    private Mat quadRegression (int[] xdata,int[] ydata, boolean fin){
        Mat res = new Mat(ydata.length,3, CvType.CV_32FC1);
        for (int i = 0; i < ydata.length; i++){
            res.put(i,0,new float[]{1});
            res.put(i,1,new float[]{ydata[i]});
            res.put(i,2,new float[]{(ydata[i] * ydata[i])});
        }
        Mat temp = new Mat(3,3,CvType.CV_32FC1);
        /* Mat B = t.mmul(convert1dtocol(xdata,xdata.length)); */
        Mat B = new Mat(3,1,CvType.CV_32FC1);
        Mat X = convert1dtocol(xdata,xdata.length);
        Core.gemm(res.t(),X,1,Mat.zeros(3,1,CvType.CV_32FC1),0,B,0);
        Core.gemm(res.t(),res,1,Mat.zeros(3,3,CvType.CV_32FC1),0,temp,0);
        Mat para = new Mat(3,1,CvType.CV_32FC1);
        /*Core.solve.solve(temp,B); */
        Core.solve(temp,B,para);
        if (fin) return para;
        Mat X1 = new Mat(xdata.length,1,CvType.CV_32FC1);
        Core.gemm(res,para,1,Mat.zeros(ydata.length,1,CvType.CV_32FC1),0,X1,0);
        Mat err = Mat.zeros(xdata.length,1,CvType.CV_32FC1);
        Core.subtract(X,X1,err);
        return err;
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

    private Mat convert1dtocol(int[] m,int width){
        Mat temp = new Mat(width,1,CvType.CV_32FC1);
        for (int i = 0; i < width; i++) {
            temp.put(i,0,new float[]{m[i]});
        }
        return temp;
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
}
