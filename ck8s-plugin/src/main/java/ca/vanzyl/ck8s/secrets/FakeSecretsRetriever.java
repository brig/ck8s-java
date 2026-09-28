package ca.vanzyl.ck8s.secrets;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FakeSecretsRetriever
        implements SecretsRetriever
{

    private final static Logger log = LoggerFactory.getLogger(FakeSecretsRetriever.class);

    @Override
    public String get(Context context, String key)
    {
        return "******";
    }

    @Override
    public Map<String, String> map(Context context) {
        return null;
    }

    @Override
    public void delete(Context context, String key)
    {
        log.warn("Delete operation ignored");
    }

    @Override
    public void put(Context context, String key, String value, String description)
    {
        log.warn("Update operation ignored");
    }

    @Override
    public String toString()
    {
        return "fake secrets";
    }
}
