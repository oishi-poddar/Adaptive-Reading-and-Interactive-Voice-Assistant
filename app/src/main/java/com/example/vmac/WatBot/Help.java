package com.example.vmac.WatBot;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.vmac.WatBot.models.SavedDocs;
import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.example.vmac.WatBot.singleton.SpeakerManager;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.mapzen.speakerbox.Speakerbox;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;
import static com.example.vmac.WatBot.Favourites.adapter;
import static com.example.vmac.WatBot.Favourites.savedDocs;

public class Help extends AppCompatActivity {
    Speakerbox speakerbox;
    String micInput;
    int currIndex;
    SpeechCounter speechCounter;
    String fileData,inputmessage,previousIntent;
    WatsonSettings watsonSettings;
    String pdfUrl;
    Context mcontext;
    SpeakerManager speakerManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        final WebView webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        speakerManager = SpeakerManager.getInstance();
        speakerManager.setup(getApplicationContext());
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mcontext = getApplicationContext();
        watsonSettings = WatsonSettings.getInstance();
        watsonSettings.setup(getApplicationContext());
        webView.setWebViewClient(new Callback());
        webView.getSettings().setBuiltInZoomControls(true);
        currIndex=0;
        speakerbox = new Speakerbox(getApplication());
        pdfUrl = "http://storage.googleapis.com/gradhack-v1/help/FAQs.pdf";
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl(
                getResources().getString(R.string.googleDocURL)+"&url="+pdfUrl);
        String myUrl = getResources().getString(R.string.pdfReaderURL)+pdfUrl;

        HttpGetRequest getFileDataRequest = new HttpGetRequest();
        try {
            fileData = getFileDataRequest.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myUrl).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fileData = fileData.concat("This is the end of your document. Thank you for your time");
//            speakerManager.speak(fileData);
        speakerbox.unmute();
        speakerbox.play(fileData.substring(fileData.indexOf("\"0\"")+3,fileData.indexOf("end")));
        if(!speakerbox.isMuted()){
            speechCounter = new SpeechCounter();
            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }
    private class Callback extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view, String url) {
            return (false);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode == RESULT_OK && data!=null){
//                    speakerManager.pause();

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    micInput = result.get(0);
                    sendMessage();
                    Toast.makeText(getApplicationContext(),micInput,Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private class SpeechCounter extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            while (true) {

                currIndex++;
                Log.i("Counter", "" + currIndex);
                try {
                    Thread.sleep(50); // sleep for 450 milliseconds
                    if (currIndex == fileData.length()) {
                        return "Finished";
                    }
                } catch (InterruptedException e) {
                    System.out.println("Thread is interrupted");
                    return "Stopped";
                }
            }
        }
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.favs) {
            // do something here
            Intent intent = new Intent(getApplicationContext(), Favourites.class);
            startActivity(intent);
            speakerbox.mute();
            finish();
        }
        if (id == R.id.logout) {
            // do something here
            Toast.makeText(getApplicationContext(),"Logged out",Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            speakerbox.mute();
            if(speechCounter.getStatus()== AsyncTask.Status.RUNNING)
                speechCounter.cancel(true);
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
    private void sendMessage() {


        inputmessage = micInput;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (previousIntent != null && previousIntent.equals("savedocument")) {
                        inputmessage = "savefilename";
                        Log.i(TAG, "in first if" + inputmessage + " mic msg: " + micInput);
                    }
                    else if (previousIntent != null && previousIntent.equals("count_of_particular_transaction")) {
                        inputmessage = "merchantName";
                        Log.i(TAG, "in second if" + inputmessage + " mic msg: " + micInput);
                    }
                    else if (previousIntent != null && previousIntent.equals("transaction_details")) {
                        inputmessage = "merchantName";
                        Log.i(TAG, "in third if" + inputmessage + " mic msg: " + micInput);
                    }
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
                    String intent="";
                    final Message outMessage = new Message();
                    if (response != null &&
                            response.getOutput() != null &&
                            !response.getOutput().getGeneric().isEmpty() &&
                            "text".equals(response.getOutput().getGeneric().get(0).getResponseType())) {
                        outMessage.setMessage(response.getOutput().getGeneric().get(0).getText());
                        outMessage.setId("2");
                        Log.i(TAG, outMessage.getMessage());
                        speakerbox.unmute();
                        speakerbox.play(outMessage.getMessage());
                        intent = response.getOutput().getIntents().size() != 0 ? (response.getOutput().getIntents().get(0).getIntent()) : "";

                        if (intent.equals("rewind")) {
                            currIndex = currIndex-40;
                            if(currIndex<0){
                                currIndex=0;
                            }
                            speakerbox.unmute();
                            Log.i(TAG, "Rewind: "+currIndex);
                            speakerbox.play(fileData.substring(currIndex));
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else if (intent.equals("forward")) {

                            currIndex = currIndex+40;
                            if(currIndex>fileData.length()){
                                currIndex=0;
                            }
                            Log.i(TAG, "Forward: "+currIndex);
                            speakerbox.unmute();
                            speakerbox.play(fileData.substring(currIndex));
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else if (intent.equals("play")) {
                            Log.i(TAG, "Playing");
                            speakerbox.unmute();
                            speakerbox.play(fileData.substring(currIndex));
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                        else if(intent.equals("about_app")){
                            String result = new CallDiscovery().execute("What is the app about?").get();
                            result = result.replace("<em>","");
                            result = result.replace("</em>","");
                            speakerbox.unmute();
                            speakerbox.play(result);
                            //speakerbox.mute();
                        }
                        else if(intent.equals("FAQ1")){
                            String result = new CallDiscovery().execute("How do I open a Current / Savings Account with HSBC?").get();
                            result = result.replace("<em>","");
                            result = result.replace("</em>","");
                            speakerbox.unmute();
                            speakerbox.play(result);
                            //speakerbox.mute();
                        }
                        else if(intent.equals("FAQ2")){
                            String result = new CallDiscovery().execute("What is a chip enabled debit card?").get();
                            result = result.replace("<em>","");
                            result = result.replace("</em>","");
                            speakerbox.unmute();
                            speakerbox.play(result);
                            //speakerbox.mute();
                        }
                        else if(intent.equals("FAQ3")){
                            String result = new CallDiscovery().execute("Do I have to ask for a new PIN for the replaced debit card?").get();
                            result = result.replace("<em>","");
                            result = result.replace("</em>","");
                            speakerbox.unmute();
                            speakerbox.play(result);
                            //speakerbox.mute();
                        }
                        else if(intent.equals("home")){
                            startActivity(new Intent(getApplicationContext(), home.class));
                        }


                        Log.i(TAG,"previous intent: "+previousIntent);



                    }
                    previousIntent = intent;
                    Log.i(TAG,"new prev intent"+previousIntent+"old intent: "+intent);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
}
