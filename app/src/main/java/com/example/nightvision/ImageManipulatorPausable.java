package com.example.nightvision;

/**
 * Created by Solomon on 7/7/2017.
 */

import android.graphics.Bitmap;
import android.icu.util.GregorianCalendar;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Calendar;


public class ImageManipulatorPausable{

    Bitmap[] inputImages;
    int[] inputX;
    int[] inputY;
    private int xOutput;
    private int yOutput;

    int[] data;
    int index;
    private boolean doneWithData;
    boolean paused;

    int prevSecond;
    int lastIndex;

    Calendar calendar;

    int time;

    ImageManipulatorPausable(Bitmap[] inputImagesList){

        this.inputImages = inputImagesList;
        inputY = new int[inputImagesList.length];
        inputX = new int[inputImagesList.length];
        for(int i=0; i<inputImagesList.length; i++){
            this.inputY[i] = inputImagesList[i].getHeight();
            this.inputX[i] = inputImagesList[i].getWidth();
        }
        //chosen arbitrarily
        this.yOutput = inputImages[0].getWidth();
        this.xOutput = inputImages[0].getHeight();
        System.out.println(xOutput);
        System.out.println(yOutput);

        data = new int[xOutput * yOutput];
        index = 0;
        paused = false;
    }

    public void pause(){
        paused = true;
    }


    public void averageColor(){
        doneWithData = false;
        averageColorBrighten(1);
    }

//    Old way, using for loops.
//    public void averageColorBrighten(float increase){
//        doneWithData = false;
//        System.out.println("Merging " + inputImages.length + " images...");
//        index = 0;
//        for(int i=0; i<xOutput; i++){
//            for(int j=0; j<yOutput; j++){
//                int[] targetArray = averagedColorAtPointWithBrighten(j, i, increase);
//                data[index] =  arrayToColorInt(targetArray);
//                index++;
//            }
//        }
//
//    }

    public int percentDone() {
        return index/data.length;
    }
    public int lastPercentDone() {
        return lastIndex/data.length;
    }

    public void resetTime() {
        this.lastIndex = index;
        this.prevSecond = Calendar.getInstance().get(Calendar.SECOND);
    }

    public void endTime() {
        time = ((Calendar.getInstance().get(Calendar.SECOND)) - prevSecond) & 60;
        Log.d(MergeActivity.MERGE_TAG, "Finished averaged color operation in " + time + " seconds, over " + (index - lastIndex) + " indices, and " + inputImages.length + " images");
        //Log.d(MergeActivity.MERGE_TAG, "This is an average rate of " + (index-lastIndex)/(inputImages.length* time) + " indices per picture per second");
    }


    public void averageColorBrighten(float increase){
        index = 0;
        if(false) {
            averageColorBrightenResume(increase);
        } else if(false){ //no speed improvements noted
            averageColorBrightenResumeOneColor(increase);
        } else {
            averageColorBrightenResumeWithBuffers(increase);
        }
    }

    public void averageColorBrightenResume(float increase){
        resetTime();
        doneWithData = false;
        while (!doneWithData && !paused){
            data[index] = arrayToColorInt(averagedColorAtIndexWithBrighten(index, increase));
            //data[index] = averagedAlphaAtIndexWithBrighten(index, increase); No speed improvements.
            index++;
            if(index >= data.length){
                doneWithData = true;
            }
        }
        endTime();
    }

    public void averageColorBrightenResumeOneColor(float increase){
        resetTime();
        doneWithData = false;
        while (!doneWithData && !paused){
            data[index] = averagedAlphaAtIndexWithBrighten(index, increase);
            index++;
            if(index >= data.length){
                doneWithData = true;
            }
        }
        endTime();
    }


    IntBuffer[] mBuffers;
    public void averageColorBrightenResumeWithBuffers(float increase) {

        mBuffers = new IntBuffer[inputImages.length];
        for (int i=0; i<mBuffers.length; i++) {
            mBuffers[i]= IntBuffer.allocate(inputImages[i].getAllocationByteCount());
            inputImages[i].copyPixelsToBuffer(mBuffers[i]);
            mBuffers[i].position(0);
            inputImages[i]=null;
        }
        resetTime();
        Log.d(MergeActivity.MERGE_TAG, "Starting to read buffers");
        Log.d(MergeActivity.MERGE_TAG, "Buffer length: "+mBuffers[0].limit());
        Log.d(MergeActivity.MERGE_TAG, "Data length: "+ data.length);

        while(mBuffers[0].remaining()>0 && index < data.length){
            data[index] = arrayToColorInt(averagedColorFromBuffer(mBuffers, increase));
            index++;
        }
        Log.d(MergeActivity.MERGE_TAG, "Done: " + index);
        endTime();
    }

    public Bitmap constructBitmap(){
        Bitmap output = Bitmap.createBitmap(data, yOutput, xOutput, MergeActivity.options.inPreferredConfig);
        return output;
    }

