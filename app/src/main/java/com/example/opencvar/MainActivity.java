package com.example.opencvar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.open_cv);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat mt = inputFrame.rgba();
        Mat lrg;
        try {
            lrg = findLargestRectangle(mt);
        }catch (Exception e){
            return mt;
        }
        return lrg;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
                mLoaderCallback);
    }

    private Mat findLargestRectangle(Mat original_image) {
        Mat imgSource = original_image.clone();
        //Mat untouched = original_image.clone();

        //convert the image to black and white
        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);

        //convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource, imgSource, 50, 50);

        //apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource,
                imgSource,
                new Size(5, 5),
                5);

        //find the contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        int maxAreaIdx = -1;
        MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f maxCurve = new MatOfPoint2f();
        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
        for (int idx = 0; idx < contours.size(); idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            //compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
                int contourSize = (int)temp_contour.total();
                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize*0.05, true);
                if (approxCurve.total() == 4) {
                    maxCurve = approxCurve;
                    maxArea = contourarea;
                    maxAreaIdx = idx;
                    largest_contours.add(temp_contour);
                }
            }
        }

        //create the new image here using the largest detected square
        Mat new_image = new Mat(imgSource.size(), CvType.CV_8U); //we will create a new black blank image with the largest contour
        Imgproc.cvtColor(new_image, new_image, Imgproc.COLOR_BayerBG2RGB);
        Imgproc.drawContours(new_image, contours, maxAreaIdx, new Scalar(255, 255, 255), 1); //will draw the largest square/rectangle

        double temp_double[] = maxCurve.get(0, 0);
        Point p1 = new Point(temp_double[0], temp_double[1]);

        temp_double = maxCurve.get(1, 0);
        Point p2 = new Point(temp_double[0], temp_double[1]);

        temp_double = maxCurve.get(2, 0);
        Point p3 = new Point(temp_double[0], temp_double[1]);

        temp_double = maxCurve.get(3, 0);
        Point p4 = new Point(temp_double[0], temp_double[1]);

        Log.i("P1: x", String.valueOf(p1.x));
        Log.i("P1: y", String.valueOf(p1.y));
        Log.i("P2: x", String.valueOf(p2.x));
        Log.i("P2: y", String.valueOf(p2.y));
        Log.i("P3: x", String.valueOf(p3.x));
        Log.i("P3: y", String.valueOf(p3.y));
        Log.i("P4: x", String.valueOf(p4.x));
        Log.i("P4: y", String.valueOf(p4.y));

        Mat img = null;
        try {
            img = Utils.loadResource(this, R.drawable.frame, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LinkedList<Point> cornerList = new LinkedList<Point>();
        cornerList.add(new Point(0,0));
        cornerList.add(new Point(img.cols(),0));
        cornerList.add(new Point(img.cols(),img.rows()));
        cornerList.add(new Point(0,img.rows()));

        LinkedList<Point> cornerListmx = new LinkedList<Point>();
        cornerListmx.add(new Point(p1.x,p1.y));
        cornerListmx.add(new Point(p2.x,p2.y));
        cornerListmx.add(new Point(p3.x,p3.y));
        cornerListmx.add(new Point(p4.x,p4.y));

        MatOfPoint mpoints = new MatOfPoint();
        mpoints.fromList(cornerList);
        MatOfPoint mpointsmx = new MatOfPoint();
        mpointsmx.fromList(cornerListmx);

        MatOfPoint2f imgpoints2f = new MatOfPoint2f(mpoints.toArray());
        MatOfPoint2f imgpoints2fmx = new MatOfPoint2f(mpointsmx.toArray());

        Mat H = Calib3d.findHomography(imgpoints2f,imgpoints2fmx);

        Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
        Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);

        Mat warp = Imgproc.getPerspectiveTransform(obj_corners,scene_corners);
        Mat result = new Mat();
        Imgproc.warpPerspective(img, result, warp, original_image.size());
        Mat finalresult = new Mat();
        Core.addWeighted(result,0.5,original_image,0.5,0,finalresult);

//        Size s = new Size(original_image.cols(),original_image.rows());
//        Mat result = new Mat();
//        Imgproc.warpPerspective(img,result,H,s);
//        result.copyTo(original_image);
//        Imgproc.getPerspectiveTransform()
//        Core.perspectiveTransform(obj_corners, scene_corners, H);
//        Matrix transform = new Matrix();
//
//        float[] src = new float[]{(float) p1.x, (float) p1.y, (float) p2.x, (float) p2.y, (float) p3.x, (float) p3.y, (float) p4.x, (float) p4.y};
//        float[] dst = new float[]{0f,0f,bitmap.getWidth(),0f,bitmap.getWidth(),bitmap.getHeight(),0,bitmap.getHeight()};
//        transform.setPolyToPoly(src,0,dst,0,8);

        return finalresult;
    }

}