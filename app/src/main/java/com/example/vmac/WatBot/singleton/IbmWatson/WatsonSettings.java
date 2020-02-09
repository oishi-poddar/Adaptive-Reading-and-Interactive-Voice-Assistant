package com.example.vmac.WatBot.singleton.IbmWatson;

import android.content.Context;

import com.example.vmac.WatBot.R;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;

public class WatsonSettings {
    private static WatsonSettings INSTANCE = null;
    private WatsonSettings() {};
    public Assistant watsonAssistant;
    public SessionResponse watsonAssistantSession;
    public SpeechToText speechService;
    public TextToSpeech textToSpeech;
    public static WatsonSettings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WatsonSettings();

        }
        return(INSTANCE);
    }

    public void setup(Context mContext){
        watsonAssistant = new Assistant("2018-11-08", new IamOptions.Builder()
                .apiKey(mContext.getString(R.string.assistant_apikey))
                .build());
        watsonAssistant.setEndPoint(mContext.getString(R.string.assistant_url));
        textToSpeech = new TextToSpeech();
        textToSpeech.setIamCredentials(new IamOptions.Builder()
                .apiKey(mContext.getString(R.string.TTS_apikey))
                .build());
        textToSpeech.setEndPoint(mContext.getString(R.string.TTS_url));

        speechService = new SpeechToText();
        speechService.setIamCredentials(new IamOptions.Builder()
                .apiKey(mContext.getString(R.string.STT_apikey))
                .build());
        speechService.setEndPoint(mContext.getString(R.string.STT_url));
    }


}
