package com.example.sunshine.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.sunshine.MainActivity;
import com.example.sunshine.data.WeatherContract;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

public class SunshineSyncUtils {

    /*
     * Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
     * writing out a bunch of multiplication ourselves and risk making a silly mistake.
     */
    private static final int SYNC_INTERVAL_HOURS = 3;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;
    private static final String SUNSHINE_SYNC_TAG = "sunshine-sync";
    private static boolean sInitialized;

    /**
     * Schedules a repeating sync of Sunshine's weather data using FirebaseJobDispatcher.
     *
     * @param context Context used to create the GooglePlayDriver that powers the
     *                FirebaseJobDispatcher
     */
    static void scheduleFirebaseJobDispatcherSync(@NonNull final Context context) {

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        /* Create the Job to periodically sync Sunshine */
        Job syncSunshineJob = dispatcher.newJobBuilder()
                /* The Service that will be used to sync Sunshine's data */
                .setService(SunshineFirebaseJobService.class)
                /* Set the UNIQUE tag used to identify this Job */
                .setTag(SUNSHINE_SYNC_TAG)
                /*
                 * Network constraints on which this Job should run. We choose to run on any
                 * network, but you can also choose to run only on un-metered networks or when the
                 * device is charging. It might be a good idea to include a preference for this,
                 * as some users may not want to download any data on their mobile plan. ($$$)
                 */
                .setConstraints(Constraint.ON_ANY_NETWORK)
                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.FOREVER)
                /*
                 * We want Sunshine's weather data to stay up to date, so we tell this Job to recur.
                 */
                .setRecurring(true)
                /*
                 * We want the weather data to be synced every 3 to 4 hours. The first argument for
                 * Trigger's static executionWindow method is the start of the time frame when the
                 * sync should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(syncSunshineJob);
    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     *
     * @param context Context that will be passed to other methods and used to access the
     *                ContentResolver
     */
    synchronized public static void initialize(@NonNull final Context context) {

        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized) return;

        sInitialized = true;


        scheduleFirebaseJobDispatcherSync(context);

        Thread checkForEmpty = new Thread(new Runnable() {
            @Override
            public void run() {

                /* URI for every row of weather data in our weather table*/
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;


                String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
                String selectionStatement = WeatherContract.WeatherEntry
                        .getSqlSelectForTodayOnwards();

                /* Here, we perform the query to check to see if we have any weather data */
                Cursor cursor = context.getContentResolver().query(
                        forecastQueryUri,
                        projectionColumns,
                        selectionStatement,
                        null,
                        null);

                if (null == cursor || cursor.getCount() == 0) {
                    MainActivity.startImmediateSync(context);
                }

                /* Make sure to close the Cursor to avoid memory leaks! */
                cursor.close();
            }
        });

        /* Finally, once the thread is prepared, fire it off to perform our checks. */
        checkForEmpty.start();
    }

}