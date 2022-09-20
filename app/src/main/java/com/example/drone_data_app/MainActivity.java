package com.example.drone_data_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {

    Button select_file, upload;
    TextView notification;
    Uri fileUri; // Uri are URLs that are meant for local storage
    ProgressDialog progressDialog;

    FirebaseStorage storage; // used for uploading files.. Ex: PDF, txt
    FirebaseDatabase database; // used to store URLS of uploaded files

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = FirebaseStorage.getInstance(); // returns an object of Firebase storage
        database = FirebaseDatabase.getInstance(); // return an object of Firebase database

        // connecting components to their ids
        select_file = findViewById(R.id.selectFile);
        upload = findViewById(R.id.upload);
        notification = findViewById(R.id.notification);

        select_file.setOnClickListener(view -> {

            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
                selectFile();
            }
            else{
                // prompt for users to give permissions
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 9);
            }

        });

        // if upload button is clicked
        upload.setOnClickListener(v -> {

            if(fileUri != null) {
                // user has selected the file
                uploadFile(fileUri);
            }
            else { // file not selected
                Toast.makeText(MainActivity.this, "Select a file", Toast.LENGTH_SHORT).show();
            }

        });
    }

    // Method triggered from requestPermissions call
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // check to see if request code matches when user accepts permissions
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 9 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectFile();
        } else {
            Toast.makeText(MainActivity.this, "Please provide permissions", Toast.LENGTH_SHORT).show();
        }

    }

    // offers user to select any file from their Android's file manager
    private void selectFile() {

        // Will be using an Intent
        // use intents to request action from another app component

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT); // will fetch the files
        startActivityForResult(intent, 86);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // check whether the user has selected file or not

        //super.onActivityResult(requestCode, resultCode, data);

        if(requestCode== 86 && resultCode== RESULT_OK && data!= null) {
            fileUri = data.getData(); // returns the uri of selected file
            notification.setText("A file is selected: " + data.getData().getLastPathSegment());
        }
        else {
            Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadFile(Uri fileUri) {

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading file...");
        progressDialog.setProgress(0);
        progressDialog.show();

        final String fileName = System.currentTimeMillis()+"";
        StorageReference storageReference = storage.getReference(); // returns root path

        storageReference.child("Uploads").child(fileName).putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        // return url of uploaded file
                        String url = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();

                        // store url in real time database
                        DatabaseReference reference = database.getReference(); // return path to root

                        reference.child(fileName).setValue(url).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "File successfully uploaded", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Toast.makeText(MainActivity.this, "File NOT successfully uploaded", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(MainActivity.this, "File NOT successfully uploaded", Toast.LENGTH_SHORT).show();

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                        // track the progress of uploading file
                        int currentProgress = (int) (100* taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                        progressDialog.setProgress(currentProgress);
                    }
                });

    }

}