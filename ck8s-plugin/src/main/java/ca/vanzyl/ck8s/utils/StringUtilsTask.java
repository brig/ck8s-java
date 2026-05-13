package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;

@Named("stringUtils")
@DryRunReady
public class StringUtilsTask
        implements Task
{

    public String truncate(String str, int maxLength)
    {
        if (str == null) {
            return null;
        }

        if (maxLength <= 3) {
            throw new IllegalArgumentException("maxLength should be greater than 3");
        }

        if (str.length() <= maxLength) {
            return str;
        }

        int halfLength = (maxLength - 3) / 2;
        String firstHalf = str.substring(0, halfLength);
        int secondHalfLength = maxLength - 3 - halfLength;
        String secondHalf = str.substring(str.length() - secondHalfLength);

        return firstHalf + "..." + secondHalf;
    }

    public String truncateClean(String str, int maxLength)
    {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public static String stripEnd(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        int len = str.length();
        while (len > 0 && Character.isWhitespace(str.charAt(len - 1))) {
            len--;
        }
        return str.substring(0, len);
    }

    public static String join(Iterable<String> iterable, String separator) {
        var sb = new StringBuilder();
        var iterator = iterable.iterator();

        if (iterator.hasNext()) {
            sb.append(iterator.next());
        }

        while (iterator.hasNext()) {
            sb.append(separator).append(iterator.next());
        }

        return sb.toString();
    }
}
