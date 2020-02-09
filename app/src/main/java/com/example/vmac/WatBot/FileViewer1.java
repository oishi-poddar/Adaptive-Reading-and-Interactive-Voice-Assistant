package com.example.vmac.WatBot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.vmac.WatBot.authentication.Login;
import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.example.vmac.WatBot.models.SavedDocs;
import com.example.vmac.WatBot.singleton.SpeakerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;
import com.mapzen.speakerbox.Speakerbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;
import static com.example.vmac.WatBot.Favourites.adapter;
import static com.example.vmac.WatBot.Favourites.savedDocs;

public class FileViewer1 extends AppCompatActivity {

    private String pdfUrl;
    public String fileData;
    Context mcontext;
    int currIndex=0;
    SpeechCounter speechCounter;
    String fileName;
    WatsonSettings watsonSettings = WatsonSettings.getInstance();
    String micInput;
    String previousIntent;
    String inputmessage;
    String saveFileName;
    Speakerbox speakerbox;
    SpeakerManager speakerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer1);
        Log.i(TAG,"in fileViewer1");
        mcontext = getApplicationContext();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        speakerManager = SpeakerManager.getInstance();
        speakerManager.setup(getApplicationContext());
        watsonSettings.setup(mcontext);
        speakerbox = new Speakerbox(getApplication());
//        speakerbox.mute();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:04067173402"));//change the number
                startActivity(callIntent);
            }
        });
        pdfUrl = getIntent().getExtras().getString("url");
        Toast.makeText(getApplicationContext(),pdfUrl,Toast.LENGTH_LONG).show();

        final WebView webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.setWebViewClient(new Callback());
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl(
                getResources().getString(R.string.googleDocURL)+"&url=" + pdfUrl);
        fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/')+1);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("File Viewer");
        Button b = (Button) findViewById(R.id.save);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = getSharedPreferences("Favourite Documents", MODE_PRIVATE).edit();
                if(!getSharedPreferences("Favourite Documents", MODE_PRIVATE).contains(fileName)){
                    editor.putString(fileName, pdfUrl);
                    editor.apply();
                    savedDocs.add(new SavedDocs(fileName,pdfUrl));
                    adapter.notifyItemInserted(getSharedPreferences("Favourite Documents",MODE_PRIVATE).getAll().size() - 1);
                }
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("TEST", "LongPress");

                return true;
            }
        });


        String myUrl = getResources().getString(R.string.pdfReaderURL)+pdfUrl;

        HttpGetRequest getFileDataRequest = new HttpGetRequest();
        try {
            fileData = getFileDataRequest.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myUrl).get();
            fileData = fileData.concat("This is the end of your document. Thank you for your time");
