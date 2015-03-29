package hanqis.com.qbot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
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

    public double Sampling(Mat vidRgb){
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
        Size sz = new Size(adjWidth,adjHeight);
        Imgproc.resize(vidRgb,vidRgbAdj,sz);
        Imgproc.cvtColor(vidHsv, vidRgbAdj, Imgproc.COLOR_BGR2HSV);

        List<Mat> hsv_planes = new ArrayList<Mat>();
        Core.split(vidHsv, hsv_planes);

        Mat vidHue = hsv_planes.get(0);
        int[] randompoints = new int[amount];
        for (int i = 0; i < amount; i++){
            Random rn = new Random();
            randompoints[i] = rn.nextInt(adjHeight * adjWidth);
        }

        int[] xsub = ind2subx(adjWidth,amount,randompoints);
        int[] ysub = ind2suby(adjWidth,amount,randompoints);

        ArrayList selectedpoints = new ArrayList<>();
        for (int i = 0; i < amount; i++){
            if (vidHue.get(xsub[i],ysub[i])[0] > threshold){
                selectedpoints.add(randompoints[i]);
            }
        }


        int ssize = selectedpoints.size();
        int[] spoints = new int [ssize];
        for (int i = 0; i < ssize; i++){
            spoints[i] = (int) selectedpoints.get(i);
        }
        int[][] linear2d = new int [ssize][2];
        int[][] quad2d = new int [ssize][3];
        int[] sxsub = ind2subx(adjWidth, ssize, spoints);
        Mat matxsub = convert1dtomat(sxsub, ssize);
        Mat tmatxsub = new Mat (ssize,1,CvType.CV_16U);
        int[] sysub = ind2suby(adjWidth, ssize, spoints);
        for (int i = 0; i < ssize; i++) {
            linear2d[i][0] = 1;
            quad2d[i][0] = 1;
            linear2d[i][1] = sysub[i];
            quad2d[i][1] = sysub[i];
            quad2d[i][2] = sysub[i] * sysub[i];
        }

        Mat linearmat = convert2dtomat(linear2d,ssize,2);
        Mat quadmat = convert2dtomat(quad2d,ssize,3);
        Mat tlinearmat = new Mat(2,ssize,CvType.CV_16U);
        Mat tquadmat = new Mat(3,ssize,CvType.CV_16U);
        Core.transpose(tlinearmat,linearmat);
        Core.transpose(tquadmat,quadmat);
        Core.transpose(tmatxsub,matxsub);



        return angle;
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

    private Mat convert2dtomat(int[][] m, int height, int width) {
        Mat temp = new Mat(height, width, CvType.CV_16U);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int[] tmp = new int[]{m[i][j]};
                temp.put(i, j, tmp);
            }

        }
        return temp;
    }

    private Mat convert1dtomat(int[] m,int width){
        Mat temp = new Mat(1,width,CvType.CV_16U);
        for (int i = 0; i < width; i++) {
            int[] tmp = new int[]{m[i]};
            temp.put(0,i,tmp);
        }
        return temp;
    }

}
