package com.example.vmac.WatBot.authentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Credentials;
import android.net.Uri;
import android.net.wifi.hotspot2.pps.Credential;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.vmac.WatBot.MainActivity;
import com.example.vmac.WatBot.R;
import com.example.vmac.WatBot.WavRecorder;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Register extends AppCompatActivity {
    private Button play, stop, record;
    WavRecorder wavRecorder;
//    private MediaRecorder myAudioRecorder;
////    private String outputFile;
private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        wavRecorder = new WavRecorder(Environment.getExternalStorageDirectory()+"/"+"voice1.wav");
        mStorageRef = FirebaseStorage.getInstance().getReference();

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        record = (Button) findViewById(R.id.record);
        stop.setEnabled(false);
        play.setEnabled(false);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.startRecording();
                                record.setEnabled(false);
                stop.setEnabled(true);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.stopRecording();
                                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                Uri file = Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/"+"voice1.wav"));
                StorageReference riversRef = mStorageRef.child("authentication/voice.wav");

                riversRef.putFile(file)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                                Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                                while(!downloadUrl.isComplete());
                                Uri url = downloadUrl.getResult();

                                Toast.makeText(getApplicationContext(), "Upload Success, download URL " +
                                        url.toString(), Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                            }
                        });
               // new SpeechCounter().execute();

            }
        });

    }
}
