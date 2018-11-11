package com.example.fran.imachineappv2.CIEngine.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtils {

    /*
    static BufferedImage resize(BufferedImage img, int width, int height) {
        // Extracted from https://stackoverflow.com/questions/9417356/bufferedimage-resize
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
    */

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth){
            //Calculate ratios of height and width to requested heigth and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            //Choose the smallest ratio as inSampleSie value, this will guarantee
            //a final image with both dimensions larger than or equal to the requested
            //height and width
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap lessResolution (String filePath, int width, int height){
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
        //4 por el uso de flotantes, 4 bytes -> 1 float de 32 bits
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * batchSize * h * w * pixelSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        //byteBuffer.rewind();

        int[] intValues = new int[w * h];

        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        // TODO: this in a separate function?
        int pixel = 0;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                final int val = intValues[pixel++];
                //byteBuffer.put((byte) ((val >> 16) & 0xFF));
                //byteBuffer.put((byte) ((val >> 8) & 0xFF));
                //byteBuffer.put((byte) (val & 0xFF));
                byteBuffer.putFloat((((val >> 16) & 0xFF)-imageMean)/imageStd);  // red
                byteBuffer.putFloat((((val >> 8) & 0xFF)-imageMean)/imageStd);  // green
                byteBuffer.putFloat((((val) & 0xFF)-imageMean)/imageStd);  // blue
            }
        }
        return byteBuffer;
    }

}
