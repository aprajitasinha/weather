package com.application.weather.weatherapplication.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.application.weather.weatherapplication.utils.MyApplication;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }


  @Override
  public void applyOverrideConfiguration(@Nullable Configuration overrideConfiguration) {
    if (overrideConfiguration != null) {
      int uiMode = overrideConfiguration.uiMode;
      overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
      overrideConfiguration.uiMode = uiMode;
    }
    super.applyOverrideConfiguration(getResources().getConfiguration());
  }
}