package hanqis.com.qbot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by LaserSH on 4/5/15.
 */
public class Regression_algorithm {
    private static int dimension;

    private void setDimension(int n){
        dimension = n + 1;
    }


    /* xdata is the dependent variable, ydata is the independent variable. Sounds weird,
       but that what actually happens for our program.
       n is the dimension you want to use, linearRegression just pass 1, quad pass 2, and so on.
       fin determines the result you want. True will just return the coeffcient of the final
       polynomial; false will return the difference between estimated data and real data.
     */
    public Mat Regression(int[] xdata,int[] ydata, int n, boolean fin){
        setDimension(n);
        Mat res = new Mat(ydata.length,dimension, CvType.CV_32FC1);
        for (int i = 0; i < ydata.length; i++){
            for (int j = 0; j < dimension; j++){
                res.put(i,j,new float[]{(float) Math.pow(ydata[i],j)});
            }
        }
        Mat resTres = new Mat(dimension,dimension,CvType.CV_32FC1);
        Mat B = new Mat(dimension,1,CvType.CV_32FC1);
        Mat X = convert1dtocol(xdata,xdata.length);
        Core.gemm(res.t(), X, 1, Mat.zeros(dimension, 1, CvType.CV_32FC1), 0, B, 0);
        Core.gemm(res.t(),res,1,Mat.zeros(dimension,dimension,CvType.CV_32FC1),0,resTres,0);
        Mat para = new Mat(dimension,1,CvType.CV_32FC1);
        Core.solve(resTres,B,para);
        if (fin) return para;
        Mat X1 = new Mat(xdata.length,1,CvType.CV_32FC1);
        Core.gemm(res,para,1,Mat.zeros(ydata.length,1,CvType.CV_32FC1),0,X1,0);
        Mat err = Mat.zeros(xdata.length,1,CvType.CV_32FC1);
        Core.subtract(X,X1,err);
        return err;
    }

    public Mat convert1dtocol(int[] m,int width){
        Mat temp = new Mat(width,1,CvType.CV_32FC1);
        for (int i = 0; i < width; i++) {
            temp.put(i,0,new float[]{m[i]});
        }
        return temp;
    }
}

