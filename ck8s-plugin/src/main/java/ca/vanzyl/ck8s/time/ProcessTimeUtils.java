package ca.vanzyl.ck8s.time;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ProcessTimeUtils {

    public static Duration elapsedDuration(OffsetDateTime start, OffsetDateTime end) {
        return Duration.between(start, end);
    }

    public static String elapsedTime(OffsetDateTime start, OffsetDateTime end) {
        Duration difference = elapsedDuration(start, end);
        long minutes = difference.toMinutes();
        long seconds = difference.toSecondsPart();
        return String.format("%d minutes %d seconds", minutes, seconds);
    }

    public static void main(String[] args) throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        Thread.sleep(100000);
        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC);
        System.out.println(elapsedTime(start, end));
    }
}
