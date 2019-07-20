package com.example.sunshine.utilities;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.example.sunshine.data.SunshinePreferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * These utilities will be used to communicate with the weather servers.
 */
public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private static final String DYNAMIC_WEATHER_URL =
            "https://andfun-weather.udacity.com/weather";

    private static final String STATIC_WEATHER_URL =
            "https://andfun-weather.udacity.com/staticweather";

    private static final String FORECAST_BASE_URL = DYNAMIC_WEATHER_URL;



    /* The format we want our API to return */
    private static final String format = "json";
    /* The units we want our API to return */
    private static final String units = "metric";
    /* The number of days we want our API to return */
    private static final int numDays = 14;

    /* The query parameter allows us to provide a location string to the API */
    private static final String QUERY_PARAM = "q";

    private static final String LAT_PARAM = "lat";
    private static final String LON_PARAM = "lon";

    /* The format parameter allows us to designate whether we want JSON or XML from our API */
    private static final String FORMAT_PARAM = "mode";
    /* The units parameter allows us to designate whether we want metric units or imperial units */
    private static final String UNITS_PARAM = "units";
    /* The days parameter allows us to designate how many days of weather data we want */
    private static final String DAYS_PARAM = "cnt";

    /**
     * Retrieves the proper URL to query for the weather data. The reason for both this method as
     * well as  is two fold.
     * <p>
     *
     * @param context used to access other Utility methods
     * @return URL to query weather service
     */
    public static URL getUrl(Context context) {
        if (SunshinePreferences.isLocationLatLonAvailable(context)) {
            double[] preferredCoordinates = SunshinePreferences.getLocationCoordinates(context);
            double latitude = preferredCoordinates[0];
            double longitude = preferredCoordinates[1];
            return buildUrlWithLatitudeLongitude(latitude, longitude);
        } else {
            String locationQuery = SunshinePreferences.getPreferredWeatherLocation(context);
            return buildUrlWithLocationQuery(locationQuery);
        }
    }

    /**
     * Builds the URL used to talk to the weather server using latitude and longitude of a
     * location.
     *
     * @param latitude  The latitude of the location
     * @param longitude The longitude of the location
     * @return The Url to use to query the weather server.
     */
    private static URL buildUrlWithLatitudeLongitude(Double latitude, Double longitude) {
        Uri weatherQueryUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(LAT_PARAM, String.valueOf(latitude))
                .appendQueryParameter(LON_PARAM, String.valueOf(longitude))
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                .build();

        try {
            URL weatherQueryUrl = new URL(weatherQueryUri.toString());
            Log.v(TAG, "URL: " + weatherQueryUrl);
            return weatherQueryUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds the URL used to talk to the weather server using a location. This location is based
     * on the query capabilities of the weather provider that we are using.
     *
     * @param locationQuery The location that will be queried for.
     * @return The URL to use to query the weather server.
     */
    private static URL buildUrlWithLocationQuery(String locationQuery) {
        Uri weatherQueryUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, locationQuery)
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                .build();

        try {
            URL weatherQueryUrl = new URL(weatherQueryUri.toString());
            Log.v(TAG, "URL: " + weatherQueryUrl);
            return weatherQueryUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response, null if no response
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            scanner.close();
            return response;
        } finally {
            urlConnection.disconnect();
        }
    }

    public static boolean getNetworkAvailability(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}