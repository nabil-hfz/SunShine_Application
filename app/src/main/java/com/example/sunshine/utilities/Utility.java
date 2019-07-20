package com.example.sunshine.utilities;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.sunshine.MainActivity;
import com.example.sunshine.data.SunshinePreferences;
import com.example.sunshine.data.WeatherContract;
import com.example.sunshine.data.WeatherDbHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import static android.content.Context.LOCATION_SERVICE;
import static com.example.sunshine.data.WeatherProvider.checkEmpty;


public class Utility {


    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
    };


    public static final int LOCATION_PERMISSIONS_REQUEST = 721;
    public static final String CHECK_STATE_DATA_BASE_KEY = "is_data_base_empty";
    public static final String CHECK_STATE_ROTATE_SCREEN_KEY = "is_screen_rotated";

    public static final int ID_FORECAST_LOADER = 44;
    public static final int REQUEST_CODE_FOR_ENABLE_GPS = 911;
    public static final int STATE_CHANGE_FOR_GPS_UNAVAILABLE = 47;
    public static final int STATE_CHANGE_FOR_INTERNET_UNAVAILABLE = 45;
    public static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;
    public static int mPosition = RecyclerView.NO_POSITION;

    public static void checkAllStateAndSettings(Context context) {

        checkGooglePlayServices(context);

        checkTls(context);

        checkStateOfDataBase(context);
    }

    private static boolean checkGooglePlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int checkGooglePlayServices = apiAvailability.isGooglePlayServicesAvailable(context);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
            apiAvailability.getErrorDialog((Activity) context, checkGooglePlayServices,
                    REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
            return false;
        }
        return true;
    }

    private static void checkTls(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                ProviderInstaller.installIfNeeded(context);
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean shouldAskPermissions() {
        Log.i("MainActivity ", "shouldAskPermissions  ");
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    private static void checkStateOfDataBase(Context context) {
        WeatherDbHelper mOpenHelper = new WeatherDbHelper(context);
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        MainActivity.isDatabaseEmpty = checkEmpty(database, WeatherContract.WeatherEntry.TABLE_NAME);
        if (mOpenHelper != null) mOpenHelper.close();
        if (database != null) database.close();
    }


    public static void askPermissions(Context context) {
        boolean Res = checkStateOfLocationPermission(context);
        if (Res) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_REQUEST);
        }
    }

    public static boolean checkStateOfLocationPermission(Context context) {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void EnforceToRefreshData(Context context) {
        MainActivity.startImmediateSync(context);
    }

    /**
     * Uses the URI scheme for showing a location found on a map in conjunction with
     * an implicit Intent. This super-handy Intent is detailed in the "Common Intents" page of
     * Android's developer site:
     *
     * @see "http://developer.android.com/guide/components/intents-common.html#Maps"
     * <p>
     * Protip: Hold Command on Mac or Control on Windows and click that link to automagically
     * open the Common Intents page
     */
    public static void openPreferredLocationInMap(Context context) {
        double[] cords = SunshinePreferences.getLocationCoordinates(context);
        String posLat = Double.toString(cords[0]);
        String posLong = Double.toString(cords[1]);
        //  Toast.makeText(context, "Your location is \n " + "Latitude : " + String.valueOf(posLat) + "\n Longitude : " + String.valueOf(posLong), Toast.LENGTH_SHORT).show();
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Log.d(MainActivity.TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
    }

}
