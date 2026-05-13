package ca.vanzyl.ck8s.secrets;

import java.util.Map;

public interface SecretsRetriever
{

    void delete(String secretName);

    void put(String secretName, String value, String description);

    String get(String secretName);

    Map<String,String> map();
}
