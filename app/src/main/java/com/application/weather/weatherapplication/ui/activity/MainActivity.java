package com.application.weather.weatherapplication.ui.activity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.application.weather.weatherapplication.R;
import com.application.weather.weatherapplication.databinding.ActivityMainBinding;
import com.application.weather.weatherapplication.model.CityInfo;
import com.application.weather.weatherapplication.model.currentweather.CurrentWeatherResponse;
import com.application.weather.weatherapplication.model.db.CurrentWeather;
import com.application.weather.weatherapplication.service.ApiService;
import com.application.weather.weatherapplication.utils.ApiClient;
import com.application.weather.weatherapplication.utils.AppUtil;
import com.application.weather.weatherapplication.utils.Constants;
import com.application.weather.weatherapplication.utils.DbUtil;
import com.application.weather.weatherapplication.utils.MyApplication;
import com.application.weather.weatherapplication.utils.SnackbarUtil;
import com.application.weather.weatherapplication.utils.TextViewFactory;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import java.util.List;
import java.util.Locale;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscriptionList;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class MainActivity extends BaseActivity {

  private CompositeDisposable disposable = new CompositeDisposable();
  private String defaultLang = "en";
  private ApiService apiService;
  private Prefser prefser;
  private Box<CurrentWeather> currentWeatherBox;
  private DataSubscriptionList subscriptions = new DataSubscriptionList();
  private boolean isLoad = false;
  private CityInfo cityInfo;
  private String apiKey;
  private Typeface typeface;
  private ActivityMainBinding binding;
  private int[] colors;
  private int[] colorsAlpha;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setSupportActionBar(binding.toolbarLayout.toolbar);
    initSearchView();
    initValues();
    setupTextSwitchers();
    showStoredCurrentWeather();
    checkLastUpdate();
  }

  private void initSearchView() {
    binding.toolbarLayout.searchView.setVoiceSearch(false);
    binding.toolbarLayout.searchView.setHint(getString(R.string.search_label));
    binding.toolbarLayout.searchView.setCursorDrawable(R.drawable.custom_curosr);
    binding.toolbarLayout.searchView.setEllipsize(true);
    binding.toolbarLayout.searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        requestWeather(query, true);
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });
    binding.toolbarLayout.searchView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        binding.toolbarLayout.searchView.showSearch();
      }
    });

  }

  private void initValues() {
    colors = getResources().getIntArray(R.array.mdcolor_500);
    colorsAlpha = getResources().getIntArray(R.array.mdcolor_500_alpha);
    prefser = new Prefser(this);
    apiService = ApiClient.getClient().create(ApiService.class);
    BoxStore boxStore = MyApplication.getBoxStore();
    currentWeatherBox = boxStore.boxFor(CurrentWeather.class);
    binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
        android.R.color.holo_green_light,
        android.R.color.holo_orange_light,
        android.R.color.holo_red_light);
    binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

      @Override
      public void onRefresh() {
        cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
        if (cityInfo != null) {
          long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
          if (AppUtil.isTimePass(lastStored)) {
            requestWeather(cityInfo.getName(), false);
          } else {
            binding.swipeContainer.setRefreshing(false);
          }
        } else {
          binding.swipeContainer.setRefreshing(false);
        }
      }

    });

    typeface = Typeface.createFromAsset(getAssets(), "fonts/Vazir.ttf");

  }

  private void setupTextSwitchers() {
    binding.contentMainLayout.tempTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.TempTextView, true, typeface));
    binding.contentMainLayout.tempTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
    binding.contentMainLayout.tempTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
    binding.contentMainLayout.descriptionTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.DescriptionTextView, true, typeface));
    binding.contentMainLayout.descriptionTextView.setInAnimation(MainActivity.this, R.anim.slide_in_right);
    binding.contentMainLayout.descriptionTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_left);
    binding.contentMainLayout.humidityTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.HumidityTextView, false, typeface));
    binding.contentMainLayout.humidityTextView.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
    binding.contentMainLayout.humidityTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
    binding.contentMainLayout.windTextView.setFactory(new TextViewFactory(MainActivity.this, R.style.WindSpeedTextView, false, typeface));
    binding.contentMainLayout.windTextView.setInAnimation(MainActivity.this, R.anim.slide_in_bottom);
    binding.contentMainLayout.windTextView.setOutAnimation(MainActivity.this, R.anim.slide_out_top);
  }


  private void showStoredCurrentWeather() {
    Query<CurrentWeather> query = DbUtil.getCurrentWeatherQuery(currentWeatherBox);
    query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
        .observer(new DataObserver<List<CurrentWeather>>() {
          @Override
          public void onData(@NonNull List<CurrentWeather> data) {
            if (data.size() > 0) {
              hideEmptyLayout();
              CurrentWeather currentWeather = data.get(0);
              if (isLoad) {
                binding.contentMainLayout.tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                binding.contentMainLayout.descriptionTextView.setText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                binding.contentMainLayout.humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                binding.contentMainLayout.windTextView.setText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
              } else {
                binding.contentMainLayout.tempTextView.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                binding.contentMainLayout.descriptionTextView.setCurrentText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(MainActivity.this)));
                binding.contentMainLayout.humidityTextView.setCurrentText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                binding.contentMainLayout.windTextView.setCurrentText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
              }
              binding.contentMainLayout.animationView.setAnimation(AppUtil.getWeatherAnimation(currentWeather.getWeatherId()));
              binding.contentMainLayout.animationView.playAnimation();
            }
          }
        });
  }


  private void checkLastUpdate() {
    cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
    if (cityInfo != null) {
      binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
      if (prefser.contains(Constants.LAST_STORED_CURRENT)) {
        long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
        if (AppUtil.isTimePass(lastStored)) {
          requestWeather(cityInfo.getName(), false);
        }
      } else {
        requestWeather(cityInfo.getName(), false);
      }
    } else {
      showEmptyLayout();
    }

  }


  private void requestWeather(String cityName, boolean isSearch) {
    if (AppUtil.isNetworkConnected()) {
      getCurrentWeather(cityName, isSearch);
    } else {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.no_internet_message))
          .setDuration(SnackbarUtil.LENGTH_LONG)
          .showError();
      binding.swipeContainer.setRefreshing(false);
    }
  }

  private void getCurrentWeather(String cityName, boolean isSearch) {
    apiKey = getResources().getString(R.string.open_weather_map_api);
    disposable.add(
        apiService.getCurrentWeather(
            cityName, Constants.UNITS, defaultLang, apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableSingleObserver<CurrentWeatherResponse>() {
              @Override
              public void onSuccess(CurrentWeatherResponse currentWeatherResponse) {
                isLoad = true;
                storeCurrentWeather(currentWeatherResponse);
                storeCityInfo(currentWeatherResponse);
                binding.swipeContainer.setRefreshing(false);
                if (isSearch) {
                  prefser.remove(Constants.LAST_STORED_MULTIPLE_DAYS);
                }
              }

              @Override
              public void onError(Throwable e) {
                binding.swipeContainer.setRefreshing(false);
                try {
                  HttpException error = (HttpException) e;
                  handleErrorCode(error);
                } catch (Exception exception) {
                  e.printStackTrace();
                }
              }
            })

    );
  }

  private void handleErrorCode(HttpException error) {
    if (error.code() == 404) {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.no_city_found_message))
          .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
          .setAction(getResources().getString(R.string.search_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              binding.toolbarLayout.searchView.showSearch();
            }
          })
          .showWarning();

    } else if (error.code() == 401) {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.invalid_api_key_message))
          .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
          .setAction(getString(R.string.ok_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
          })
          .showError();

    } else {
      SnackbarUtil
          .with(binding.swipeContainer)
          .setMessage(getString(R.string.network_exception_message))
          .setDuration(SnackbarUtil.LENGTH_LONG)
          .setAction(getResources().getString(R.string.retry_label), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (cityInfo != null) {
                requestWeather(cityInfo.getName(), false);
              } else {
                binding.toolbarLayout.searchView.showSearch();
              }
            }
          })
          .showWarning();
    }
  }

  private void showEmptyLayout() {
    Glide.with(MainActivity.this).load(R.drawable.no_city).into(binding.contentEmptyLayout.noCityImageView);
    binding.contentEmptyLayout.emptyLayout.setVisibility(View.VISIBLE);
    binding.contentMainLayout.nestedScrollView.setVisibility(View.GONE);
  }

  private void hideEmptyLayout() {
    binding.contentEmptyLayout.emptyLayout.setVisibility(View.GONE);
    binding.contentMainLayout.nestedScrollView.setVisibility(View.VISIBLE);
  }


  private void storeCurrentWeather(CurrentWeatherResponse response) {
    CurrentWeather currentWeather = new CurrentWeather();
    currentWeather.setTemp(response.getMain().getTemp());
    currentWeather.setHumidity(response.getMain().getHumidity());
    currentWeather.setDescription(response.getWeather().get(0).getDescription());
    currentWeather.setMain(response.getWeather().get(0).getMain());
    currentWeather.setWeatherId(response.getWeather().get(0).getId());
    currentWeather.setWindDeg(response.getWind().getDeg());
    currentWeather.setWindSpeed(response.getWind().getSpeed());
    currentWeather.setStoreTimestamp(System.currentTimeMillis());
    prefser.put(Constants.LAST_STORED_CURRENT, System.currentTimeMillis());
    if (!currentWeatherBox.isEmpty()) {
      currentWeatherBox.removeAll();
      currentWeatherBox.put(currentWeather);
    } else {
      currentWeatherBox.put(currentWeather);
    }
  }

  private void storeCityInfo(CurrentWeatherResponse response) {
    CityInfo cityInfo = new CityInfo();
    cityInfo.setCountry(response.getSys().getCountry());
    cityInfo.setId(response.getId());
    cityInfo.setName(response.getName());
    prefser.put(Constants.CITY_INFO, cityInfo);
    binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
  }






  @Override
  protected void onDestroy() {
    super.onDestroy();
    disposable.dispose();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    MenuItem item = menu.findItem(R.id.action_search);
    binding.toolbarLayout.searchView.setMenuItem(item);
    return true;
  }


  @Override
  public void onBackPressed() {
    if (binding.toolbarLayout.searchView.isSearchOpen()) {
      binding.toolbarLayout.searchView.closeSearch();
    } else {
      super.onBackPressed();
    }
  }
}