    public int xFromIndex(int index){
        return index / yOutput;
    }
    public int yFromIndex(int index){
        return (index - xFromIndex(index) * yOutput);
    }
    public int coordToIndex(int x, int y) {return x*xOutput + y;}

    public static int arrayToColorInt(int[] input){
        int ans = (input[3] << 24) + (input[0] << 16)
                + (input[1] << 8) + input[2];
        return ans;
    }



    public static int closestPixel(int targetPoint, int oldResolution, int newResolution){
        int ans = Math.min(Math.round((float) targetPoint * ((float) newResolution)/((float) oldResolution)), newResolution-1);
        return ans;
    }

    /***
     * Calculates the average color of a particular point over all the images..
     * @param x- x coordinate in the target image
     * @param y - y coordinate in the target image
     * @param brightenUpFactor - Scale the brightness of the image by the given factor.
     * @return- an array in RGBA order containing the average color at the corresponding point.
     */
    public int[] averagedColorAtPointWithBrighten(int x, int y, float brightenUpFactor){
        int numImages = inputImages.length;
        int[] targetArray = new int[]{0, 0, 0, 0};
        for(int k=0; k<numImages; k++){
            Bitmap target = inputImages[k];
            int rgb;
            try {
                rgb = target.getPixel(
                        closestPixel(x, xOutput, target.getWidth()),
                        closestPixel(y, yOutput, target.getHeight()));
            } catch (Exception e) {
                e.printStackTrace();
                rgb=0;
            }

            targetArray[0]+=(rgb >> 16) & 0xFF;
            targetArray[1]+=(rgb >>  8) & 0xFF;
            targetArray[2]+=(rgb      ) & 0xFF;
            targetArray[3]+=(rgb >> 24) & 0xFF;
        }
        //Renormalize.
        for(int i=0; i<targetArray.length;/*4- unless I change to RGB*/ i++){
            targetArray[i] = Math.round(targetArray[i] * brightenUpFactor);
        }
        for(int i=0; i<targetArray.length;/*4- unless I change to RGB*/ i++){
            targetArray[i] = Math.min(targetArray[i]/numImages, 255);
        }

        return targetArray;
    }

    /**
     * A version for ALPHA_8 format storage. Sadly, no speed improvements noted, and it would take time to work out the bugs.
     * @param x
     * @param y
     * @param brightenUpFactor
     * @return
     */
    public int averagedAlphaAtPointWithBrighten(int x, int y, float brightenUpFactor){
        int numImages = inputImages.length;
        int ans = 0;
        for(int k=0; k<numImages; k++){
            Bitmap target = inputImages[k];

            int rgb;
            try {
                rgb = target.getPixel(
                        closestPixel(x, xOutput, target.getWidth()),
                        closestPixel(y, yOutput, target.getHeight()));
            } catch (Exception e) {
                e.printStackTrace();
                rgb=0;
            }

            ans += (rgb >> 24) & 0xFF;
        }
        //Renormalize.

        ans = Math.round(ans * brightenUpFactor);

        ans = Math.min(ans/numImages, 255);
        ans = (255 << 24) + (ans << 16) + (ans << 8) + ans;

        return ans;
    }


    public int[] averagedColorFromBuffer(IntBuffer[] buffers, float brightenUpFactor){
        int[] targetArray = {0, 0, 0, 0};

        for(int i = 0; i<buffers.length; i++){
            IntBuffer buffer = buffers[i];
            int rgb = buffer.get();
            targetArray[2]+=(rgb >> 16) & 0xFF;
            targetArray[1]+=(rgb >>  8) & 0xFF;
            targetArray[0]+=(rgb      ) & 0xFF;
            //targetArray[3]+=(rgb >> 24) & 0xFF;
        }

        for(int i=0; i<targetArray.length;/*4- unless I change to RGB*/ i++){
            targetArray[i] = Math.round(targetArray[i] * brightenUpFactor);
        }
        for(int i=0; i<targetArray.length;/*4- unless I change to RGB*/ i++){
            targetArray[i] = Math.min(targetArray[i]/inputImages.length, 255);
        }
        targetArray[3] = 255;

        return targetArray;
    }

    public int[] averagedColorAtIndexWithBrighten(int index, float brightenUpFactor){
        int x = xFromIndex(index);
        int y = yFromIndex(index);
        return averagedColorAtPointWithBrighten(x, y, brightenUpFactor);
    }

    public int averagedAlphaAtIndexWithBrighten(int index, float brightenUpFactor){
        int x = xFromIndex(index);
        int y = yFromIndex(index);
        return averagedAlphaAtPointWithBrighten(x, y, brightenUpFactor);
    }

    public int[] averagedColorAtPoint(int x, int y){
        return averagedColorAtPointWithBrighten(x, y, 1f);
    }


}

