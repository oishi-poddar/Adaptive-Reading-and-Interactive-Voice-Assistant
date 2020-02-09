package com.example.vmac.WatBot.authentication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.vmac.WatBot.Favourites;
import com.example.vmac.WatBot.FileViewer1;
import com.example.vmac.WatBot.Message;
import com.example.vmac.WatBot.QRScanner;
import com.example.vmac.WatBot.R;
import com.example.vmac.WatBot.SplashActivity;
import com.example.vmac.WatBot.WavRecorder;
import com.example.vmac.WatBot.home;
import com.example.vmac.WatBot.models.SavedDocs;
import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.mapzen.speakerbox.Speakerbox;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;
import static com.example.vmac.WatBot.Favourites.adapter;
import static com.example.vmac.WatBot.Favourites.savedDocs;

public class Login extends AppCompatActivity {

    Context mcontext;
    Button login;
    Button register;
    Speakerbox speakerbox;
    WavRecorder wavRecorder;
    WatsonSettings watsonSettings = WatsonSettings.getInstance();
    String micInput;
    String inputmessage;
    String loginVoiceUrl;
    String profId;
    String loginVoiceName;
    String result;
    Boolean x=false;
    private StorageReference mStorageRef;

    int i=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mcontext = getApplicationContext();
        login = (Button) findViewById(R.id.login);
        watsonSettings.setup(mcontext);
        loginVoiceName = "voice"+new Date() +".wav";
        wavRecorder = new WavRecorder(Environment.getExternalStorageDirectory()+"/"+loginVoiceName);
        speakerbox = new Speakerbox(getApplication());
        speakerbox.unmute();
        speakerbox.play("Welcome to gradbank. You can register your voice , if you are a first time user or simply login. Press the volume down button to talk to our chatbot");
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                        speakerbox.mute();
                        if(!x) {
                            startActivity(new Intent(getApplicationContext(), home.class));
                            finish();
                        }
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        wavRecorder.startRecording();
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, 12);
                        } else {
                            Toast.makeText(getApplicationContext(), "Your device does not support input speech!!", Toast.LENGTH_SHORT).show();
                        }

            }
        });
        register = (Button) findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakerbox.mute();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                wavRecorder.startRecording();
                if(intent.resolveActivity(getPackageManager())!=null){
                    startActivityForResult(intent,11);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Your device does not support input speech!!", Toast.LENGTH_SHORT).show();
                }
                i=1;



            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode == RESULT_OK && data!=null){
                    speakerbox.mute();
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    micInput = result.get(0);
                    sendMessage();
                    Toast.makeText(getApplicationContext(),micInput,Toast.LENGTH_LONG).show();
                }
                break;
            case 11:
                if(resultCode == RESULT_OK && data!=null){
                    speakerbox.mute();
                    wavRecorder.stopRecording();
                    File audioFile = new File(Environment.getExternalStorageDirectory()+"/"+loginVoiceName);
                    Uri file = Uri.fromFile(audioFile);
                    StorageReference riversRef = mStorageRef.child("authentication/"+loginVoiceName);

                    riversRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Get a URL to the uploaded content
                                    Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                                    while(!downloadUrl.isComplete());
                                    Uri url = downloadUrl.getResult();
                                    loginVoiceUrl = url.toString();
                                    Map<String, String> postData = new HashMap<>();
                                    postData.put("audio_link", loginVoiceUrl);

                                    HttpPostAsyncTask httpGetRequest = new HttpPostAsyncTask(postData);

                                        httpGetRequest.execute( "http://192.168.43.127:5000/enroll");


                                    Log.i(TAG,loginVoiceUrl);
                                    //startActivity(new Intent(mcontext, home.class));

                                    speakerbox.unmute();
//                                    try {
//                                        httpGetRequest.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"http://192.168.43.80:5000/verify").get();
//                                    } catch (ExecutionException e) {
//                                        e.printStackTrace();
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    // ...
                                }
                            });

                    audioFile.delete();
                    //sendMessage();


                }
                break;
            case 12:
                if(resultCode == RESULT_OK && data!=null){
                    speakerbox.mute();
                    wavRecorder.stopRecording();
                    File audioFile = new File(Environment.getExternalStorageDirectory()+"/"+loginVoiceName);
                    Uri file = Uri.fromFile(audioFile);
                    StorageReference riversRef = mStorageRef.child("authentication/"+loginVoiceName);

                    riversRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Get a URL to the uploaded content
                                    Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
                                    while(!downloadUrl.isComplete());
                                    Uri url = downloadUrl.getResult();
                                    loginVoiceUrl = url.toString();

                                    Map<String, ?> allEntries = getSharedPreferences("authentication",MODE_PRIVATE).getAll();
                                    Log.i(TAG,"size"+allEntries.size());
                                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                            profId = entry.getValue().toString();
                                    }
                                    profId = getSharedPreferences("authentication",Context.MODE_PRIVATE).getString("prof_id","Not Found");

                                    Map<String, String> postData = new HashMap<>();
                                    postData.put("audio_link", loginVoiceUrl);
                                    postData.put("prof_id",profId);
                                    HttpPostAsyncTask httpGetRequest = new HttpPostAsyncTask(postData);
                                    httpGetRequest.execute( "http://192.168.43.127:5000/verify");
                                        Log.i(TAG,loginVoiceUrl);
                                    //startActivity(new Intent(mcontext, home.class));

                                    speakerbox.unmute();
