package com.example.nightvision;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Solomon on 7/18/2017.
 */

public class MergeFragment extends Fragment {
    private static final String URL_LIST_ID = "url_id";

    private static final String OPTIONS_ID = "options_id";
    private static BitmapFactory.Options options;

    private ArrayList<String> urlList;


    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface MergeCallbacks {
        void onPreExecute();
        void onProgressUpdate(int percent);
        void onCancelled();
        void onPostExecute(Bitmap bmp);
    }

    /**
     *
     * @param urlList- the urls of the images that will be dealt with
     * @return- a set up mergeFragment.
     */
    public static MergeFragment newFragment(ArrayList<String> urlList){
        MergeFragment fragment = new MergeFragment();

        Bundle arguments = new Bundle();
        arguments.putStringArrayList(URL_LIST_ID, urlList);
        fragment.setArguments(arguments);

        return fragment;
    }

    private MergeCallbacks mCallbacks;
    private MergeTask mTask;
    public Bitmap finishedImage = null;


    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mCallbacks = (MergeCallbacks) activity;
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        Bundle arguments = getArguments();
        if(arguments == null){
            Log.d(MergeActivity.MERGE_TAG, "Arguments null");
        } else {
            urlList = arguments.getStringArrayList(URL_LIST_ID);
            if(urlList == null){
                Log.d(MergeActivity.MERGE_TAG, "URL list empty");
            }
        }

        // Create and execute the background task.
        mTask = new MergeTask();
        mTask.execute(urlList);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }



    private class MergeTask extends AsyncTask<ArrayList<String>, Integer, Bitmap> {

        ImageManipulatorPausable mergeSystem;



        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }

        /**
         * Note that we do NOT call the callback object's methods
         * directly from the background thread, as this could result
         * in a race condition.
         */
        @Override
        protected Bitmap doInBackground(ArrayList<String>... URLs) {
            mergeSystem = new ImageManipulatorPausable(loadImages(URLs[0]));
            Log.d(MergeActivity.MERGE_TAG, MergeActivity.imageBrightness+"");
            mergeSystem.averageColorBrighten(MergeActivity.imageBrightness);
            System.out.println("Done!");
            return mergeSystem.constructBitmap();

        }

        @Override
        protected void onProgressUpdate(Integer... percent) {
            if (mCallbacks != null) {
                mCallbacks.onProgressUpdate(percent[0]);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute(result);
            }
            System.out.println("Post-execute done");
        }

        private Bitmap[] loadImages(ArrayList<String> urlList){

            Bitmap[] listOfImages = null;

            System.out.println("Loading images");
            listOfImages = new Bitmap[urlList.size()];
            for(int i = 0; i< urlList.size(); i++){
                System.out.println("Decoding: " + urlList.get(i));
                Bitmap bmp = BitmapFactory.decodeFile(urlList.get(i), MergeActivity.options);
                listOfImages[i] = bmp;
            }
            System.out.println("Finished loading images");
            return listOfImages;
        }



    }
}
