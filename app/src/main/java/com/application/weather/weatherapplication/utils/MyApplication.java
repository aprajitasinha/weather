package com.application.weather.weatherapplication.utils;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.application.weather.weatherapplication.BuildConfig;
import com.application.weather.weatherapplication.R;
import com.application.weather.weatherapplication.model.db.MyObjectBox;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

public class MyApplication extends Application {
  private static BoxStore boxStore;

  public static BoxStore getBoxStore() {
    return boxStore;
  }

  @Override
  public void onCreate() {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    super.onCreate();
    ViewPump.init(ViewPump.builder()
        .addInterceptor(new CalligraphyInterceptor(
            new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Vazir.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()))
        .build());
    createBoxStore();
  }

  private void createBoxStore() {
    boxStore = MyObjectBox.builder().androidContext(MyApplication.this).build();
    if (BuildConfig.DEBUG) {
      new AndroidObjectBrowser(boxStore).start(this);
    }
  }


}
