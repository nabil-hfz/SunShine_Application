package com.example.sunshine.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.example.sunshine.GPSTracker;
import com.example.sunshine.data.SunshinePreferences;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.RetryStrategy;


public class SunshineFirebaseJobService extends JobService {

    private AsyncTask<Void, Void, Void> mFetchWeatherTask;

    /**
     * The entry point to your Job. Implementations should offload work to another thread of
     * execution as soon as possible.
     * <p>
     * This is called by the Job Dispatcher to tell us we should start our job. Keep in mind this
     * method is run on the application's main thread, so we need to offload work to a background
     * thread.
     *
     * @return whether there is more work remaining.
     */
    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        mFetchWeatherTask = new AsyncTask<Void, Void, Void>() {
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                Context context = getApplicationContext();
                GPSTracker gps = new GPSTracker(context);
                if (gps.canGetLocation()) {
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();
                    //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    SunshinePreferences.resetLocationCoordinates(context);
                    SunshinePreferences.setLocationDetails(context, latitude, longitude);
                }
                gps.stopUsingGPS();

                SunshineSyncTask.syncWeather(context);
                jobFinished(jobParameters, false);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(jobParameters, false);
            }
        };

        mFetchWeatherTask.execute();
        return true;
    }

    /**
     * Called when the scheduling engine has decided to interrupt the execution of a running job,
     * most likely because the runtime constraints associated with the job are no longer satisfied.
     *
     * @return whether the job should be retried
     * @see Job.Builder#setRetryStrategy(RetryStrategy)
     * @see RetryStrategy
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mFetchWeatherTask != null) {
            mFetchWeatherTask.cancel(true);
        }
        return true;
    }
}