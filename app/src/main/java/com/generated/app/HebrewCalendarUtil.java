package com.generated.app;

import java.util.Calendar;

/**
 * Accurate mathematical converter from Gregorian dates to Hebrew Calendar dates,
 * generating correct Hebrew text (day, month, year in Gematria format).
 */
public class HebrewCalendarUtil {

    private static final String[] HEBREW_MONTHS_NORMAL = {
            "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר",
            "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    };

    private static final String[] HEBREW_MONTHS_LEAP = {
            "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר ב׳",
            "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    };

    private static final String[] HEBREW_DAYS_OF_WEEK = {
            "יום ראשון", "יום שני", "יום שלישי", "יום רביעי",
            "יום חמישי", "יום שישי", "שבת קודש"
    };

    public static class HebrewDate {
        public final int day;
        public final int month; // 1-based (1 = Tishrei)
        public final int year;
        public final boolean isLeap;

        public HebrewDate(int day, int month, int year, boolean isLeap) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isLeap = isLeap;
        }

        public String getMonthName() {
            if (isLeap) {
                if (month >= 1 && month <= HEBREW_MONTHS_LEAP.length) {
                    return HEBREW_MONTHS_LEAP[month - 1];
                }
            } else {
                if (month >= 1 && month <= HEBREW_MONTHS_NORMAL.length) {
                    return HEBREW_MONTHS_NORMAL[month - 1];
                }
            }
            return "";
        }

        public String formatHebrewDateString() {
            return formatGematriaNumber(day, true) + " ב" + getMonthName() + " " + formatHebrewYear(year);
        }
    }

    public static boolean isHebrewLeapYear(int hYear) {
        return ((7 * hYear + 1) % 19) < 7;
    }

    // Convert absolute day (days since 1/1/0001 Gregorian) to Hebrew Date
    public static HebrewDate getHebrewDate(Calendar cal) {
        int gYear = cal.get(Calendar.YEAR);
        int gMonth = cal.get(Calendar.MONTH) + 1; // 1-12
        int gDay = cal.get(Calendar.DAY_OF_MONTH);

        long absDate = gregorianToAbsolute(gYear, gMonth, gDay);
        return absoluteToHebrew(absDate);
    }

    public static String getDayOfWeekHebrew(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday
        return HEBREW_DAYS_OF_WEEK[day - 1];
    }

    private static long gregorianToAbsolute(int year, int month, int day) {
        long d = day;
        for (int m = 1; m < month; ++m) {
            d += daysInGregorianMonth(m, year);
        }
        return d + 365L * (year - 1) + (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400;
    }

    private static int daysInGregorianMonth(int month, int year) {
        if (month == 2) {
            boolean leap = (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
            return leap ? 29 : 28;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }

    private static long hebrewCalendarElapsedDays(int hYear) {
        long monthsElapsed = (235L * (hYear - 1) + 1) / 19;
        long partsElapsed = 204L + 793L * (monthsElapsed % 1080);
        long hoursElapsed = 5L + 12L * monthsElapsed + 793L * (monthsElapsed / 1080) + partsElapsed / 1080;
        long conDay = 1L + 29L * monthsElapsed + hoursElapsed / 24;
        long conParts = 1080L * (hoursElapsed % 24) + (partsElapsed % 1080);

        long altDay;
        if (conParts >= 19440L ||
                (conParts >= 9924L && conDay % 7 == 3 && !isHebrewLeapYear(hYear)) ||
                (conParts >= 16789L && conDay % 7 == 2 && isHebrewLeapYear(hYear - 1))) {
            altDay = conDay + 1;
        } else {
            altDay = conDay;
        }

        if (altDay % 7 == 0 || altDay % 7 == 3 || altDay % 7 == 5) {
            altDay += 1;
        }
        return altDay;
    }

    private static long daysInHebrewYear(int hYear) {
        return hebrewCalendarElapsedDays(hYear + 1) - hebrewCalendarElapsedDays(hYear);
    }

    private static HebrewDate absoluteToHebrew(long absDate) {
        // Absolute date epoch adjustment
        long approxYear = (absDate + 1373429L) / 365;
        int hYear = (int) approxYear;

        while (absDate < hebrewCalendarElapsedDays(hYear) - 1373429L) {
            hYear--;
        }
        while (absDate >= hebrewCalendarElapsedDays(hYear + 1) - 1373429L) {
            hYear++;
        }

        boolean isLeap = isHebrewLeapYear(hYear);
        int totalMonths = isLeap ? 13 : 12;
        long firstDayOfYear = hebrewCalendarElapsedDays(hYear) - 1373429L;
        long dayInYear = absDate - firstDayOfYear + 1;

        int hMonth = 1;
        long daysInY = daysInHebrewYear(hYear);
        boolean kislevCheshvanShort = (daysInY == 353 || daysInY == 383);
        boolean kislevCheshvanFull = (daysInY == 355 || daysInY == 385);

        for (int m = 1; m <= totalMonths; m++) {
            int mDays = getDaysInHebrewMonth(m, isLeap, kislevCheshvanShort, kislevCheshvanFull);
            if (dayInYear <= mDays) {
                hMonth = m;
                break;
            }
            dayInYear -= mDays;
        }

        return new HebrewDate((int) dayInYear, hMonth, hYear, isLeap);
    }

    private static int getDaysInHebrewMonth(int month, boolean isLeap, boolean isShort, boolean isFull) {
        if (month == 1) return 30; // Tishrei
        if (month == 2) return isFull ? 30 : 29; // Cheshvan
        if (month == 3) return isShort ? 29 : 30; // Kislev
        if (month == 4) return 29; // Tevet
        if (month == 5) return 30; // Shevat
        if (isLeap) {
            if (month == 6) return 30; // Adar I
            if (month == 7) return 29; // Adar II
            if (month == 8) return 30; // Nisan
            if (month == 9) return 29; // Iyar
            if (month == 10) return 30; // Sivan
            if (month == 11) return 29; // Tammuz
            if (month == 12) return 30; // Av
            if (month == 13) return 29; // Elul
        } else {
            if (month == 6) return 29; // Adar
            if (month == 7) return 30; // Nisan
            if (month == 8) return 29; // Iyar
            if (month == 9) return 30; // Sivan
            if (month == 10) return 29; // Tammuz
            if (month == 11) return 30; // Av
            if (month == 12) return 29; // Elul
        }
        return 30;
    }

    // Convert number to Gematria representation (1-30)
    public static String formatGematriaNumber(int num, boolean withGershayim) {
        if (num <= 0) return String.valueOf(num);
        if (num == 15) return "ט״ו";
        if (num == 16) return "ט״ז";

        String[] tens = {"", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ"};
        String[] units = {"", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט"};

        StringBuilder sb = new StringBuilder();
        int t = num / 10;
        int u = num % 10;
        if (t > 0 && t < tens.length) sb.append(tens[t]);
        if (u > 0 && u < units.length) sb.append(units[u]);

        String s = sb.toString();
        if (!withGershayim) return s;

        if (s.length() == 1) {
            return s + "׳";
        } else if (s.length() >= 2) {
            return s.substring(0, s.length() - 1) + "״" + s.charAt(s.length() - 1);
        }
        return s;
    }

    public static String formatHebrewYear(int year) {
        int rem = year % 1000;
        StringBuilder sb = new StringBuilder();
        int[] val = {400, 300, 200, 100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        String[] letter = {"ת", "ש", "ר", "ק", "צ", "פ", "ע", "ס", "נ", "מ", "ל", "כ", "י", "ט", "ח", "ז", "ו", "ה", "ד", "ג", "ב", "א"};

        if (rem == 15) return "תש״ו"; // Special handling if needed

        int curr = rem;
        for (int i = 0; i < val.length; i++) {
            while (curr >= val[i]) {
                if (curr == 15) {
                    sb.append("טו");
                    curr = 0;
                    break;
                } else if (curr == 16) {
                    sb.append("טז");
                    curr = 0;
                    break;
                }
                sb.append(letter[i]);
                curr -= val[i];
            }
        }

        String s = sb.toString();
        if (s.length() == 1) {
            return s + "׳";
        } else if (s.length() >= 2) {
            return s.substring(0, s.length() - 1) + "״" + s.charAt(s.length() - 1);
        }
        return s;
    }
}