//            speakerManager.speak(fileData);

            speakerbox.play(fileData);

            if(!speakerbox.isMuted()){
                speechCounter = new SpeechCounter();
                speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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

    private class Callback extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view, String url) {
            return (false);
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



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        menu.getItem(1).setVisible(false);
        return super.onCreateOptionsMenu(menu);
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
            startActivity(new Intent(getApplicationContext(), Login.class));
            speakerbox.unmute();
            speakerbox.play("Logged out");
            finish();
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
                    else if (previousIntent != null && (previousIntent.equals("transaction_details") || previousIntent.equals("sum_transaction_merchant"))) {
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

                        if(intent.equals("openQRScanner")){//working
                            startActivity(new Intent(getApplicationContext(), QRScanner.class));
                        } else if (intent.equals("openFavourites")) {//working
                            startActivity(new Intent(getApplicationContext(), Favourites.class));
                        } else if (intent.equals("rewind")) {//working
                             currIndex = currIndex-40;
                            if(currIndex<0){
                                currIndex=0;
                            }
                            speakerbox.unmute();
                            int temp = currIndex;
                            Log.i(TAG, "Rewind: "+currIndex);
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            speakerbox.play(fileData.substring(temp));
                        } else if (intent.equals("forward")) {//working

                             currIndex = currIndex+40;
                            if(currIndex>fileData.length()){
                                currIndex=0;
                            }
                            Log.i(TAG, "Forward: "+currIndex);
                            int temp = currIndex;
                            speakerbox.unmute();
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            speakerbox.play(fileData.substring(temp));
                        } else if (intent.equals("play")) {//working
                            Log.i(TAG, "Playing");
                            speakerbox.unmute();
                            int temp = currIndex;
                            speechCounter = new SpeechCounter();
                            speechCounter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            speakerbox.play(fileData.substring(temp));
                        }
                        else if (intent.equals("pause")) {
                            Log.i(TAG, "Paused");
//                            speakerbox.mute();
                            speechCounter.cancel(true);
                        }

                        else if(intent.equals("help")){
                            startActivity(new Intent(getApplicationContext(), Help.class));
                        }else if(intent.equals("log_out")){
                            startActivity(new Intent(getApplicationContext(), Login.class));
                        }else if(intent.equals("home")){
                            startActivity(new Intent(getApplicationContext(), home.class));
                        }
                        else if(intent.equals("bill")){//working
                            String query = "Select sum(amount) from transaction inner join document on document.transaction_id=transaction.transaction_id where docId=(select distinct(docId) from document where docUrl='"+pdfUrl+"')";
                            Log.i(TAG,query);
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output==null){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Rupees "+output+"is your bill");
                            Log.i(TAG,output);
                        }
                        else if(intent.equals("volume_up")){//working
                            speakerManager.increaseVolume();
                            Log.i(TAG,"here");
                        } else if (intent.equals("volume_down")) {//working
                            speakerManager.decreaseVolume();
                        }
                        else if(intent.equals("max_amount")){//working
                            String query = "select max(amount) from Joined where docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your highest transaction was: "+output);
                            Log.i(TAG,output);
                        }else if(intent.equals("minimum_amount")){//working
                            String query = "select min(amount) from Joined where docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your lowest transaction was: "+output);
                            Log.i(TAG,output);
                        }
                        else if(intent.equals("credit_limit")){//working
                            String query = "select distinct(credit_limit) from credit_card inner join Joined on Joined.account_id=credit_card.account_id  where docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your Credit limit is: "+output);
                            Log.i(TAG,output);
                        }
                        else if(intent.equals("account_number")){//working
                            String query = "select distinct(account_no) from account inner join Joined on account.account_id=Joined.account_id where docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your account number is: "+output);
                            Log.i(TAG,output);
                        }
                        else if(intent.equals("balance")){//working
                            String query = "select distinct(balance) from document where docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your account balance is: "+output);
                            Log.i(TAG,output);
                        }

                        Log.i(TAG,"previous intent: "+previousIntent);
                        //working
                        if (previousIntent!=null && previousIntent.equals("savedocument") && intent == "" && response.getOutput().getGeneric().get(0).getText().equals("Document Saved !!")) {
                            saveFileName = micInput + ".pdf";
                            saveFileName = saveFileName.toLowerCase();
                            Log.i(TAG, "new file name is ::::: " + saveFileName);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SharedPreferences.Editor editor = getSharedPreferences("Favourite Documents", MODE_PRIVATE).edit();
                                    if (!getSharedPreferences("Favourite Documents", MODE_PRIVATE).contains(saveFileName)) {
                                        editor.putString(saveFileName, pdfUrl);
                                        editor.apply();
                                        savedDocs.add(new SavedDocs(saveFileName, pdfUrl));
                                        adapter.notifyItemInserted(getSharedPreferences("Favourite Documents", MODE_PRIVATE).getAll().size() - 1);
                                    }
                                }
                            });

                        }
                        //working
                        else if( previousIntent!=null && previousIntent.equals("customer_rep") && response.getOutput().getEntities().size()>0 && response.getOutput().getEntities().get(0).getValue().equals("yes")){
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:04067173402"));//change the number
                            startActivity(callIntent);
                        }
                        //working
                        else if(previousIntent!=null && previousIntent.equals("count_of_particular_transaction") && response.getOutput().getGeneric().get(0).getText().equals("Fetching...")){
                            String query = "select count(transaction_name) from Joined where transaction_name='"+micInput+"' and docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Number of transactions for "+micInput+" are: "+output);
                            Log.i(TAG,output);
                        }
                        //working
                        else if(previousIntent!=null && previousIntent.equals("transaction_details") && response.getOutput().getGeneric().get(0).getText().equals("Fetching...")){
                            String query = "select date, amount from Joined where transaction_name='"+micInput+"' and docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Your "+micInput+" transaction details are as follows!! "+output+" rupees");
                            Log.i(TAG,output);
                        }
                        //working
                        else if(previousIntent!=null && previousIntent.equals("sum_transaction_merchant")){
                            String query = "select sum(amount) from Joined where transaction_name='"+micInput+"' and docUrl='"+pdfUrl+"'";
                            String output = new HttpGetRequest().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"http://192.168.43.127:8080/get?param="+query).get();
                            speakerbox.unmute();
                            if(output.length()==0){
                                output = "Sorry, couldnt fetch. Please try again after some time";
                            }
                            speakerbox.play("Total sum of transactions for "+micInput+" are: "+output);
                            Log.i(TAG,output);
                        }


                    }
                    Log.i(TAG,"new prev intent"+previousIntent+"old intent: "+intent);

                    previousIntent = intent;


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }



}



