package edduarte.protbox.utils;

import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class DateTime implements Comparable<DateTime>, Cloneable {

    private LocalDateTime dateTime;

    public static DateTime today() {
        Calendar c = GregorianCalendar.getInstance();
        return of(c);
    }

    public static DateTime of(int hour, int minute) {
        DateTime d = new DateTime();
        d.set(0, 1, 1, hour, minute);
        return d;
    }

    public static DateTime of(int year, int month, int day, int hour, int minute) {
        DateTime d = new DateTime();
        d.set(year, month, day, hour, minute);
        return d;
    }


    public static DateTime of(Calendar c) {
        DateTime d = new DateTime();
        d.set(c);
        return d;
    }

    public static DateTime of(Date d) {
        DateTime dt = new DateTime();
        Calendar c = new GregorianCalendar();
        c.setTime(d);
        dt.set(c);
        return dt;
    }

    public static DateTime of(String string) {
        DateTime dt = new DateTime();
        String[] split1 = string.split(" ");
        String dateString = split1[0];
        String timeString = split1[1];

        String[] split2 = dateString.split("/");
        int day = Integer.parseInt(split2[0]);
        int month = Integer.parseInt(split2[1]);
        int year = Integer.parseInt(split2[2]);

        String[] split3 = timeString.split(":");
        int hour = Integer.parseInt(split3[0]);
        int minutes = Integer.parseInt(split3[1]);

        dt.set(year, month, day, hour, minutes);

        return dt;
    }

    public static DateTime of(LocalDateTime dt) {
        DateTime dt2 = new DateTime();
        dt2.set(dt);
        return dt2;
    }

    public static DateTime of(DateTime dt) {
        DateTime dt2 = new DateTime();
        dt2.set(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getHour(), dt.getMinutes());
        return dt2;
    }


    private DateTime() {
    }


    private void set(int year, int month, int day, int hour, int minutes) {
        this.dateTime = new LocalDateTime(year, month, day, hour, minutes);
    }


    private void set(Calendar c) {
        this.dateTime = new LocalDateTime(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE)
        );
    }


    private void set(LocalDateTime dt) {
        this.dateTime = new LocalDateTime(dt);
    }


    public DateTime setYear(int year) {
        set(dateTime.withYear(year));
        return this;
    }


    public int getYear() {
        return dateTime.getYear();
    }


    public DateTime setMonth(int month) {
        set(dateTime.withMonthOfYear(month));
        return this;
    }


    public int getMonth() {
        return dateTime.getMonthOfYear();
    }


    public DateTime setDay(int day) {
        set(dateTime.withDayOfMonth(day));
        return this;
    }


    public int getDay() {
        return dateTime.getDayOfMonth();
    }


    public DateTime setHour(int hour) {
        set(dateTime.withHourOfDay(hour));
        return this;
    }


    public int getHour() {
        return dateTime.getHourOfDay();
    }


    public DateTime setMinutes(int minutes) {
        set(dateTime.withMinuteOfHour(minutes));
        return this;
    }


    public int getMinutes() {
        return dateTime.getMinuteOfHour();
    }


    public DateTime plusMinutes(int minutes) {
        set(dateTime.plusMinutes(minutes));
        return this;
    }


    public DateTime minusMinutes(int minutes) {
        set(dateTime.minusMinutes(minutes));
        return this;
    }


    public DateTime plusDays(int days) {
        set(dateTime.plusDays(days));
        return this;
    }


    public DateTime minusDays(int days) {
        set(dateTime.minusDays(days));
        return this;
    }


    public DateTime plusMonths(int months) {
        set(dateTime.plusMonths(months));
        return this;
    }


    public DateTime minusMonths(int months) {
        set(dateTime.minusMonths(months));
        return this;
    }


    public DateTime plusYears(int years) {
        set(dateTime.plusYears(years));
        return this;
    }


    public DateTime minusYears(int years) {
        set(dateTime.minusYears(years));
        return this;
    }


    public int numOfDaysUntil(DateTime dt) {
        int compare = dateCompareTo(dt);

        if (compare == 0) {
            return 0;
        }

        int extraDay = 0;
        if (compare > 0 && timeCompareTo(dt) < 0) {
            extraDay = -1;
        } else if (compare < 0 && timeCompareTo(dt) > 0) {
            extraDay = 1;
        }

        LocalDateTime date1 = this.dateTime;
        LocalDateTime date2 = dt.dateTime;

        return Days.daysBetween(date1, date2).getDays() + extraDay;
    }


    public int numOfMonthsUntil(DateTime dt) {
        int compare = dateCompareTo(dt);

        if (compare == 0) {
            return 0;
        }

        int extraMonth = 0;
        if (compare > 0 && timeCompareTo(dt) < 0) {
            extraMonth = -1;
        } else if (compare < 0 && timeCompareTo(dt) > 0) {
            extraMonth = 1;
        }

        LocalDateTime date1 = this.dateTime;
        LocalDateTime date2 = dt.dateTime;

        return Months.monthsBetween(date1, date2).getMonths() + extraMonth;
    }


    public int getUniqueIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getYear());
        sb.append(getMonth());
        sb.append(getDay());
        sb.append(getHour());
        sb.append(getMinutes());

        return Integer.parseInt(sb.toString());
    }


    public LocalDateTime toJodaDateTime() {
        return new LocalDateTime(getYear(), getMonth(), getDay(), getHour(), getMinutes());
    }


    public GregorianCalendar toGregorianCalendar() {
        GregorianCalendar c = new GregorianCalendar();
        c.set(Calendar.YEAR, getYear());
        c.set(Calendar.MONTH, getMonth() - 1);
        c.set(Calendar.DAY_OF_MONTH, getDay());
        c.set(Calendar.HOUR_OF_DAY, getHour());
        c.set(Calendar.MINUTE, getMinutes());
        c.set(Calendar.SECOND, 0);
        return c;
    }


    public String timeToString() {
        String hour = String.valueOf(getHour());
        if (hour.length() == 1) {
            hour = "0" + hour;
        }

        String minute = String.valueOf(getMinutes());
        if (minute.length() == 1) {
            minute = "0" + minute;
        }

        return hour + ":" + minute;
    }


    @Override
    public String toString() {
        return
//                dateFormat.toString() + "\n" + dateFormat.format(toGregorianCalendar().getTime()) + "\n" +
                getDay() + "/" + getMonth() + "/" + getYear() + " " + timeToString();
    }


    public int dateCompareTo(DateTime another) {
        if (another == null) {
            return 0;
        }

        if (getYear() < another.getYear()) {
            return -1;
        }
        if (getYear() > another.getYear()) {
            return 1;
        }

        if (getMonth() < another.getMonth()) {
            return -1;
        }
        if (getMonth() > another.getMonth()) {
            return 1;
        }

        if (getDay() < another.getDay()) {
            return -1;
        }
        if (getDay() > another.getDay()) {
            return 1;
        }

        return 0;
    }


    public int timeCompareTo(DateTime another) {
        if (another == null) {
            return 0;
        }

        if (getHour() < another.getHour()) {
            return -1;
        }
        if (getHour() > another.getHour()) {
            return 1;
        }

        if (getMinutes() < another.getMinutes()) {
            return -1;
        }
        if (getMinutes() > another.getMinutes()) {
            return 1;
        }

        return 0;
    }


    @Override
    public int compareTo(DateTime another) {
        int dateCompareTo = dateCompareTo(another);
        return dateCompareTo == 0 ? timeCompareTo(another) : dateCompareTo;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof DateTime)) return false;

        DateTime dateTime = (DateTime) o;
        return compareTo(dateTime) == 0;
    }


    @Override
    public int hashCode() {
        return getYear() ^ getMonth() ^ getDay() ^ getHour() ^ getMonth();
    }


    @Override
    public DateTime clone() throws CloneNotSupportedException {
        return DateTime.of(this);
    }
}
