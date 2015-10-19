package org.jboss.elasticsearch.tools.content;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * Utility Class for computing working time between two dates
 *
 * @author roney.stein
 */
public class WorkingTime {

    protected static int defaultWorkingDayStartHour = 8;
    protected static int defaultWorkingDayEndHour = 18;
    protected static int defaultWorkingHoursPerDay = 8;
    protected static int defaultWorkingDayLunchHours = 0;
    protected static int defaultWorkingDayLunchHour = 12;
    protected static String defaultDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    protected static int defaultTimeDifThreshold = 5;
    protected String firstDateString;
    protected String lastDateString;
    protected String sourceDateFormat;
    protected int workingDayEndHour;
    protected int workingDayStartHour;
    protected int workingHoursPerDay;
    protected int workingDayLunchHours;
    protected int workingDayLunchHour;
    // total minutes in the period
    protected int totalMinutes;
    // total working minutes
    protected int workingMinutes;
    protected int timeDifThreshold;

    public WorkingTime(String startingDate, String endingDate) {
        this(startingDate, endingDate, defaultDateFormat, defaultWorkingDayStartHour, defaultWorkingDayEndHour,
                defaultWorkingHoursPerDay, defaultWorkingDayLunchHour, defaultWorkingDayLunchHours);
    }

    public WorkingTime(String startingDate, String endingDate, String dateFormat) {
        this(startingDate, endingDate, dateFormat, defaultWorkingDayStartHour, defaultWorkingDayEndHour,
                defaultWorkingHoursPerDay, defaultWorkingDayLunchHour, defaultWorkingDayLunchHours);
    }

    public WorkingTime(String startingDate, String endingDate, String dateFormat,
                            int startHour, int endHour, int hoursPerDay, int lunchHours, int lunchHour) {
        if ( startingDate == null || endingDate == null) {
            throw new NullPointerException();
        }
        firstDateString = startingDate;
        lastDateString = endingDate;
        sourceDateFormat = dateFormat;
        workingDayStartHour = startHour;
        workingDayEndHour = endHour;
        workingHoursPerDay = hoursPerDay;
        workingDayLunchHour = lunchHour;
        workingDayLunchHours = lunchHours;
        timeDifThreshold = defaultTimeDifThreshold;
        calcWorkingTime();
    }

    private void calcWorkingTime() {

        DateTime firstDate;
        DateTime lastDate;
        int totalWorkedHours=0;
        Duration totalWorkedDuration = new Duration(Duration.ZERO);
        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(sourceDateFormat);

        firstDate = dateFormatter.parseDateTime(firstDateString);
        lastDate = dateFormatter.parseDateTime(lastDateString);

        // The upper limit of the working time in the start day
        DateTime workingEndFirstDate = new DateTime(firstDate.getYear(), firstDate.getMonthOfYear(),
                firstDate.getDayOfMonth(), workingDayEndHour, 0, firstDate.getZone());

        // The second day, same time
        DateTime nextFirstDate = firstDate.plusDays(1);

        // The second day, starting working hours
        DateTime workingStartNextDay = new DateTime(nextFirstDate.getYear(), nextFirstDate.getMonthOfYear(),
                nextFirstDate.getDayOfMonth(), 0, 0, nextFirstDate.getZone());

        // The last day, starting working hours
        DateTime workingStartLastDate = new DateTime(lastDate.getYear(), lastDate.getMonthOfYear(),
                lastDate.getDayOfMonth(), workingDayStartHour, 0, lastDate.getZone());

        // The entire interval
        Interval intervalFull = new Interval(firstDate, lastDate);
        totalMinutes = Minutes.minutesIn(intervalFull).getMinutes();

        // The max interval in the start day, in business hours only
        Interval businessWorkedHours;
        if ( firstDate.isBefore(workingEndFirstDate)) {
            businessWorkedHours = new Interval(firstDate, workingEndFirstDate);
        } else {
            businessWorkedHours = new Interval(firstDate, firstDate);
        }


        // Total worked hours and Duration straight
        Hours workedHours = Hours.hoursIn(intervalFull);
        Duration workedDuration = new Duration(intervalFull.getStart().toInstant(), intervalFull.getEnd().toInstant());

        // Does the the interval ends in the same day?
        if ( ! intervalFull.contains(workingStartNextDay.toInstant()) ) {
            // Does the interval ends after the business hours?
            // if so, treats as if it ended in the end of business hours
            //TODO: subtract lunch time
            if (intervalFull.contains(workingEndFirstDate.toInstant())) {
                totalWorkedHours = Hours.hoursIn(businessWorkedHours).getHours();
                totalWorkedDuration = totalWorkedDuration.plus( new Duration(
                                businessWorkedHours.getStart().toInstant(),
                                businessWorkedHours.getEnd().toInstant() ));
            } else {
                // Else, the total number of hours will be the reported by the interval
                //TODO: subtract lunch time
                totalWorkedHours = workedHours.getHours();
                totalWorkedDuration = workedDuration;
            }
        } else {
            //TODO: subtract lunch time
            //Gets the hours worked in the start day
            totalWorkedHours = Hours.hoursIn(businessWorkedHours).getHours();
            totalWorkedDuration = totalWorkedDuration.plus(new Duration(
                    businessWorkedHours.getStart().toInstant(),
                    businessWorkedHours.getEnd().toInstant()));

            //TODO: subtract lunch time
            //Gets the hours worked in the end day
            Interval lastBusinessWorkedHours;

            if ( workingStartLastDate.compareTo(lastDate.toInstant()) < 0  ) {
                lastBusinessWorkedHours = new Interval(workingStartLastDate, lastDate);
            } else {
                lastBusinessWorkedHours = new Interval(workingStartLastDate, workingStartLastDate);
            }

            totalWorkedHours += Hours.hoursIn(lastBusinessWorkedHours).getHours();
            totalWorkedDuration = totalWorkedDuration.plus(new Duration(
                    lastBusinessWorkedHours.getStart().toInstant(),
                    lastBusinessWorkedHours.getEnd().toInstant() ));

            //Hours worked in between
            //TODO: subtract all the hollidays in between
            Interval intervalInBetween = new Interval(workingStartNextDay, workingStartLastDate);
            int hoursInBetween = (Days.daysIn(intervalInBetween).getDays()
                    // minus saturdays and sundays
                    - (Weeks.weeksIn(intervalInBetween).getWeeks() * 2 )) * workingHoursPerDay;

            totalWorkedHours += hoursInBetween;
            totalWorkedDuration = totalWorkedDuration.plus((new Period(0,0,0,0,hoursInBetween,0,0,0)).toStandardDuration());
        }

        workingMinutes = (int) (long) totalWorkedDuration.getStandardMinutes();
    }

    public int getWorkingMinutes() {
        return workingMinutes;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public int getWorkingHours() {
        return workingMinutes / 60;
    }

    public int getWorkingHoursRoundUp() {
        if ( workingMinutes > timeDifThreshold) {
            Double hoursUp = Math.ceil((double) workingMinutes / 60);
            return hoursUp.intValue();
        } else {
            return getWorkingHours();
        }
    }

    public int getWorkingHoursRoundUp(int threshold) {
        if ( workingMinutes > threshold) {
            Double hoursUp = Math.ceil((double) workingMinutes / 60);
            return hoursUp.intValue();
        } else {
            return getWorkingHours();
        }
    }

}
