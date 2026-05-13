package ca.vanzyl.ck8s;

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.crypto.CryptoTaskV2;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;

@Named("awsCredentialsFromSecret")
public class AwsCredentialsFromSecret
        implements Task
{

    private final CryptoTaskV2 crypto;
    private final String orgName;

    @Inject
    public AwsCredentialsFromSecret(CryptoTaskV2 crypto, Context context)
    {
        this.crypto = crypto;
        this.orgName = context.processConfiguration().projectInfo().orgName();
    }

    public void export(String secretName)
            throws Exception
    {
        String path = crypto.exportAsFile(orgName, secretName, null);
        Path dest = Path.of(System.getProperty("user.home")).resolve(".aws");
        IOUtils.deleteRecursively(dest);
        IOUtils.unzip(Path.of(path), dest);
    }
}
