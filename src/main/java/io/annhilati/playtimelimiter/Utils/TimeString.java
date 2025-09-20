package io.annhilati.playtimelimiter.Utils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeString {
    
    public Duration DurationFromString(String string) {

        String regex = "(\\d+d)?(\\d+h)?(\\d+m)?";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);

        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                days += Long.parseLong(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                hours += Long.parseLong(matcher.group(2));
            }
            if (matcher.group(3) != null) {
                minutes += Long.parseLong(matcher.group(3));
            }
            if (matcher.group(4) != null) {
                seconds += Long.parseLong(matcher.group(3));
            }
        }

        return Duration.ZERO.plusDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

}