//                                    try {
//                                        httpGetRequest.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"http://192.168.43.80:5000/verify").get();
//                                    } catch (ExecutionException e) {
//                                        e.printStackTrace();
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    // ...
                                }
                            });

                    audioFile.delete();
                    //sendMessage();

                }
                break;

        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            speakerbox.mute();
            //speakerbox.play("Hi! I am Ariva");
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            if(intent.resolveActivity(getPackageManager())!=null){
                startActivityForResult(intent,10);
            }
            else{
                Toast.makeText(getApplicationContext(), "Your device does not support input speech!!", Toast.LENGTH_SHORT).show();
            }
        }
        else if(keyCode==KeyEvent.KEYCODE_BACK){
            speakerbox.mute();
            finish();
        }
        return true;
    }


    public class HttpPostAsyncTask extends AsyncTask<String, Void, String> {
        // This is the JSON body of the post
        JSONObject postData;
        // This is a constructor that allows you to pass in the JSON body
        public HttpPostAsyncTask(Map<String, String> postData) {
            if (postData != null) {
                this.postData = new JSONObject(postData);
            }
        }

        // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
        @Override
        protected String doInBackground(String... params) {

            try {
                // This is getting the url from the string we passed in
                URL url = new URL(params[0]);

                // Create the urlConnection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");


                // OPTIONAL - Sets an authorization header
                urlConnection.setRequestProperty("Authorization", "someAuthString");

                // Send the post body
                if (this.postData != null) {
                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(postData.toString());
                    writer.flush();
                }

                int statusCode = urlConnection.getResponseCode();

                if (statusCode ==  200) {

                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    result = convertInputStreamToString(inputStream);
                    if(result.toLowerCase().equals("accepted")){
                        startActivity(new Intent(mcontext, home.class));
                        speakerbox.unmute();
                        speakerbox.play("Successful");
                        finish();
                    }
                    else if(result.toLowerCase().equals("rejected")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mcontext,"Unauthorized",Toast.LENGTH_LONG).show();
                                speakerbox.unmute();
                                x=false;
                                speakerbox.play("Please try again");

                            }
                        });
                    }
                    else if(result.toLowerCase().contains("success")){
                        profId =result.substring(7);
                        SharedPreferences.Editor editor = getSharedPreferences("authentication", MODE_PRIVATE).edit();
                        editor.putString("prof_id", profId);
                        editor.apply();
                        Log.i(TAG,"fdsf"+result);
                    }

                    // From here you can convert the string to JSON with whatever JSON parser you like to use
                    // After converting the string to JSON, I call my custom callback. You can follow this process too, or you can implement the onPostExecute(Result) method
                } else {
                    // Status code is not 200
                    // Do something to handle the error
                    speakerbox.unmute();
                    speakerbox.play("Please try again");
                }

            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }
    }
    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    private void sendMessage() {
        Thread thread = new Thread(new Runnable() {
            public void run() {

                inputmessage = micInput;
                Log.i(TAG,"ffhg"+inputmessage);
                try {

                    if (watsonSettings.watsonAssistantSession == null) {
                        ServiceCall<SessionResponse> call = watsonSettings.watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mcontext.getString(R.string.assistant_id)).build());
                        watsonSettings.watsonAssistantSession = call.execute().getResult();
                    }

                    MessageInput input = new MessageInput.Builder()
                            .text(inputmessage)
                            .build();
                    MessageOptions options = new MessageOptions.Builder()
                            .assistantId(mcontext.getString(R.string.assistant_id))
                            .input(input)
                            .sessionId(watsonSettings.watsonAssistantSession.getSessionId())
                            .build();
                    MessageResponse response = watsonSettings.watsonAssistant.message(options).execute().getResult();

                    Log.i(TAG, "run: " + response);

                    final Message outMessage = new Message();
                    if (response != null &&
                            response.getOutput() != null &&
                            !response.getOutput().getGeneric().isEmpty() &&
                            "text".equals(response.getOutput().getGeneric().get(0).getResponseType())) {
                        outMessage.setMessage(response.getOutput().getGeneric().get(0).getText());
                        outMessage.setId("2");
                        Log.i(TAG, outMessage.getMessage());
                        speakerbox.unmute();

//                        speakerbox.mute();
                        String intent = response.getOutput().getIntents().size() != 0 ? (response.getOutput().getIntents().get(0).getIntent()) : "";

                        if (intent.toLowerCase().equals("login")) {
                            Log.i(TAG,"login");

                            speakerbox.play(outMessage.getMessage());
                            x=true;
                            login.callOnClick();
                        }else if(intent.toLowerCase().equals("register")){
                            speakerbox.play(outMessage.getMessage());
                            register.callOnClick();
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
}


