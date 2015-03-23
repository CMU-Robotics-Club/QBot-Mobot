package hanqis.com.qbot;

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

    private static int amount;

    public double Sampling(Mat vidRgb){
        int vidHeight = vidRgb.height();
        int vidWidth = vidRgb.width();

        int scale = 2;
        double threshold = 0.5;
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

        //Not finished.



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
}
