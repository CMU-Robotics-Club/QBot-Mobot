package hanqis.com.qbot;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by LaserSH on 4/12/15.
 */
public class Sample_help {
    private double Std;
    private double[] Res;
    private Mat Err2;
    private int[] Npts;
    private List<Point> selectedPts = new ArrayList<Point>();

    public Sample_help(int[] pts, int width, int dimension,int scale) {
        Regression_algorithm Regress = new Regression_algorithm();

        int[] xsub = ind2subx(width,pts.length,pts);
        int[] ysub = ind2suby(width,pts.length,pts);
        Mat err = Regress.Regression(xsub,ysub,dimension,false);

        double mean = meanofcol(err,xsub.length);
        double stdv = stdofcol(err,xsub.length,mean);

        int[] npts = removeoutliers(pts,err,mean,stdv,xsub.length,2);
        setNpts(npts);
        int[] nxsub = ind2subx(width,npts.length,npts);
        int[] nysub = ind2suby(width,npts.length,npts);
        setSelectedPoints(nxsub,nysub,0,scale);

        Mat err2 = Regress.Regression(nxsub,nysub,dimension,false);
        setErr2(err2);
        double mean2 = meanofcol(err2,nxsub.length);
        double stdv2 = stdofcol(err2,nxsub.length,mean2);
        setStandardDeviation(stdv2);

        Mat res = Regress.Regression(nxsub,nysub,dimension,true);
        ArrayList<Double> resList = new ArrayList<>();
        for(int i = 0; i <= dimension; i++){
            resList.add(res.get(i,0)[0]);
        }
        setRes(resList);
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

    public static double[] convertDouble(List<Double> input)
    {
        double[] res = new double[input.size()];
        Iterator<Double> iterator = input.iterator();
        for (int i = 0; i < res.length; i++)
        {
            res[i] = iterator.next().doubleValue();
        }
        return res;
    }
    private double meanofcol(Mat col, int length){
        float total = 0;
        for (int i = 0; i < length; i++){
            total += col.get(i,0)[0];
        }
        double mean = total / length;
        return mean;
    }

    private double stdofcol(Mat col, int length, double mean){
        double t = 0;
        for (int i = 0; i < length; i++){
            double err = Math.pow(col.get(i,0)[0] - mean,2.0);
            t += err;
        }
        return Math.sqrt(t / (length - 1));
    }

    private int[] removeoutliers(int[] data, Mat err, double mean,
                                 double std, int size, int k){
       ArrayList res = new ArrayList<Integer>();
       for (int i = 0; i < size; i++){
           if (err.get(i,0)[0] - mean <= k * std){
               res.add(data[i]);
           }
       }
       return convertIntegers(res);
    }

    public int[][] splitIntoTwo(int[] data,Mat err,int size,int width) {
        int[][] res = new int[4][];
        ArrayList newPtsU = new ArrayList<Integer>();
        ArrayList newPtsD = new ArrayList<Integer>();
        for (int i = 0; i < size; i++){
            if (err.get(i, 0)[0] > 0) {
                newPtsU.add(data[i]);
            } else {
                newPtsD.add(data[i]);
            }
        }

        res[0] = convertIntegers(newPtsU);
        res[1] = convertIntegers(newPtsD);

        return res;
    }

    public List<Point> getSelectedPoints() {
        return selectedPts;
    }

    private void setSelectedPoints(int[] xsub,int[] ysub,int yoff,int scale){
        selectedPts.clear();
        for (int i = 0; i < xsub.length; i++){
            Point p = new Point(xsub[i] * scale,(ysub[i]) * scale);
            selectedPts.add(p);
        }
    }

    private void setStandardDeviation(double std){ Std = std; }

    public double getStandardDeviation(){ return Std; }

    private void setRes(ArrayList<Double> res){ Res = convertDouble(res); }

    public double[] getRes(){ return Res; }

    public Mat getErr2() { return Err2; }

    public void setErr2(Mat err2) { Err2 = err2; }

    public int[] getNpts() { return Npts; }

    public void setNpts(int[] npts) { Npts = npts; }
}
