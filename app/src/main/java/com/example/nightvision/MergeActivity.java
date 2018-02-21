package com.example.nightvision;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MergeActivity extends AppCompatActivity implements MergeFragment.MergeCallbacks{

    //Goal: Load the image just saved by the camera.

    public static final String MERGE_TAG = "MERGE";

    ArrayList<String> urlList;
    ImageView mImageView;


    //Old hotness for background stuff
    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;


    //New hotness for background stuff
    private static final String TAG_TASK_FRAGMENT = "merge_fragment";
    private MergeFragment mMergeFragment;

    public static BitmapFactory.Options options = new BitmapFactory.Options();

    public static float imageBrightness = 1;

    File mFile;
    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (options.inSampleSize != 4) {
            options.inSampleSize = 4;
            //options.inPreferredConfig = Bitmap.Config.ALPHA_8; Doesn't make it faster
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_shower);
        mImageView = (ImageView) findViewById(R.id.imageView);

        Intent intent = getIntent();
        urlList = intent.getStringArrayListExtra("URLs");
        imageBrightness = (float) intent.getIntExtra("Brightness", 1);

        FragmentManager fm = getFragmentManager();
        mMergeFragment = (MergeFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        if (mMergeFragment == null) {
            mMergeFragment = MergeFragment.newFragment(urlList);
            fm.beginTransaction().add(mMergeFragment, TAG_TASK_FRAGMENT).commit();
        } else if (mMergeFragment.finishedImage !=null){
            mImageView.setImageBitmap(mMergeFragment.finishedImage);
        }
    }

    // The four methods below are called by the TaskFragment when new
    // progress updates or results are available. The MainActivity
    // should respond by updating its UI to indicate the change.

    @Override
    public void onPreExecute() {


    }

    @Override
    public void onProgressUpdate(int percent) {}

    @Override
    public void onCancelled() {}

    @Override
    public void onPostExecute(Bitmap bmp) {
        mMergeFragment.finishedImage = bmp;
        mImageView.setImageBitmap(bmp);
        for(String url: urlList){
            File file = new File(url);

            boolean deleted = false;
            try {
                deleted = file.getCanonicalFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(deleted){
                Log.d(MERGE_TAG, "Deleted file");
            } else {
                Log.d(MERGE_TAG, "Failed to delete file");
            }
        }

        BuildNewFile();
        ImageSaver saver = new ImageSaver(bmp, mFile);
        mBackgroundHandler.post(saver);
    }

    private void BuildNewFile(){
        Calendar calendar = new GregorianCalendar();
        String targetURL = "Merge";
        targetURL = targetURL + calendar.get(Calendar.DATE);
        targetURL = targetURL +"_" +  calendar.get(Calendar.HOUR_OF_DAY);
        targetURL = targetURL + "_" + calendar.get(Calendar.MINUTE);
        targetURL = targetURL + "_" + calendar.get(Calendar.SECOND);
        targetURL = targetURL + "_" + calendar.get(Calendar.MILLISECOND);


        targetURL = targetURL + ".jpg";
        filePath = targetURL;

        mFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), targetURL);
        System.out.println("New file: " + mFile.getName());
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Bitmap mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Bitmap image, File file) {
            mImage = image;
            mFile = file;

        }


        @Override
        public void run() {
            boolean done = false;
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);

                done = mImage.compress(Bitmap.CompressFormat.JPEG, 30, output);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(done){
                    Log.d(MERGE_TAG, "Saving Done");
                } else {
                    Log.d(MERGE_TAG, "Saving not done");
                }
            }
        }

    }

    //Old hotness, down below.


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        super.onPause();
    }





    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Image Merging");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    private static class ImageHandler implements Runnable {
//        BitmapFactory.Options mOptions;
//        ImageView mImageView;
//
//        ImageHandler(BitmapFactory.Options options, ArrayList<String> urlList, ImageView source){
//            mOptions = options;
//            mUrlList = urlList;
//            mImageView = source;
//        }
//
//        ArrayList<String> mUrlList;
//
//        Bitmap[] listOfImages = null;
//        Bitmap mergedImage = null;
//
//        private void loadImages(){
//            System.out.println("Loading images");
//            listOfImages = new Bitmap[mUrlList.size()];
//            for(int i = 0; i< mUrlList.size(); i++){
//                System.out.println("Decoding: " + mUrlList.get(i));
//                Bitmap bmp = BitmapFactory.decodeFile(mUrlList.get(i), mOptions);
//                listOfImages[i] = bmp;
//            }
//            System.out.println("Finished loading images");
//        }
//
//
//        private void mergeImages(){
//            ImageManipulatorPausable manipulator = new ImageManipulatorPausable(listOfImages);
//            manipulator.averageColorBrighten(4f);
//            mergedImage = manipulator.constructBitmap();
//        }
//
//
//        @Override
//        public void run() {
//            loadImages();
//            mergeImages();
//            mImageView.post(new Runnable() {
//                public void run() {
//                    mImageView.setImageBitmap(mergedImage);
//                }
//            });
//        }
//    }



}