package com.example.fran.imachineappv2.CIEngine.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtils {

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth){
            //Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            //Choose the smallest ratio as inSampleSize value, this will guarantee
            //a final image with both dimensions larger than or equal to the requested
            //height and width
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap lessResolution (String filePath, int width, int height){
        // TODO: handle exceptions
        BitmapFactory.Options options = new BitmapFactory.Options();

        //First decode with inJustDecodeBounds = true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath,options);

        //Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        //Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath,options);

    }

    public static ByteBuffer convertBufferedImageToByteBuffer(
            Bitmap image,int batchSize,int w,int h,int pixelSize,float imageMean,float imageStd) {
        // 4 bytes -> 1 float (32 bits)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * batchSize * h * w * pixelSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        //byteBuffer.rewind();

        int[] intValues = new int[w * h];

        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        int pixel = 0;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)-imageMean)/imageStd);  // red
                byteBuffer.putFloat((((val >> 8) & 0xFF)-imageMean)/imageStd);  // green
                byteBuffer.putFloat((((val) & 0xFF)-imageMean)/imageStd);  // blue
            }
        }
        return byteBuffer;
    }

}
