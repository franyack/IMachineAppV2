package com.example.fran.imachineappv2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.fran.imachineappv2.FilesManager.FilesMainActivity;

import java.io.File;


public class ResultsActivityView extends Activity implements ResultsActivityMvpView {

    private ResultsActivityMvpPresenter presenter;

    String pathFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pathFolder = (String) getIntent().getStringExtra("pathFolder");
        presenter = new ResultsActivityPresenter(this);
        setContentView(R.layout.results);
    }

    public void backToMainActivity(String dstFolder) {
        if(dstFolder.equals("")) {
            Toast.makeText(getApplicationContext(), getString(R.string.resultdescardedToast), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),getString(R.string.resultsavedToast) + dstFolder, Toast.LENGTH_SHORT).show();
        }
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    public void confirmResults(View view) {
        presenter.confirmResults(pathFolder, ResultsActivityView.this);
    }

    public void deleteResults(View view) {
        presenter.deleteResults(pathFolder, ResultsActivityView.this);
    }

    public void backToEditions(View view) {
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFolder);
        startActivity(i);
    }
}
