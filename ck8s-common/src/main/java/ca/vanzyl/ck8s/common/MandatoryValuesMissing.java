package ca.vanzyl.ck8s.common;

import java.util.Arrays;
import java.util.List;

public class MandatoryValuesMissing
        extends IllegalStateException
{

    private final List<String> missed;

    public MandatoryValuesMissing(String[] keys)
    {
        this.missed = Arrays.asList(keys);
    }

    public List<String> missed()
    {
        return missed;
    }

    public String getMessage()
    {
        return "mandatory values '" + missed + "' missing";
    }
}
