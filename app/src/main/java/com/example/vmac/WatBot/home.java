package com.example.vmac.WatBot;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vmac.WatBot.authentication.Login;
import com.example.vmac.WatBot.models.HomeCard;
import com.example.vmac.WatBot.models.SavedDocs;
import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.example.vmac.WatBot.singleton.SpeakerManager;
import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryResponse;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;
import com.mapzen.speakerbox.Speakerbox;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import jp.wasabeef.blurry.Blurry;

import static android.content.ContentValues.TAG;
import static com.example.vmac.WatBot.Favourites.adapter;
import static com.example.vmac.WatBot.Favourites.savedDocs;

public class home extends AppCompatActivity {

    CardView qr;
    CardView fav;
    CardView profile;
    TextToSpeech textToSpeech;
    StreamPlayer streamPlayer;
    Context mcontext;
    WatsonSettings watsonSettings = WatsonSettings.getInstance();
    String micInput;
    String inputmessage;
    Speakerbox speakerbox;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager RecyclerViewLayoutManager;
    RecyclerViewAdapter RecyclerViewHorizontalAdapter;
    LinearLayoutManager HorizontalLayout ;

    //    TextView body;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mcontext = getApplicationContext();
//        ActionBar menu = getSupportActionBar();
        getSupportActionBar().setIcon(R.drawable.money);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        speakerbox = new Speakerbox(getApplication());
        speakerbox.unmute();
        speakerbox.play("Welcome to GradBank");
        speakerbox.mute();
        watsonSettings.setup(mcontext);
        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        RecyclerViewLayoutManager = new LinearLayoutManager(getApplicationContext());

        recyclerView.setLayoutManager(RecyclerViewLayoutManager);
        ArrayList<HomeCard> list = new ArrayList<>();
        list.add(new HomeCard("Need some help!!",R.drawable.question));
        list.add(new HomeCard("Call Customer Care!!",R.drawable.customer));
        RecyclerViewHorizontalAdapter = new RecyclerViewAdapter(list);

        HorizontalLayout = new LinearLayoutManager(home.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(HorizontalLayout);

        recyclerView.setAdapter(RecyclerViewHorizontalAdapter);



        qr = (CardView)findViewById(R.id.qr);
        fav = (CardView) findViewById(R.id.fav);
        profile = (CardView) findViewById(R.id.profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Profile",Toast.LENGTH_LONG).show();
            }
        });
//        body = (TextView)findViewById(R.id.description);
        LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
        layout.getBackground().setAlpha(100);
        qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakerbox.mute();
                startActivity(new Intent(getApplicationContext(),QRScanner.class));

            }
        });
        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakerbox.mute();
                startActivity(new Intent(getApplicationContext(),Favourites.class));
            }
        });
        speakerbox.unmute();
//        speakerbox.play(body.getText().toString());

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode == RESULT_OK && data!=null){
//                    speakerManager.pause();
                    speakerbox.mute();
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
    private class SayTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... params) {

            streamPlayer.playStream(textToSpeech.synthesize(new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                    .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                    .build()).execute().getResult());


            return "Did synthesize";
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            speakerbox.mute();
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
                        //SpeakerManager.getInstance().speak(outMessage.getMessage());
                        String intent = response.getOutput().getIntents().size() != 0 ? (response.getOutput().getIntents().get(0).getIntent()) : "";
                        speakerbox.unmute();
                        speakerbox.play(outMessage.getMessage());
                        if (intent.equals("openFavourites")) {
                            startActivity(new Intent(getApplicationContext(), Favourites.class));
                        }
                        else if(intent.equals("openQRScanner")){
                            startActivity(new Intent(getApplicationContext(), QRScanner.class));
                        }
                        else if(intent.equals("help")){
                            startActivity(new Intent(getApplicationContext(), Help.class));
                        }else if(intent.equals("log_out")){
                            startActivity(new Intent(getApplicationContext(), Login.class));
                        }else if(intent.equals("home")){
                            startActivity(new Intent(getApplicationContext(), home.class));
                        }else if(intent.equals("profile")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(),"Profile - to be implemented",Toast.LENGTH_LONG).show();
                                }
                            });
//                            startActivity(new Intent(getApplicationContext(), profile.class));
                        }
                        else if(response.getOutput().getEntities().get(0).getValue().equals("yes")){
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:04067173402"));//change the number
                            startActivity(callIntent);
                        }

                    }
                }
                catch (Exception e){

                }
            }
        });

        thread.start();

    }



}
