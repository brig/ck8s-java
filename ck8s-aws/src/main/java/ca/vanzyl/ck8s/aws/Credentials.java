package ca.vanzyl.ck8s.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class Credentials
{
    public static AwsCredentialsProvider get(String profile)
    {
        return DefaultCredentialsProvider.builder()
                .profileName(profile)
                .build();
    }
}
