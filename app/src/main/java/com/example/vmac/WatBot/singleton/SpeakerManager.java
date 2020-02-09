package com.example.vmac.WatBot.singleton;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.vmac.WatBot.singleton.IbmWatson.WatsonSettings;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class SpeakerManager {
    private AudioManager audioManager;

    private static SpeakerManager INSTANCE = null;
    private  TextToSpeech speaker;
    public static SpeakerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpeakerManager();

        }
        return(INSTANCE);
    }
    public void setup(Context mcontext) {
        if(speaker == null) {
            audioManager = (AudioManager) mcontext.getSystemService(Context.AUDIO_SERVICE);
            speaker = new TextToSpeech(mcontext, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS)
                        speaker.setLanguage(Locale.getDefault());
                }
            });
        }
    }

    public void speak(String toBeSpoken) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            speaker.speak(toBeSpoken, TextToSpeech.QUEUE_FLUSH, null, null);
        else
            speaker.speak(toBeSpoken.toString(), TextToSpeech.QUEUE_FLUSH, null);

    }
    public void pause() {
        speaker.stop();

        speaker.shutdown();
    }
    public boolean isSpeaking(){
        return speaker.isSpeaking();
    }

    public void increaseVolume(){
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,curVolume+5
                , 0);
    }
    public void decreaseVolume(){
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,curVolume-5
                , 0);
    }
}