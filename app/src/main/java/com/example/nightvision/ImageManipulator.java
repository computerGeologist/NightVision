package com.example.nightvision;

/**
 * Created by Solomon on 7/7/2017.
 */

import android.graphics.Bitmap;



public class ImageManipulator {

    Bitmap[] inputImages;
    int xOutput;
    int yOutput;

    ImageManipulator(Bitmap[] inputImagesList){
        this.inputImages = inputImagesList;
        //chosen arbitrarily
        this.xOutput = inputImages[0].getWidth();
        this.yOutput = inputImages[0].getHeight();
        System.out.println(xOutput);
        System.out.println(yOutput);
    }

    public Bitmap averagedColor(){
        System.out.println("Merging " + inputImages.length + " images...");
        int[] data = new int[xOutput * yOutput];
        int index = 0;
        //There's probably a better way to do this than to set every pixel individually.
        for(int i=0; i<xOutput; i++){
            for(int j=0; j<yOutput; j++){
                int[] targetArray = averagedColorAtPoint(j, i, yOutput, xOutput);
                data[index] =  (targetArray[3] << 24) + (targetArray[0] << 16)
                                + (targetArray[1] << 8) + targetArray[2];
                index++;
            }
        }
        System.out.println("Finished merging");

        Bitmap output = Bitmap.createBitmap(data, yOutput, xOutput, inputImages[0].getConfig());
        return output;
    }

    public Bitmap averagedColorBrightened(float increase){
        System.out.println("Merging " + inputImages.length + " images...");
        int[] data = new int[xOutput * yOutput];
        int index = 0;


        for(int i=0; i<xOutput; i++){
            for(int j=0; j<yOutput; j++){
                int[] targetArray = averagedColorAtPointWithBrighten(j, i, yOutput, xOutput, increase);
                data[index] =  (targetArray[3] << 24) + (targetArray[0] << 16)
                        + (targetArray[1] << 8) + targetArray[2];
                index++;
            }
        }
        System.out.println("Finished merging");

        Bitmap output = Bitmap.createBitmap(data, yOutput, xOutput, inputImages[0].getConfig());
        return output;
    }




    public static int closestPixel(int targetPoint, int oldResolution, int newResolution){
        int ans = Math.min(Math.round((float) targetPoint * ((float) newResolution)/((float) oldResolution)), newResolution-1);
        return ans;
    }

    /***
     * Calculates the average color of a particular point over all the images..
     * @param x- x coordinate in the target image
     * @param y - y coordinate in the target image
     * @param xRes - x/xRes is the % across the image.
     * @param yRes - y/yRes is the % down the image.
     * @param brightenUpFactor - Scale the brightness of the image by the given factor.
     * @return- an array in RGBA order containing the average color at the corresponding point.
     */
    public int[] averagedColorAtPointWithBrighten(int x, int y, int xRes, int yRes, float brightenUpFactor){
        int numImages = inputImages.length;
        int[] targetArray = new int[]{0, 0, 0, 0};
        for(int k=0; k<numImages; k++){
            Bitmap target = inputImages[k];

            int rgb;
            try {
                rgb = target.getPixel(closestPixel(x, xRes, target.getWidth()),
                        closestPixel(y, yRes, target.getHeight()));
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

    public int[] averagedColorAtPoint(int x, int y, int xRes, int yRes){
        return averagedColorAtPointWithBrighten(x, y, xRes, yRes, 1f);
    }


}

