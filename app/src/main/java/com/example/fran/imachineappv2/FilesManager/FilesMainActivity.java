package com.example.fran.imachineappv2.FilesManager;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.fran.imachineappv2.FilesManager.Dialogs.AddItemsDialog;
import com.example.fran.imachineappv2.FilesManager.Dialogs.ConfirmDeleteDialog;
import com.example.fran.imachineappv2.FilesManager.Dialogs.NewFolderDialog;
import com.example.fran.imachineappv2.FilesManager.Dialogs.NewTextFileDialog;
import com.example.fran.imachineappv2.FilesManager.Dialogs.RenameDialog;
import com.example.fran.imachineappv2.FilesManager.Dialogs.UpdateItemDialog;
import com.example.fran.imachineappv2.R;
import com.example.fran.imachineappv2.ResultsActivityView;
import com.mzelzoghbi.zgallery.ZGallery;
import com.mzelzoghbi.zgallery.entities.ZColor;
import com.snatik.storage.EncryptConfiguration;
import com.snatik.storage.Storage;
import com.snatik.storage.helpers.OrderType;
import com.snatik.storage.helpers.SizeUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.example.fran.imachineappv2.FilesManager.Helper.fileExt;

/**
 * Based on: https://github.com/sromku/android-storage
 */

public class FilesMainActivity extends AppCompatActivity implements
        FilesAdapter.OnFileItemListener,
        AddItemsDialog.DialogListener,
        UpdateItemDialog.DialogListener,
        NewFolderDialog.DialogListener,
        ConfirmDeleteDialog.ConfirmListener,
        RenameDialog.DialogListener {

    private RecyclerView mRecyclerView;
    private FilesAdapter mFilesAdapter;
    private Storage mStorage;
    private TextView mPathView;
    private TextView mMovingText;
    private boolean mCopy;
    private View mMovingLayout;
    private int mTreeSteps = 0;
    private String mMovingPath;
    private boolean mInternal = false;
    String pathFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: wizard sobre diferentes opciones de administracion?
        super.onCreate(savedInstanceState);
        pathFolder = (String) getIntent().getStringExtra("pathFolder");
        mStorage = new Storage(getApplicationContext());



        setContentView(R.layout.filemanager_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mPathView = (TextView) findViewById(R.id.path);
        mMovingLayout = findViewById(R.id.moving_layout);
        mMovingText = (TextView) mMovingLayout.findViewById(R.id.moving_file_name);



        mMovingLayout.findViewById(R.id.accept_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMovingLayout.setVisibility(View.GONE);
                if (mMovingPath != null) {

                    if (!mCopy) {
                        String toPath = getCurrentPath() + File.separator + mStorage.getFile(mMovingPath).getName();
                        if (!mMovingPath.equals(toPath)) {
                            mStorage.move(mMovingPath, toPath);
                            Helper.showSnackbar(getString(R.string.moved), mRecyclerView);
                            showFiles(getCurrentPath());
                        } else {
                            Helper.showSnackbar(getString(R.string.file_already_here), mRecyclerView);
                        }
                    } else {
                        String toPath = getCurrentPath() + File.separator + "copy " + mStorage.getFile(mMovingPath)
                                .getName();
                        mStorage.copy(mMovingPath, toPath);
                        Helper.showSnackbar(getString(R.string.copied), mRecyclerView);
                        showFiles(getCurrentPath());
                    }
                    mMovingPath = null;
                }
            }
        });

        mMovingLayout.findViewById(R.id.decline_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMovingLayout.setVisibility(View.GONE);
                mMovingPath = null;
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mFilesAdapter = new FilesAdapter(getApplicationContext());
        mFilesAdapter.setListener(this);
        mRecyclerView.setAdapter(mFilesAdapter);
//        layoutManager.scrollToPosition(0);

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddItemsDialog.newInstance().show(getFragmentManager(), "add_items");
            }
        });

        mPathView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPathMenu();
            }
        });

        // load files
//        showFiles(mStorage.getExternalStorageDirectory());
//        showFiles("/storage/emulated/0/clusterResult");
        showFiles(pathFolder);

