package com.example.vmac.WatBot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

  private String temp = "";
  private String myREsponse;
  private RecyclerView recyclerView;
  private ChatAdapter mAdapter;
  private ArrayList messageArrayList;
  private EditText inputMessage;
  private ImageButton btnSend;
  private ImageButton btnRecord;
  StreamPlayer streamPlayer = new StreamPlayer();
  private boolean initialRequest;
  private boolean permissionToRecordAccepted = false;
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  private static String TAG = "MainActivity";
  private static final int RECORD_REQUEST_CODE = 101;
  private boolean listening = false;
  private MicrophoneInputStream capture;

  private MicrophoneHelper microphoneHelper;//https://github.com/IBM/watson-voice-bot/tree/master/data
  private Context mContext;
  String result;
  private WatsonSettings watsonSettings;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mContext = getApplicationContext();
    watsonSettings = WatsonSettings.getInstance();
    inputMessage = findViewById(R.id.message);
    btnSend = findViewById(R.id.btn_send);
    btnRecord = findViewById(R.id.btn_record);
    String customFont = "Montserrat-Regular.ttf";
    Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
    inputMessage.setTypeface(typeface);
    recyclerView = findViewById(R.id.recycler_view);

    messageArrayList = new ArrayList<>();
    mAdapter = new ChatAdapter(messageArrayList);
    microphoneHelper = new MicrophoneHelper(this);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(mAdapter);
    this.inputMessage.setText("");
    this.initialRequest = true;


    int permission = ContextCompat.checkSelfPermission(this,
      Manifest.permission.RECORD_AUDIO);

    if (permission != PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission to record denied");
      makeRequest();
    } else {
      Log.i(TAG, "Permission to record was already granted");
    }


//    recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
//      @Override
//      public void onClick(View view, final int position) {
//        Message audioMessage = (Message) messageArrayList.get(position);
//        if (audioMessage != null && !audioMessage.getMessage().isEmpty()) {
////          new SayTask().execute(audioMessage.getMessage());
//        }
//      }
//
//      @Override
//      public void onLongClick(View view, int position) {
//        recordMessage();
//        Toast.makeText(mContext,myREsponse,Toast.LENGTH_LONG);
//      }
//    }));

    btnSend.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (checkInternetConnection()) {
          sendMessage();
        }
      }
    });

    btnRecord.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        recordMessage();
      }
    });
    watsonSettings.setup(mContext);
  }

  ;

  // Speech-to-Text Record Audio permission
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_RECORD_AUDIO_PERMISSION:
        permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        break;
      case RECORD_REQUEST_CODE: {

        if (grantResults.length == 0
          || grantResults[0] !=
          PackageManager.PERMISSION_GRANTED) {

          Log.i(TAG, "Permission has been denied by user");
        } else {
          Log.i(TAG, "Permission has been granted by user");
        }
        return;
      }

      case MicrophoneHelper.REQUEST_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
        }
      }
    }
    // if (!permissionToRecordAccepted ) finish();

  }

  protected void makeRequest() {
    ActivityCompat.requestPermissions(this,
      new String[]{Manifest.permission.RECORD_AUDIO},
      MicrophoneHelper.REQUEST_PERMISSION);
  }

  // Sending a message to Watson Assistant Service
  private void sendMessage() {

    final String inputmessage = this.inputMessage.getText().toString().trim();
    if (!this.initialRequest) {
      Message inputMessage = new Message();
      inputMessage.setMessage(inputmessage);
      inputMessage.setId("1");
      messageArrayList.add(inputMessage);
    } else {
      Message inputMessage = new Message();
      inputMessage.setMessage(inputmessage);
      inputMessage.setId("100");
      this.initialRequest = false;
      Toast.makeText(getApplicationContext(), "Tap on the message for Voice", Toast.LENGTH_LONG).show();

    }

    this.inputMessage.setText("");
    mAdapter.notifyDataSetChanged();

    Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          if (watsonSettings.watsonAssistantSession == null) {
            ServiceCall<SessionResponse> call = watsonSettings.watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.assistant_id)).build());
            watsonSettings.watsonAssistantSession = call.execute().getResult();
          }

          MessageInput input = new MessageInput.Builder()
            .text(inputmessage)
            .build();
          MessageOptions options = new MessageOptions.Builder()
            .assistantId(mContext.getString(R.string.assistant_id))
            .input(input)
            .sessionId(watsonSettings.watsonAssistantSession.getSessionId())
            .build();
          MessageResponse response = watsonSettings.watsonAssistant.message(options).execute().getResult();
            Log.i(TAG, "run: "+response);
