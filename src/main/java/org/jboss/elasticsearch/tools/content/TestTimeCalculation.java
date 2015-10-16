
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.Interval;


/**
 * Created by roney on 16/10/15.
 * Teste utiliza Jollyday, objectlabkit e Joda-Time
 * objetivo: demonstrar calculo de working hours entre 2 datas considernado feriados e fds
 */
public class TestTimeCalculation {


    public static void main(String[] args) {

        String CFG_DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        String sourceDateFormat;
        String startDateString = "2015-10-06T13:42:55.837-0300";
        String endDateString = "2015-10-13T13:42:55.837-0300";

        sourceDateFormat = CFG_DEFAULT_DATE_FORMAT;

        DateTime startDate;
        DateTime endDate;

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(sourceDateFormat);

        startDate = dateFormatter.parseDateTime(startDateString);
        endDate = dateFormatter.parseDateTime(endDateString);

        Interval intervalo = new Interval(startDate, endDate);
        Hours horas = Hours.hoursIn(intervalo);

        System.out.println("horas no intervalo: " + horas.getHours());


        // Semanas no intervalo
        org.joda.time.Weeks semanas = org.joda.time.Weeks.weeksIn(intervalo);

        // Dias de finais de semana a serem descontados
        int numWeeks = semanas.getWeeks();

        // feriados





        /*
        Algoritmo para calculo poderia ser:
        pego a data aleatoria no dia
        calculo o tempo até as 18h
        calculo quantos dias uteis se passam até o proximo momento

            sem contar os finais de semana (calculo quantas semanas se passam e tiro 2 dias de cada)



            sem contar os feriados (1 intervalo para cada feriado e verificamos os intervalos de cada um)

            biblioteca




        calculo o tempo util até o momento no dia final

         */

    }


}
