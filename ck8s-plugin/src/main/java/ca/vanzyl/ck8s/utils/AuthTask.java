package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.apache.commons.codec.digest.Md5Crypt;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.nio.file.Files.writeString;

@Named("ck8sAuth")
public class AuthTask
        implements Task
{

    public static String htpasswdEntry(String username, String password)
    {
        return username + ":" + Md5Crypt.md5Crypt(password.getBytes());
    }

    public static String htpasswdFile(String username, String password, String file)
            throws IOException
    {
        String entry = htpasswdEntry(username, password);
        writeString(Paths.get(file), format("%s%n", entry));
        return file;
    }
}
