package com.application.weather.weatherapplication.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.RequiresPermission;
import androidx.core.os.ConfigurationCompat;
import com.application.weather.weatherapplication.R;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;

public class AppUtil {
  public static int getWeatherAnimation(int weatherCode) {
    if (weatherCode / 100 == 2) {
      return R.raw.storm_weather;
    } else if (weatherCode / 100 == 3) {
      return R.raw.rainy_weather;
    } else if (weatherCode / 100 == 5) {
      return R.raw.rainy_weather;
    } else if (weatherCode / 100 == 6) {
      return R.raw.snow_weather;
    } else if (weatherCode / 100 == 7) {
      return R.raw.unknown;
    } else if (weatherCode == 800) {
      return R.raw.clear_day;
    } else if (weatherCode == 801) {
      return R.raw.few_clouds;
    } else if (weatherCode == 803) {
      return R.raw.broken_clouds;
    } else if (weatherCode / 100 == 8) {
      return R.raw.cloudy_weather;
    }
    return R.raw.unknown;
  }


  public static String getWeatherStatus(int weatherCode, boolean isRTL) {
    if (weatherCode / 100 == 2) {
        return Constants.WEATHER_STATUS[0];
    } else if (weatherCode / 100 == 3) {
        return Constants.WEATHER_STATUS[1];
    } else if (weatherCode / 100 == 5) {

        return Constants.WEATHER_STATUS[2];

    } else if (weatherCode / 100 == 6) {
        return Constants.WEATHER_STATUS[3];

    } else if (weatherCode / 100 == 7) {
        return Constants.WEATHER_STATUS[4];

    } else if (weatherCode == 800) {
        return Constants.WEATHER_STATUS[5];

    } else if (weatherCode == 801) {
        return Constants.WEATHER_STATUS[6];

    } else if (weatherCode == 803) {
        return Constants.WEATHER_STATUS[7];

    } else if (weatherCode / 100 == 8) {
        return Constants.WEATHER_STATUS[8];

    }
      return Constants.WEATHER_STATUS[4];

  }

  /**
   * If thirty minutes is pass from parameter return true otherwise return false
   *
   * @param lastStored timestamp
   * @return boolean value
   */
  public static boolean isTimePass(long lastStored) {
    return System.currentTimeMillis() - lastStored > Constants.TIME_TO_PASS;
  }






  static boolean isAtLeastVersion(int version) {
    return Build.VERSION.SDK_INT >= version;
  }


  public static boolean isRTL(Context context) {
    Locale locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
    final int directionality = Character.getDirectionality(locale.getDisplayName().charAt(0));
    return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
        directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
  }

  @SuppressLint("StaticFieldLeak")
  private static Application sApplication;


  private static void init(final Application app) {
    if (sApplication == null) {
      if (app == null) {
        sApplication = getApplicationByReflect();
      } else {
        sApplication = app;
      }
    } else {
      if (app != null && app.getClass() != sApplication.getClass()) {
        sApplication = app;
      }
    }
  }

  public static Application getApp() {
    if (sApplication != null) return sApplication;
    Application app = getApplicationByReflect();
    init(app);
    return app;
  }

  private static Application getApplicationByReflect() {
    try {
      @SuppressLint("PrivateApi")
      Class<?> activityThread = Class.forName("android.app.ActivityThread");
      Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
      Object app = activityThread.getMethod("getApplication").invoke(thread);
      if (app == null) {
        throw new NullPointerException("u should init first");
      }
      return (Application) app;
    } catch (NoSuchMethodException | IllegalAccessException |
        InvocationTargetException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    throw new NullPointerException("u should init first");
  }


  @RequiresPermission(ACCESS_NETWORK_STATE)
  public static boolean isNetworkConnected() {
    NetworkInfo info = getActiveNetworkInfo();
    return info != null && info.isConnected();
  }


  @RequiresPermission(ACCESS_NETWORK_STATE)
  private static NetworkInfo getActiveNetworkInfo() {
    ConnectivityManager cm =
        (ConnectivityManager) getApp().getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) return null;
    return cm.getActiveNetworkInfo();
  }



}