//            Log.i(TAG,response.getOutput().getIntents().get(0).getIntent());
//            if(response.getOutput().getIntents().get(0).getIntent().equals("savedocument")){
//              startActivity(new Intent(mContext,Favourites.class));
//            }
            //new HttpGetRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"http://192.168.43.127:8080/get?param=Rakshit");


          final Message outMessage = new Message();
          if (response != null &&
            response.getOutput() != null &&
            !response.getOutput().getGeneric().isEmpty() &&
            "text".equals(response.getOutput().getGeneric().get(0).getResponseType())) {
            outMessage.setMessage(response.getOutput().getGeneric().get(0).getText());
            outMessage.setId("2");

            messageArrayList.add(outMessage);

            // speak the message
//            new SayTask().execute(outMessage.getMessage());

            runOnUiThread(new Runnable() {
              public void run() {
                mAdapter.notifyDataSetChanged();
                if (mAdapter.getItemCount() > 1) {
                  recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);

                }

              }
            });
          }
          //recordMessage();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();

  }

//  private class SayTask extends AsyncTask<String, Void, String> {
//    @Override
//    protected String doInBackground(String... params) {
//
//      streamPlayer.playStream(textToSpeech.synthesize(new SynthesizeOptions.Builder()
//        .text(params[0])
//        .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
//        .accept(SynthesizeOptions.Accept.AUDIO_WAV)
//        .build()).execute().getResult());
//      return "Did synthesize";
//    }
//  }

  //Record a message via Watson Speech to Text
  private void recordMessage() {
    if (listening != true) {

      capture = microphoneHelper.getInputStream(true);
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            watsonSettings.speechService.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());
          } catch (Exception e) {
            showError(e);
          }
        }
      }).start();
      listening = true;
      Toast.makeText(MainActivity.this, "Listening....Click to Stop", Toast.LENGTH_LONG).show();

    } else {
      try {
        microphoneHelper.closeInputStream();
        listening = false;
        Toast.makeText(MainActivity.this, "Stopped Listening....Click to Start", Toast.LENGTH_LONG).show();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  /**
   * Check Internet Connection
   *
   * @return
   */
  private boolean checkInternetConnection() {
    // get Connectivity Manager object to check connection
    ConnectivityManager cm =
      (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null &&
      activeNetwork.isConnectedOrConnecting();

    // Check for network connections
    if (isConnected) {
      return true;
    } else {
      Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
      return false;
    }

  }

  //Private Methods - Speech to Text
  private RecognizeOptions getRecognizeOptions(InputStream audio) {
    return new RecognizeOptions.Builder()
      .audio(audio)
      .contentType(ContentType.OPUS.toString())
      .model("en-US_BroadbandModel")
      .interimResults(true)
      .inactivityTimeout(2000)
      .build();
  }

  //Watson Speech to Text Methods.
  private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {

    @Override
    public void onTranscription(SpeechRecognitionResults speechResults) {
      if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
        String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
        showMicText(text);
      }
    }

    @Override
    public void onError(Exception e) {
      showError(e);
      enableMicButton();
    }

    @Override
    public void onDisconnected() {
      enableMicButton();
    }

  }

  private void showMicText(final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.i(TAG,"dsdsad"+(text.trim()==temp.trim()));
        Log.i(TAG,text);
        Log.i(TAG,temp);
        if(text.trim().equals(temp.trim())){
//          temp = text;
//          temp = text.replaceAll("send","");

//
          try {
            TimeUnit.SECONDS.sleep(3);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          microphoneHelper.closeInputStream();
          listening = false;
          sendMessage();
          return;
        }
        temp = text;
        inputMessage.setText(temp);


      }
    });
  }

  private void enableMicButton() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        btnRecord.setEnabled(true);
      }
    });
  }

  private void showError(final Exception e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        e.printStackTrace();
      }
    });
  }


  private class HttpGetRequest extends AsyncTask<String, Void, String> {
    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;
    @Override
    protected String doInBackground(String... params){
      String stringUrl = params[0];

      String inputLine;
      try {
        //Create a URL object holding our url
        URL myUrl = new URL(stringUrl);
        //Create a connection
        HttpURLConnection connection =(HttpURLConnection)
                myUrl.openConnection();
        //Set methods and timeouts
        connection.setRequestMethod(REQUEST_METHOD);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(CONNECTION_TIMEOUT);

        //Connect to our url
        connection.connect();
        //Create a new InputStreamReader
        InputStreamReader streamReader = new
                InputStreamReader(connection.getInputStream());
        //Create a new buffered reader and String Builder
        BufferedReader reader = new BufferedReader(streamReader);
        StringBuilder stringBuilder = new StringBuilder();
        //Check if the line we are reading is not null
        while((inputLine = reader.readLine()) != null){
          stringBuilder.append(inputLine);
        }
        //Close our InputStream and Buffered reader
        reader.close();
        streamReader.close();
        //Set our result equal to our stringBuilder
        result = stringBuilder.toString();


      }
      catch(IOException e){
        e.printStackTrace();
        result = null;
      }
      Log.i(TAG,"g"+result);
      return result;
    }
    protected void onPostExecute(String result){

      super.onPostExecute(result);
    }
  }


}



