package com.bellone.simpletexteditor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private EditText txtFile;
    private EditText txtFileName;
    private ImageButton btnSave;

    private boolean permissionDenied;
    private String path;
    private String currentFilePath;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        currentFilePath = null;

        txtFile = findViewById(R.id.txtFile);
        txtFileName = findViewById(R.id.txtFileName);
        txtFileName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        btnSave = findViewById(R.id.btnSave);

        if(ContextCompat.checkSelfPermission(context, PERMISSIONS[0]) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }else{
            permissionDenied = false;
        }

        reset();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1 && grantResults.length > 0){
            permissionDenied = ((grantResults[0] == PackageManager.PERMISSION_DENIED)
                    || (grantResults[1] == PackageManager.PERMISSION_DENIED));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean returnValue = true;
        switch (item.getItemId()){
            case R.id.openFile:
                openFile();
                break;
            case R.id.clearAll:
                reset();
                break;
            case R.id.delete:
                if(currentFilePath != null) {
                    deleteThisFile();
                }else{
                    Toast.makeText(context, context.getString(R.string.toast_delete_no_file), Toast.LENGTH_LONG).show();
                }
                break;
            default:
                returnValue = super.onOptionsItemSelected(item);
                break;
        }
        return returnValue;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 2){
            if(resultCode == Activity.RESULT_OK){
                if(data != null){
                    Uri fileUri = data.getData();

                    //=================URI to String path===========================
                    //https://gist.github.com/crackjack/5494970
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(fileUri, proj,  null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    currentFilePath = cursor.getString(column_index);
                    cursor.close();
                    //=============================================================

                    String file = readFile(currentFilePath);
                    if(file != null) {
                        txtFile.setText(file);
                        txtFileName.setText(currentFilePath.substring(currentFilePath.lastIndexOf("/")+1));
                    }else{
                        Toast.makeText(context,
                                context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!permissionDenied) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (txtFile.length() > 0) {
                        if (txtFileName.length() > 0) {
                            saveFile(txtFileName.getText().toString(), txtFile.getText().toString());
                        } else {
                            Toast.makeText(context,
                                    context.getString(R.string.toast_save_no_name), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_save_no_text),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }else{
            Snackbar.make(findViewById(android.R.id.content), "GIVE ME THE PERMISSIONS !",
                    Snackbar.LENGTH_INDEFINITE).setAction("Ok...", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
                }
            }).show();
        }
    }

    private void saveFile(String fileName, String text){
        File file = new File(path+"/"+fileName);
        if(!file.exists()) {
            writeOnFile(file, text);
        }else{
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            writeOnFile(file, text);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(context.getString(R.string.overwriteFile))
                    .setPositiveButton(context.getString(R.string.askDialog_yes), dialogClickListener)
                    .setNegativeButton(context.getString(R.string.askDialog_no), dialogClickListener).show();
        }
    }

    private void writeOnFile(File file, String text){
        currentFilePath = file.getAbsolutePath();
        try {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            bw.write(text);

            bw.close();
            String message = context.getString(R.string.toast_saved);
            String msg = message.replace("Download", "<font color=\"red\">Download</font>");
            Toast.makeText(context, Html.fromHtml(msg), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
        }
    }

    private void openFile(){
        //https://o7planning.org/12725/create-a-simple-file-chooser-in-android

        Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFileIntent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");
        startActivityForResult(chooseFileIntent, 2);

        //( go on onActivityResult() )
    }

    private String readFile(String filePath){
        File file = new File(filePath);

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String row, entireFile = "";

            while((row = br.readLine()) != null){
                entireFile += row+"\n";
            }

            br.close();

            return entireFile;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteThisFile(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        File file = new File(currentFilePath);
                        file.delete();
                        reset();
                        Toast.makeText(context, context.getString(R.string.toast_delete), Toast.LENGTH_SHORT).show();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.deleteFile))
                .setPositiveButton(context.getString(R.string.askDialog_yes), dialogClickListener)
                .setNegativeButton(context.getString(R.string.askDialog_no), dialogClickListener).show();
    }

    private void reset(){
        txtFileName.setText("");
        txtFile.setText("");
        currentFilePath = null;
    }

}