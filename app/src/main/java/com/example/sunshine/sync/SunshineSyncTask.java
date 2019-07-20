package com.example.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;

import com.example.sunshine.data.SunshinePreferences;
import com.example.sunshine.data.WeatherContract;
import com.example.sunshine.utilities.NetworkUtils;
import com.example.sunshine.utilities.NotificationUtils;
import com.example.sunshine.utilities.OpenWeatherJsonUtils;

import java.net.URL;

public class SunshineSyncTask {

    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */
    synchronized public static void syncWeather(Context context) {

        try {

            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            /* Use the URL to retrieve the JSON */
            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

            /* Parse the JSON into a list of weather values */
            ContentValues[] weatherValues = OpenWeatherJsonUtils
                    .getWeatherContentValuesFromJson(context, jsonWeatherResponse);


            if (weatherValues != null && weatherValues.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

                /* Delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherValues);


                boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                long timeSinceLastNotification = SunshinePreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean oneDayPassedSinceLastNotification = false;

                if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                    oneDayPassedSinceLastNotification = true;
                }


                if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                    NotificationUtils.notifyUserOfNewWeather(context);
                }

                /* If the code reaches this point, we have successfully performed our sync */

            }

        } catch (Exception e) {
            /* Server probably invalid */
            e.printStackTrace();
        }
    }
}

