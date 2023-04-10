package com.application.weather.weatherapplication.utils;

import com.application.weather.weatherapplication.model.db.CurrentWeather;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class DbUtil {


  public static Query<CurrentWeather> getCurrentWeatherQuery(Box<CurrentWeather> currentWeatherBox) {
    return currentWeatherBox.query().build();
  }


}
