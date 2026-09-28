package ca.vanzyl.ck8s.secrets;

import com.walmartlabs.concord.runtime.v2.sdk.Context;

import java.util.Map;

public interface SecretsRetriever
{

    void delete(Context context, String secretName);

    void put(Context context, String secretName, String value, String description);

    String get(Context context, String secretName);

    Map<String,String> map(Context context);
}
