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

    Button confirmResults,deleteResults,backToEditions;
    String pathFolder;
    String[] imagesPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pathFolder = (String) getIntent().getStringExtra("pathFolder");
        imagesPath = getIntent().getStringArrayExtra("imagesPath");
        presenter = new ResultsActivityPresenter(this);
        setContentView(R.layout.results);
//        confirmResults = (Button) findViewById(R.id.btnConfirmResults);
//        deleteResults = (Button) findViewById(R.id.btnDeleteResults);
//        backToEditions = (Button) findViewById(R.id.btnBackToEditions);
    }


//    public void generarCarpetas(View view) {
//        String pathFolder = Environment.getExternalStorageDirectory() + File.separator + "clusterResult";
//        presenter.folderGenerator(pathFolder);
//    }
    public void showFolderAlert(String pathFolder){
        Intent i = new Intent(this, FilesMainActivity.class);
        startActivity(i);
    }

    public void backToMainActivity(String dstFolder) {
        if(dstFolder.equals("")) {
            Toast.makeText(getApplicationContext(), "¡El resultado se ha descartado correctamente!", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"El resultado se ha guardado correctamente en el directorio: " + dstFolder, Toast.LENGTH_SHORT).show();
        }
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    public void confirmResults(View view) {
        //TODO: darle al usuario la posibilidad de elegir el path final?
        presenter.confirmResults(pathFolder, imagesPath, ResultsActivityView.this);
    }

    public void deleteResults(View view) {
        presenter.deleteResults(pathFolder);
    }

    public void backToEditions(View view) {
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFolder);
        startActivity(i);
    }
}
