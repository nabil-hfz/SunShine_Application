package com.example.sunshine.utilities;

import android.content.Context;
import android.text.format.DateUtils;

import com.example.sunshine.R;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Class for handling date conversions that are useful for Sunshine.
 */
public final class SunshineDateUtils {

    /* Milliseconds in a day */
    public static final long DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

    /**
     * @return The number of milliseconds (UTC / GMT) for today's date at midnight in the local
     * time zone
     */
    public static long getNormalizedUtcDateForToday() {


        long utcNowMillis = System.currentTimeMillis();


        TimeZone currentTimeZone = TimeZone.getDefault();


        long gmtOffsetMillis = currentTimeZone.getOffset(utcNowMillis);


        long timeSinceEpochLocalTimeMillis = utcNowMillis + gmtOffsetMillis;

        /* This method simply converts milliseconds to days, disregarding any fractional days */
        long daysSinceEpochLocal = TimeUnit.MILLISECONDS.toDays(timeSinceEpochLocalTimeMillis);


        long normalizedUtcMidnightMillis = TimeUnit.DAYS.toMillis(daysSinceEpochLocal);

        return normalizedUtcMidnightMillis;
    }

    /**
     * This method returns the number of days since the epoch (January 01, 1970, 12:00 Midnight UTC)
     * in UTC time from the current date.
     *
     * @param utcDate A date in milliseconds in UTC time.
     * @return The number of days from the epoch to the date argument.
     */
    private static long elapsedDaysSinceEpoch(long utcDate) {
        return TimeUnit.MILLISECONDS.toDays(utcDate);
    }


    public static long normalizeDate(long date) {
        long daysSinceEpoch = elapsedDaysSinceEpoch(date);
        long millisFromEpochToTodayAtMidnightUtc = daysSinceEpoch * DAY_IN_MILLIS;
        return millisFromEpochToTodayAtMidnightUtc;
    }

    /**
     * In order to ensure consistent inserts into WeatherProvider, we check that dates have been
     * normalized before they are inserted. If they are not normalized, we don't want to accept
     * them, and leave it up to the caller to throw an IllegalArgumentException.
     *
     * @param millisSinceEpoch Milliseconds since January 1, 1970 at midnight
     * @return true if the date represents the beginning of a day in Unix time, false otherwise
     */
    public static boolean isDateNormalized(long millisSinceEpoch) {
        boolean isDateNormalized = false;
        if (millisSinceEpoch % DAY_IN_MILLIS == 0) {
            isDateNormalized = true;
        }

        return isDateNormalized;
    }

    /**
     * This method will return the local time midnight for the provided normalized UTC date.
     *
     * @param normalizedUtcDate UTC time at midnight for a given date. This number comes from the
     *                          database
     * @return The local date corresponding to the given normalized UTC date
     */
    private static long getLocalMidnightFromNormalizedUtcDate(long normalizedUtcDate) {
        /* The timeZone object will provide us the current user's time zone offset */
        TimeZone timeZone = TimeZone.getDefault();
        /*
         * This offset, in milliseconds, when added to a UTC date time, will produce the local
         * time.
         */
        long gmtOffset = timeZone.getOffset(normalizedUtcDate);
        long localMidnightMillis = normalizedUtcDate - gmtOffset;
        return localMidnightMillis;
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users. As classy and polished a user experience as "1474061664" is, we can do better.
     * <p/>
     * The day string for forecast uses the following logic:
     * For today: "Today, June 8"
     * For tomorrow:  "Tomorrow
     * For the next 5 days: "Wednesday" (just the day name)
     * For all days after that: "Mon, Jun 8" (Mon, 8 Jun in UK, for example)
     *
     * @param context               Context to use for resource localization
     * @param normalizedUtcMidnight The date in milliseconds (UTC midnight)
     * @param showFullDate          Used to show a fuller-version of the date, which always
     *                              contains either the day of the week, today, or tomorrow, in
     *                              addition to the date.
     * @return A user-friendly representation of the date such as "Today, June 8", "Tomorrow",
     * or "Friday"
     */
    public static String getFriendlyDateString(Context context, long normalizedUtcMidnight, boolean showFullDate) {


        long localDate = getLocalMidnightFromNormalizedUtcDate(normalizedUtcMidnight);

        long daysFromEpochToProvidedDate = elapsedDaysSinceEpoch(localDate);


        long daysFromEpochToToday = elapsedDaysSinceEpoch(System.currentTimeMillis());

        if (daysFromEpochToProvidedDate == daysFromEpochToToday || showFullDate) {

            String dayName = getDayName(context, localDate);
            String readableDate = getReadableDateString(context, localDate);
            if (daysFromEpochToProvidedDate - daysFromEpochToToday < 2) {

                String localizedDayName = new SimpleDateFormat("EEEE").format(localDate);
                return readableDate.replace(localizedDayName, dayName);
            } else {
                return readableDate;
            }
        } else if (daysFromEpochToProvidedDate < daysFromEpochToToday + 7) {
            /* If the input date is less than a week in the future, just return the day name. */
            return getDayName(context, localDate);
        } else {
            int flags = DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_NO_YEAR
                    | DateUtils.FORMAT_ABBREV_ALL
                    | DateUtils.FORMAT_SHOW_WEEKDAY;

            return DateUtils.formatDateTime(context, localDate, flags);
        }
    }

    /**
     * Returns a date string in the format specified, which shows an abbreviated date without a
     * year.
     *
     * @param context      Used by DateUtils to format the date in the current locale
     * @param timeInMillis Time in milliseconds since the epoch (local time)
     * @return The formatted date string
     */
    private static String getReadableDateString(Context context, long timeInMillis) {
        int flags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY;

        return DateUtils.formatDateTime(context, timeInMillis, flags);
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "Wednesday".
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds (UTC time)
     * @return the string day of the week
     */
    private static String getDayName(Context context, long dateInMillis) {
        /*
         * If the date is today, return the localized version of "Today" instead of the actual
         * day name.
         */
        long daysFromEpochToProvidedDate = elapsedDaysSinceEpoch(dateInMillis);
        long daysFromEpochToToday = elapsedDaysSinceEpoch(System.currentTimeMillis());

        int daysAfterToday = (int) (daysFromEpochToProvidedDate - daysFromEpochToToday);

        switch (daysAfterToday) {
            case 0:
                return context.getString(R.string.today);
            case 1:
                return context.getString(R.string.tomorrow);

            default:
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                return dayFormat.format(dateInMillis);
        }
    }
}