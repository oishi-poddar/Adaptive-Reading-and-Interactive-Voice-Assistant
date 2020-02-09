package com.example.vmac.WatBot;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import com.example.vmac.WatBot.authentication.Login;
import com.example.vmac.WatBot.authentication.Register;
//import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = new Intent(this, Login.class);
    startActivity(intent);
    finish();
  }
}