//        checkPermission();
    }

    private void showPathMenu() {
        PopupMenu popupmenu = new PopupMenu(this, mPathView);
        MenuInflater inflater = popupmenu.getMenuInflater();
        inflater.inflate(R.menu.path_menu, popupmenu.getMenu());

        popupmenu.getMenu().findItem(R.id.go_internal).setVisible(!mInternal);
        popupmenu.getMenu().findItem(R.id.go_external).setVisible(mInternal);

        popupmenu.show();

        popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.go_up:
                        String previousPath = getPreviousPath();
                        mTreeSteps = 0;
                        showFiles(previousPath);
                        break;
                    case R.id.go_internal:
                        showFiles(mStorage.getInternalFilesDirectory());
                        mInternal = true;
                        break;
                    case R.id.go_external:
                        showFiles(mStorage.getExternalStorageDirectory());
                        mInternal = false;
                        break;
                }
                return true;
            }
        });
    }

    private void showFiles(String path) {
        mPathView.setText(path);
        List<File> files = mStorage.getFiles(path);
        if (files != null) {
            Collections.sort(files, OrderType.NAME.getComparator());
        }
        mFilesAdapter.setFiles(files);
        mFilesAdapter.notifyDataSetChanged();
        mRecyclerView.getLayoutManager().scrollToPosition(2);
        
    }


    @Override
    public void onClick(File file) {
        if (file.isDirectory()) {
            mTreeSteps++;
            String path = file.getAbsolutePath();
            showFiles(path);
        } else {

            try {
                ArrayList<String> images = new ArrayList<>();
                File[] files = file.getParentFile().listFiles();
                if (files != null) {
                    Collections.sort(Arrays.asList(files), OrderType.NAME.getComparator());
                }
                for(final File imageFile : files){
                    if(!imageFile.isDirectory()){
                        if ((imageFile.getAbsolutePath().contains(".jpg") || imageFile.getAbsolutePath().contains(".gif") || imageFile.getAbsolutePath().contains(".bmp")
                                || imageFile.getAbsolutePath().contains(".jpeg") || imageFile.getAbsolutePath().contains(".tif") || imageFile.getAbsolutePath().contains(".tiff")
                                || imageFile.getAbsolutePath().contains(".png"))){
                            images.add(imageFile.getAbsolutePath());
                        }
                    }
                }
                int position=0;
                for(int i=0;i<images.size();i++){
                    if(images.get(i).equals(file.getAbsolutePath())){
                        position = i;
                        break;
                    }
                }
                ZGallery.with(this, images)
                        .setToolbarTitleColor(ZColor.WHITE) // toolbar title color
                        .setSelectedImgPosition(position)
                        .setGalleryBackgroundColor(ZColor.WHITE) // activity background color
                        .setToolbarColorResId(R.color.colorPrimary) // toolbar color
                        .setTitle(file.getParentFile().getName()) // toolbar title
                        .show();
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                String mimeType =  MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt(file.getAbsolutePath()));
//                Uri apkURI = FileProvider.getUriForFile(
//                        this,
//                        getApplicationContext()
//                                .getPackageName() + ".provider", file);
//                intent.setDataAndType(apkURI, mimeType);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                if (mStorage.getSize(file, SizeUnit.KB) > 500) {
                    Helper.showSnackbar(getString(R.string.file_too_big), mRecyclerView);
                    return;
                }
                Intent intent = new Intent(this, ViewTextActivity.class);
                intent.putExtra(ViewTextActivity.EXTRA_FILE_NAME, file.getName());
                intent.putExtra(ViewTextActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
                startActivity(intent);
            }

        }
    }

    @Override
    public void onLongClick(File file) {
        UpdateItemDialog.newInstance(file.getAbsolutePath()).show(getFragmentManager(), "update_item");
    }

    @Override
    public void onBackPressed() {
        if (mTreeSteps > 0) {
            String path = getPreviousPath();
            mTreeSteps--;
            showFiles(path);
            return;
        }else{
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
        super.onBackPressed();
    }

    private String getCurrentPath() {
        return mPathView.getText().toString();
    }

    private String getPreviousPath() {
        String path = getCurrentPath();
        int lastIndexOf = path.lastIndexOf(File.separator);
        if (lastIndexOf < 0) {
            Helper.showSnackbar("Can't go anymore", mRecyclerView);
            return getCurrentPath();
        }
        return path.substring(0, lastIndexOf);
    }

    @Override
    public void onOptionClick(int which, String path) {
        switch (which) {
            case R.id.new_folder:
                NewFolderDialog.newInstance().show(getFragmentManager(), "new_folder_dialog");
                break;
            case R.id.delete:
                ConfirmDeleteDialog.newInstance(path).show(getFragmentManager(), "confirm_delete");
                break;
            case R.id.rename:
                RenameDialog.newInstance(path).show(getFragmentManager(), "rename");
                break;
            case R.id.move:
                mMovingText.setText(getString(R.string.moving_file, mStorage.getFile(path).getName()));
                mMovingPath = path;
                mCopy = false;
                mMovingLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.copy:
                mMovingText.setText(getString(R.string.copy_file, mStorage.getFile(path).getName()));
                mMovingPath = path;
                mCopy = true;
                mMovingLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.confirmResults:
                Intent i = new Intent(this, ResultsActivityView.class);
                i.putExtra("pathFolder",pathFolder);
                startActivity(i);
        }
    }

    @Override
    public void onNewFolder(String name) {
        String currentPath = getCurrentPath();
        String folderPath = currentPath + File.separator + name;
        boolean created = mStorage.createDirectory(folderPath);
        if (created) {
            showFiles(currentPath);
            Helper.showSnackbar(getString(R.string.new_folder_created) + name, mRecyclerView);
        } else {
            Helper.showSnackbar(getString(R.string.new_folder_created_fail) + name, mRecyclerView);
        }
    }

    @Override
    public void onConfirmDelete(String path) {
        if (mStorage.getFile(path).isDirectory()) {
            mStorage.deleteDirectory(path);
            Helper.showSnackbar(getString(R.string.folderDeleted), mRecyclerView);
        } else {
            mStorage.deleteFile(path);
            Helper.showSnackbar(getString(R.string.fileDeleted), mRecyclerView);
        }
        showFiles(getCurrentPath());
    }

    @Override
    public void onRename(String fromPath, String toPath) {
        mStorage.rename(fromPath, toPath);
        showFiles(getCurrentPath());
        Helper.showSnackbar(getString(R.string.renamed), mRecyclerView);
    }

}
