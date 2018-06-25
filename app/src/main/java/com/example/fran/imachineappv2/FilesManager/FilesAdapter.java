package com.example.fran.imachineappv2.FilesManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.snatik.storage.Storage;

import java.io.File;
import java.util.List;

import com.example.fran.imachineappv2.R;

/**
 * Based on: https://github.com/sromku/android-storage
 */

public class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<File> mFiles;
    private OnFileItemListener mListener;
    private Storage mStorage;

    public FilesAdapter(Context context) {
        mStorage = new Storage(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_line_view, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final File file = mFiles.get(position);
        FileViewHolder fileViewHolder = (FileViewHolder) holder;
        fileViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onClick(file);
            }
        });
        fileViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.onLongClick(file);
                return true;
            }
        });
        fileViewHolder.mName.setText(file.getName());
        if(file.isDirectory()){
            fileViewHolder.mIcon.setImageResource(R.drawable.ic_folder_primary_24dp);
        }else{
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(),bmOptions);
//            Bitmap image = lessResolution(file.getAbsolutePath(),60,60);
            image = Bitmap.createScaledBitmap(image, 60,60,false);
            fileViewHolder.mIcon.setImageBitmap(image);
        }
//        fileViewHolder.mIcon.setImageResource(file.isDirectory() ? R.drawable.ic_folder_primary_24dp : R.drawable
//                .ic_file_primary_24dp);
        if (file.isDirectory()) {
            fileViewHolder.mSize.setVisibility(View.GONE);
        } else {
            fileViewHolder.mSize.setVisibility(View.VISIBLE);
            fileViewHolder.mSize.setText(mStorage.getReadableSize(file));
        }

    }

    @Override
    public int getItemCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    public void setFiles(List<File> files) {
        mFiles = files;
    }

    public void setListener(OnFileItemListener listener) {
        mListener = listener;
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mSize;
        ImageView mIcon;

        FileViewHolder(View v) {
            super(v);
            mName = (TextView) v.findViewById(R.id.name);
            mSize = (TextView) v.findViewById(R.id.size);
            mIcon = (ImageView) v.findViewById(R.id.icon);
        }
    }

    public interface OnFileItemListener {
        void onClick(File file);

        void onLongClick(File file);
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
}
