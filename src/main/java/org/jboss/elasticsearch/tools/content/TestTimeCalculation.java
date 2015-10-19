
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * Created by roney on 16/10/15.
 * Teste utiliza Jollyday, objectlabkit e Joda-Time
 * objetivo: demonstrar calculo de working hours entre 2 datas considernado feriados e fds
 */
public class TestTimeCalculation {


    public static void main(String[] args) {

        String sourceDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        String firstDateString = "2014-10-16T10:00:00.000-0300";
        String lastDateString = "2014-10-16T18:00:00.000-0300";

        //TESTING
        System.out.println("Start date: " + firstDateString);
        System.out.println("End date  : " + lastDateString);

        int workingDayEndHour = 18;
        int workingDayStartHour = 8;
        int workingHoursPerDay = 8;

        int totalWorkedHours=0;
        Duration totalWorkedDuration = new Duration(Duration.ZERO);

        DateTime firstDate;
        DateTime lastDate;

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(sourceDateFormat);

        firstDate = dateFormatter.parseDateTime(firstDateString);
        lastDate = dateFormatter.parseDateTime(lastDateString);

        // The upper limit of the working time in the start day
        DateTime workingEndFirstDate = new DateTime(firstDate.getYear(),
                firstDate.getMonthOfYear(),
                firstDate.getDayOfMonth(),
                workingDayEndHour, 0,
                firstDate.getZone());

        // The second day, same time
        DateTime nextFirstDate = firstDate.plusDays(1);

        // The second day, starting working hours
        DateTime workingStartNextDay = new DateTime(nextFirstDate.getYear(),
                nextFirstDate.getMonthOfYear(),
                nextFirstDate.getDayOfMonth(), 0, 0,
                nextFirstDate.getZone());

        // The last day, starting working hours
        DateTime workingStartLastDate = new DateTime(lastDate.getYear(),
                lastDate.getMonthOfYear(),
                lastDate.getDayOfMonth(),
                workingDayStartHour, 0,
                lastDate.getZone()
                );
        


        // The entire interval
        Interval intervalFull = new Interval(firstDate, lastDate);

        // The max interval in the start day, in business hours only
        Interval businessWorkedHours = new Interval(firstDate, workingEndFirstDate);

        // Total worked hours and Duration straight
        Hours workedHours = Hours.hoursIn(intervalFull);
        Duration workedDuration = new Duration(intervalFull.getStart().toInstant(), intervalFull.getEnd().toInstant());

        // Testing
        System.out.println("horas no intervalo: " + workedHours.getHours());

        
        // Does the the interval ends in the same day?
        if ( ! intervalFull.contains(workingStartNextDay.toInstant()) ) {
            // Does the interval ends after the business hours?
            // if so, treats as if it ended in the end of business hours
            if (intervalFull.contains(workingEndFirstDate.toInstant())) {
                totalWorkedHours = Hours.hoursIn(businessWorkedHours).getHours();
                totalWorkedDuration = totalWorkedDuration.plus( new Duration(
                                businessWorkedHours.getStart().toInstant(),
                                businessWorkedHours.getEnd().toInstant() ));

                System.out.println("Only same day worked hours (extra): " + Integer.toString(totalWorkedHours));
            } else {
                // Else, the total number of hours will be the reported by the interval
                totalWorkedHours = workedHours.getHours();
                totalWorkedDuration = workedDuration;

                System.out.println("Only same day worked hours: " + Integer.toString(totalWorkedHours));
            }
        } else {
            
            //Gets the hours worked in the start day
            totalWorkedHours = Hours.hoursIn(businessWorkedHours).getHours();
            totalWorkedDuration = totalWorkedDuration.plus(new Duration(
                    businessWorkedHours.getStart().toInstant(),
                    businessWorkedHours.getEnd().toInstant()));
            System.out.println("Hours in the first day: " + Integer.toString(totalWorkedHours));
            
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
            System.out.println("Hours in the last day: " + Integer.toString(totalWorkedHours));

            //Hours worked in between
            int hoursInBetween = (Days.daysIn(intervalFull).getDays()
                    // minus first and last day
                    - 2
                    // minus saturdays and sundays
                    - (Weeks.weeksIn(intervalFull).getWeeks() * 2 )) * workingHoursPerDay;
            totalWorkedHours += hoursInBetween;
            totalWorkedDuration = totalWorkedDuration.plus((new Period(0,0,0,0,hoursInBetween,0,0,0)).toStandardDuration());

            System.out.println("Total # of days in between    : "
                    + Days.daysIn(intervalFull).getDays());
            System.out.println("Total # of working days in btw: "
                    + (Days.daysIn(intervalFull).getDays() - 2 - (Weeks.weeksIn(intervalFull).getWeeks() * 2 )));

            //TODO: subtract lunch time
            //TODO: subtract all the hollidays in between

        }

        System.out.println("Total # working hours: " + Integer.toString(totalWorkedHours) );
        System.out.println("Total # working hours (Duration): " + Long.toString(totalWorkedDuration.getStandardHours()) );
        System.out.println("Total # working minutes (Duration): " + Long.toString(totalWorkedDuration.getStandardMinutes()) );


    }




}
