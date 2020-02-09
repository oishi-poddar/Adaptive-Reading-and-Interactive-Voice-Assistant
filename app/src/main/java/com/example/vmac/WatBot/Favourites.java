package com.example.vmac.WatBot;

//import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vmac.WatBot.authentication.Login;
import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.example.vmac.WatBot.models.SavedDocs;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.mapzen.speakerbox.Speakerbox;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Favourites extends AppCompatActivity {
    public static ArrayList<SavedDocs> savedDocs = new ArrayList<>();
    public static FavoritesAdapter adapter = new FavoritesAdapter(savedDocs);
    private String micInput;
    private WatsonSettings watsonSettings = WatsonSettings.getInstance();
    private Context mcontext;
    Speakerbox speakerBox;
    TextToSpeech tts;
    String inputmessage;
    String previousIntent;
    String openFileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);
        mcontext = getApplicationContext();
        watsonSettings.setup(mcontext);
        speakerBox = new Speakerbox(getApplication());
        Map<String, ?> allEntries = getSharedPreferences("Favourite Documents",MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            boolean repeat=false;
            for(int i=0;i<savedDocs.size();i++){
                if(savedDocs.get(i).getName().equals(entry.getKey())){
                    repeat=true;
                    break;
                }
            }
            if(!repeat)
                savedDocs.add(new SavedDocs(entry.getKey(),entry.getValue().toString()));
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }
        RecyclerView rvContacts = (RecyclerView) findViewById(R.id.recycler_view);
        rvContacts.setAdapter(adapter);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, 10);
            } else {
                Toast.makeText(getApplicationContext(), "Your device does not support input speech!!", Toast.LENGTH_SHORT).show();
            }


            Toast.makeText(getApplicationContext(), "Device shaken!", Toast.LENGTH_SHORT).show();
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK){
             super.onBackPressed();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode == RESULT_OK && data!=null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    micInput = result.get(0);
                    sendMessage();
                    Toast.makeText(getApplicationContext(),micInput,Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    private void sendMessage() {

        inputmessage = micInput;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if(previousIntent!=null && previousIntent.equals("select_document")){
                        inputmessage = "filename";
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
                    Log.i(TAG, "run: "+response);
                    final Message outMessage = new Message();
                    if (response != null &&
                            response.getOutput() != null &&
                            !response.getOutput().getGeneric().isEmpty() &&
                            "text".equals(response.getOutput().getGeneric().get(0).getResponseType())) {
                        outMessage.setMessage(response.getOutput().getGeneric().get(0).getText());
                        outMessage.setId("2");
                        Log.i(TAG,outMessage.getMessage());
                        speakerBox.unmute();
                        speakerBox.play(outMessage.getMessage());
                        String intent = response.getOutput().getIntents().size()!=0?(response.getOutput().getIntents().get(0).getIntent()):"";
                        previousIntent = intent;
                        if(intent.equals("openQRScanner")){
                            startActivity(new Intent(getApplicationContext(), QRScanner.class));
                        }
                        else if(intent.equals("openFavourites") && response.getOutput().getEntities().get(0).getValue()==""){
                            startActivity(new Intent(getApplicationContext(),Favourites.class));
                        }

                        else if(intent.equals("help")){
                            startActivity(new Intent(getApplicationContext(), Help.class));
                        }else if(intent.equals("log_out")){
                            startActivity(new Intent(getApplicationContext(), Login.class));
                        }else if(intent.equals("home")){
                            startActivity(new Intent(getApplicationContext(), home.class));
                        }
                        if(intent=="" && response.getOutput().getGeneric().get(0).getText().equals("Please tell me which action to perform : Open or Delete")){
                            openFileName = micInput+".pdf";
                            openFileName = openFileName.toLowerCase();
                        }
                        else if(response.getOutput().getEntities().get(0).getValue().equals("open")){
                            Intent i = new Intent(mcontext,FileViewer1.class);
                            Log.i(TAG,"openfilename"+openFileName);
                            String dataFile = mcontext.getSharedPreferences("Favourite Documents",Context.MODE_PRIVATE).getString(openFileName,"Not Found");
                            i.putExtra("url",dataFile);
//                            Log.i(TAG,"sasasa"+mcontext.getSharedPreferences("Favourite Documents",Context.MODE_PRIVATE).getString(openFileName,"Not Found"));
                            if(!dataFile.equals("Not Found"))
                                mcontext.startActivity(i);
                        }
                        else if(response.getOutput().getEntities().get(0).getValue().equals("delete")){
                            SharedPreferences.Editor editor = getSharedPreferences("Favourite Documents",Context.MODE_PRIVATE).edit();
                            editor.remove(openFileName);
                            editor.apply();
                            int i=0;
                            for(i=0;i<savedDocs.size();i++){
                                Log.i(TAG,""+savedDocs.get(i).getName());
                                if(savedDocs.get(i).getName().toLowerCase().equals(openFileName.toLowerCase())){
                                    Log.i(TAG,"in if");
                                    savedDocs.remove(savedDocs.get(i));
                                    break;
                                }
                            }
                            final int index = i;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyItemRemoved(index);
                                    adapter.notifyItemRangeChanged(index, savedDocs.size());
                                }
                            });

                        }
                        else if(response.getOutput().getEntities().get(0).getValue().equals("yes")){
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:04067173402"));//change the number
                            startActivity(callIntent);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
    public void removeFromSharedPreferences(String key){
        getSharedPreferences("Favourite Documents",MODE_PRIVATE).edit().remove(key+".pdf");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        menu.getItem(0).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.home) {
            // do something here
            startActivity(new Intent(getApplicationContext(),home.class));
            finish();
        }
        if (id == R.id.logout) {
            // do something here
            startActivity(new Intent(getApplicationContext(), Login.class));
            speakerBox.unmute();
            speakerBox.play("Logged out");
            finish();
            Toast.makeText(getApplicationContext(),"Logged out",Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

}
