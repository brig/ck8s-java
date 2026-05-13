package ca.vanzyl.ck8s.secrets;

public interface SecretsProvider
{

    String get(String secretName);
}