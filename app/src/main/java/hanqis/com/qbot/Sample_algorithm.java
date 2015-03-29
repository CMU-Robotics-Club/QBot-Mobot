package hanqis.com.qbot;

import org.jblas.FloatMatrix;
import org.jblas.Solve;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
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
        double threshold = 90;

        int adjHeight = vidHeight / scale;
        int adjWidth = vidWidth / scale;
        Mat vidHsv = new Mat();
        Mat vidRgbAdj = new Mat();
        Size sz = new Size(adjWidth, adjHeight);
        Imgproc.resize(vidRgb, vidRgbAdj, sz);
        Imgproc.cvtColor(vidHsv, vidRgbAdj, Imgproc.COLOR_BGR2HSV);

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

        ArrayList selectedpoints = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            if (vidHue.get(ysub[i],xsub[i])[0] > threshold) {
                selectedpoints.add(randompoints[i]);
            }
        }
        int[] spoints = arratlistToArray(selectedpoints);
        int[] sxsub = ind2subx(adjWidth, spoints.length, spoints);
        int[] sysub = ind2suby(adjWidth, spoints.length, spoints);

        FloatMatrix err1 = linearRegression(sxsub, sysub, false);
        FloatMatrix err2 = quadRegression(sxsub, sysub, false);
        double std1 = stdofcol(err1,sxsub.length);
        double std2 = stdofcol(err2,sxsub.length);

        int[] newpoints1 = removeoutliers(spoints, err1,std1,sxsub.length, 2);
        int[] newpoints2 = removeoutliers(spoints, err2,std2,sxsub.length, 2);

        int[] nxsub = ind2subx(adjWidth, newpoints1.length, newpoints1);
        int[] nysub = ind2suby(adjWidth, newpoints2.length, newpoints2);

        FloatMatrix res1 = linearRegression(nxsub, nysub, true);
        FloatMatrix res2 = quadRegression(nxsub, nysub, true);

        double angle1 = Math.tan((double) res1.get(1, 0));
        double angle2 = calcAngleQuad(res2,adjWidth,adjHeight);

        return (angle1 + angle2) / 2;

    }

    private int[] ind2subx(int width, int length, int[] loc){
        int[] res = new int[length];
        for (int i = 0; i < amount; i++){
            res[i] = loc[i] % width;
        }
        return res;
    }

    private int[] ind2suby(int width, int length, int[] loc){
        int[] res = new int[length];
        for (int i = 0; i < amount; i++){
            res[i] = loc[i] / width;
        }
        return res;
    }

    private FloatMatrix linearRegression (int[] xdata,int[] ydata,boolean fin){
        float[][] A = new float[xdata.length][2];
        for (int i = 0; i < xdata.length; i++){
            A[i][0] = 1;
            A[i][1] = ydata[i];
        }
        FloatMatrix res = new FloatMatrix(A);
        FloatMatrix para = Solve.solve(res.transpose().mmul(res),
                res.transpose().mmul(convert1dtocol(xdata,xdata.length)));
        if (fin) return para;
        return res.sub(res.mmul(para));
    }

    private FloatMatrix quadRegression (int[] xdata,int[] ydata, boolean fin){
        float[][] A = new float[xdata.length][3];
        for (int i = 0; i < xdata.length; i++){
            A[i][0] = 1;
            A[i][1] = (float) ydata[i];
            A[i][2] = A[i][1] * A[i][1];
        }
        FloatMatrix res = new FloatMatrix(A);
        FloatMatrix para = Solve.solve(res.transpose().mmul(res),
                res.transpose().mmul(convert1dtocol(xdata,xdata.length)));
        if (fin) return para;
        return res.sub(res.mmul(para));
    }

    private int[] arratlistToArray (ArrayList s){
        int ssize = s.size();
        int[] spoints = new int [ssize];
        for (int i = 0; i < ssize; i++){
            spoints[i] = (int) s.get(i);
        }
        return spoints;
    }


    private double stdofcol(FloatMatrix col, int length){
        float total = 0;
        for (int i = 0; i < length; i++){
            total += col.get(i,0);
        }
        double mean = total / length;
        double t = 0;
        for (int i = 0; i < length; i++){
            double err = Math.pow(col.get(i,0) - mean,2.0);
            t += err;
        }
        return Math.sqrt(t / (length - 1));
    }

    private int[] removeoutliers(int[] data, FloatMatrix err,
                                 double std, int size, int k){
       ArrayList res = new ArrayList<>();
       for (int i = 0; i < size; i++){
           if (err.get(i,0) <= k * std){
               res.add(data[i]);
           }
       }
       return arratlistToArray(res);
    }

    private FloatMatrix convert1dtocol(int[] m,int width){
        FloatMatrix temp = new FloatMatrix(width,1);
        for (int i = 0; i < width; i++) {
            temp.put(i,0,(float) m[i]);
        }
        return temp;
    }

    private double calcAngleQuad(FloatMatrix res, int width,int height){
        float c = res.get(0,0);
        float b = res.get(1,0);
        float a = res.get(2,0);
        float yd = height/2;
        float xd = a*yd*yd + b*yd + c;
        float k = (xd - width/2) / yd;
        return Math.tan((double) k);
    }
}
