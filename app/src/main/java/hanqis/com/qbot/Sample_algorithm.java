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
        double angle = 0.0;

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
            if (vidHue.get(xsub[i], ysub[i])[0] > threshold) {
                selectedpoints.add(randompoints[i]);
            }
        }
        int[] spoints = arratlistToArray(selectedpoints);
        int[] sxsub = ind2subx(adjWidth, spoints.length, spoints);
        int[] sysub = ind2suby(adjWidth, spoints.length, spoints);

        FloatMatrix approx1 = linearRegression(sxsub, sysub, false);
        FloatMatrix approx2 = quadRegression(sxsub, sysub, false);
        double std1 = stdofcol(res1);
        double std2 = stdofcol(res2);

        int[] newpoints1 = removeoutliers(spoints, res1.sub(approx1), std1, 2);
        int[] newpoints2 = removeoutliers(spoints, res2.sub(approx2), std2, 2);

        int[] nxsub = ind2subx(adjWidth, newpoints1.length, newpoints1);
        int[] nysub = ind2suby(adjWidth, newpoints2.length, newpoints2);

        FloatMatrix res1 = linearRegression(nxsub, nysub, true);
        FloatMatrix res2 = quadRegression(nxsub, nysub, true);

        double angle1 = Math.tan((double) res1.get(1, 0));
        double angle2 = 0.0 //Remain to be done.
        return (angle1 + angle2) / 2;

        /*
        Mat matxsub = convert1dtomat(sxsub, ssize);
        Mat tmatxsub = new Mat (ssize,1,CvType.CV_16U);Mat linearmat = convert2dtomat(linear2d,ssize,2);
        Mat quadmat = convert2dtomat(quad2d,ssize,3);
        Mat tlinearmat = new Mat(2,ssize,CvType.CV_16U);
        Mat tquadmat = new Mat(3,ssize,CvType.CV_16U);
        Core.transpose(tlinearmat,linearmat);
        Core.transpose(tquadmat,quadmat);
        //Core.transpose(tmatxsub,matxsub);

        Mat lineartl = new Mat(2,2,CvType.CV_16U);
        Mat quadtl = new Mat(3,3,CvType.CV_16U);
        Mat lineartb = new Mat(2,1,CvType.CV_16U);
        Mat quadtb = new Mat(3,1,CvType.CV_16U);

        Core.gemm(linearmat,tlinearmat,1,new Mat(),0,lineartl,0);
        Core.gemm(quadmat,tquadmat,1,new Mat(),0,quadtl,0);
        Core.gemm(matxsub,linearmat,1,new Mat(),0,lineartb,0);
        Core.gemm(matxsub,quadmat,1,new Mat(),0,quadtb,0);

        Mat invlineartl = new Mat(2,2,CvType.CV_16U);
        Mat invquadtl = new Mat(3,3,CvType.CV_16U);
        */
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
        return res.mmul(para);
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
        return res.mmul(para);
    }

    private int[] arratlistToArray (ArrayList s){
        int ssize = s.size();
        int[] spoints = new int [ssize];
        for (int i = 0; i < ssize; i++){
            spoints[i] = (int) s.get(i);
        }
        return spoints;
    }
    /*private Mat convert2dtomat(int[][] m, int height, int width) {
        Mat temp = new Mat(height, width, CvType.CV_16U);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int[] tmp = new int[]{m[i][j]};
                temp.put(i, j, tmp);
            }

        }
        return temp;
    } */

    private FloatMatrix convert1dtocol(int[] m,int width){
        FloatMatrix temp = new FloatMatrix(width,1);
        for (int i = 0; i < width; i++) {
            temp.put(i,0,(float) m[i]);
        }
        return temp;
    }

}
