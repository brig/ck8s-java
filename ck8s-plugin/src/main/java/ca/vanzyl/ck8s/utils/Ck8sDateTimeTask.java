package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Named("ck8sDatetime")
@DryRunReady
public class Ck8sDateTimeTask implements Task {

    public ZonedDateTime parseIsoDateTime(String input) {
        var formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        return ZonedDateTime.parse(input, formatter);
    }

    public ZonedDateTime nowDateTime() {
        return ZonedDateTime.now();
    }

    public String toIsoString(ZonedDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    public boolean isExpired(String input, Integer days) {
        if (days == null) {
            return false;
        }

        var parsed = parseIsoDateTime(input);
        var threshold = nowDateTime().minusDays(days);
        return parsed.isBefore(threshold);
    }
}
