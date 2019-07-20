package com.example.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sunshine.data.SunshinePreferences;
import com.example.sunshine.data.WeatherContract;
import com.example.sunshine.databinding.ActivityForecastBinding;
import com.example.sunshine.sync.SunshineSyncIntentService;
import com.example.sunshine.sync.SunshineSyncUtils;
import com.example.sunshine.utilities.NetworkUtils;
import com.example.sunshine.utilities.Utility;

import static com.example.sunshine.utilities.Utility.isGPSEnabled;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        ForecastAdapter.ForecastAdapterOnClickHandler, GPSTracker.OnOptionsCancelSelected {

    public static final String TAG = MainActivity.class.getSimpleName();
    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_CONDITION_ID = 3;
    public static boolean isDatabaseEmpty = false;
    private ForecastAdapter mForecastAdapter;
    private ActivityForecastBinding binding;
    private ProgressBar mLoadingIndicator;
    private TextView mNetworkUnavailable;
    private RecyclerView mRecyclerView;
    private long backPressedTime;
    private long RotateTime = -1;
    private Toast backToast;
    private GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forecast);

        getSupportActionBar().setElevation(4);

        Utility.checkAllStateAndSettings(this);

        configUIAndRecycler();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Utility.CHECK_STATE_DATA_BASE_KEY)) {
                isDatabaseEmpty = savedInstanceState.getBoolean(Utility.CHECK_STATE_DATA_BASE_KEY);
            }
            if (savedInstanceState.containsKey(Utility.CHECK_STATE_ROTATE_SCREEN_KEY)) {
                RotateTime = savedInstanceState.getLong(Utility.CHECK_STATE_ROTATE_SCREEN_KEY);
            }
        } else
            RotateTime = System.currentTimeMillis();
        loadWeatherFeed();

        /*
         * Ensures a loader is initialized and active. If the loader doesn't already exist, one is
         * created and (if the activity/fragment is currently started) starts the loader. Otherwise
         * the last created loader is re-used.
         */
        //  LoaderManager.getInstance().initLoader(ID_FORECAST_LOADER, null, this);
        //LoaderManager.getInstance(this).initLoader(Utility.ID_FORECAST_LOADER, null, this);
        LoaderManager.getInstance(this).initLoader(Utility.ID_FORECAST_LOADER, null, this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Utility.CHECK_STATE_DATA_BASE_KEY, isDatabaseEmpty);
        outState.putLong(Utility.CHECK_STATE_ROTATE_SCREEN_KEY, System.currentTimeMillis());
    }

    private void configUIAndRecycler() {

        mRecyclerView = binding.recyclerviewForecast;

        mLoadingIndicator = binding.pbLoadingIndicator;

        mNetworkUnavailable = binding.tvNetworkUnavailable;

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this, this);

        mRecyclerView.setAdapter(mForecastAdapter);
    }

    /**
     * Called by the {@link android.support.v4.app.LoaderManagerImpl} when a new Loader needs to be
     * created. This Activity only uses one loader, so we don't necessarily NEED to check the
     * loaderId, but this is certainly best practice.
     *
     * @param loaderId The loader ID for which we need to create a loader
     * @param bundle   Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {


        switch (loaderId) {

            case Utility.ID_FORECAST_LOADER:
                /* URI for all rows of weather data in our weather table */
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

                String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

                return new CursorLoader(this,
                        forecastQueryUri,
                        Utility.MAIN_FORECAST_PROJECTION,
                        selection,
                        null,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    /**
     * Called when a Loader has finished loading its data.
     * <p>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if (Utility.mPosition == RecyclerView.NO_POSITION) Utility.mPosition = 0;
        mRecyclerView.smoothScrollToPosition(Utility.mPosition);
        if (data.getCount() != 0) {
            isDatabaseEmpty = false;
            showWeatherDataView();
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */
        mForecastAdapter.swapCursor(null);
    }

    private void loadWeatherFeed() {

        if (Utility.shouldAskPermissions()) {
            if (Utility.checkStateOfLocationPermission(this))
                Utility.askPermissions(this);
            else
                WeatherFeed();
        } else
            WeatherFeed();
    }

    private void WeatherFeed() {
        if (RotateTime + 500 < System.currentTimeMillis() || RotateTime == -1) {
            if (NetworkUtils.getNetworkAvailability(this)) {
                if (isGPSEnabled(this)) {
                    //if (!mLoadingIndicator.isShown())
                        showLoadingProgressPar();
                    makeGPSTrackerAndSetLocation(this);
                    if (isDatabaseEmpty) {
                        SunshineSyncUtils.initialize(this);
                    } else {
                        Utility.EnforceToRefreshData(this);
                    }
                } else {
                    GPSTracker.showSettingsAlert(this);
                }
            } else {
                FindOutWhenNoNetOrGps(Utility.STATE_CHANGE_FOR_INTERNET_UNAVAILABLE);
            }
        }
    }

    public void makeGPSTrackerAndSetLocation(Context context) {
        gps = new GPSTracker(context);
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            //Toast.makeText(context, "Your location is \n " + "Latitude : " + String.valueOf(latitude) + "\n Longitude : " + String.valueOf(longitude), Toast.LENGTH_SHORT).show();
            String cityName = gps.getRegionName(latitude, longitude);
            if (!cityName.isEmpty()) {
                // Toast.makeText(context, cityName, Toast.LENGTH_SHORT).show();
                SunshinePreferences.resetLocationCityName(context);
                SunshinePreferences.setLocationCityName(context, cityName);
            }
            SunshinePreferences.resetLocationCoordinates(context);
            SunshinePreferences.setLocationDetails(context, latitude, longitude);
        } else {
            Toast.makeText(context, "Couldn't get Location. Try it again", Toast.LENGTH_SHORT).show();
        }
    }

    private void FindOutWhenNoNetOrGps(int State) {
        String message = "";
        if (State == Utility.STATE_CHANGE_FOR_GPS_UNAVAILABLE)
            message = "GPS unavailable";
        else if (State == Utility.STATE_CHANGE_FOR_INTERNET_UNAVAILABLE)
            message = "Network unavailable";
        if (!isDatabaseEmpty) {
            Snackbar snackbar = Snackbar
                    .make(mRecyclerView, message, Snackbar.LENGTH_LONG);
            ViewGroup group = (ViewGroup) snackbar.getView();
            group.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
            snackbar.show();
            showWeatherDataView();
        } else {
            HideAllNoWayToConnect(message);
        }
    }

    private void showLoadingProgressPar() {

        mRecyclerView.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mNetworkUnavailable.setVisibility(View.INVISIBLE);
    }

    private void showWeatherDataView() {

        mRecyclerView.setVisibility(View.VISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mNetworkUnavailable.setVisibility(View.INVISIBLE);
    }

    private void HideAllNoWayToConnect(String message) {

        mRecyclerView.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mNetworkUnavailable.setText(message);
        mNetworkUnavailable.setVisibility(View.VISIBLE);
    }

    /**
     * This method is for responding to clicks from our list.
     *
     * @param date Normalized UTC time that represents the local date of the weather in GMT time.
     * @see WeatherContract.WeatherEntry#COLUMN_DATE
     */
    @Override
    public void onClick(long date) {
        Intent weatherDetailIntent = new Intent(MainActivity.this, DetailActivity.class);
        Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(date);
        weatherDetailIntent.setData(uriForDateClicked);
        startActivity(weatherDetailIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Utility.LOCATION_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherFeed();
            } else {
                HideAllNoWayToConnect("Location's permission denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Utility.REQUEST_CODE_FOR_ENABLE_GPS) {
            if (isGPSEnabled(this)) {
                // All required changes were successfully made
                showLoadingProgressPar();
                loadWeatherFeed();
            } else {
                // The user was asked to change settings, but chose not to
                FindOutWhenNoNetOrGps(Utility.STATE_CHANGE_FOR_GPS_UNAVAILABLE);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_map) {
            if (Utility.isGPSEnabled(this))
                Utility.openPreferredLocationInMap(this);
            else
                Toast.makeText(this, "GPS unavailable", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_refresh) {
            loadWeatherFeed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            finish();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }

        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        if (gps != null)
            gps.stopUsingGPS();
        super.onDestroy();
    }

    @Override
    public void OnClickPressed(int state) {
        FindOutWhenNoNetOrGps(Utility.STATE_CHANGE_FOR_GPS_UNAVAILABLE);
    }

    /**
     * Helper method to perform a sync immediately using an IntentService for asynchronous
     * execution.
     *
     * @param context The Context used to start the IntentService for the sync.
     */
    public static void startImmediateSync(@NonNull final Context context) {

        Intent intentToSyncImmediately = new Intent(context, SunshineSyncIntentService.class);
        context.startService(intentToSyncImmediately);
    }

}
