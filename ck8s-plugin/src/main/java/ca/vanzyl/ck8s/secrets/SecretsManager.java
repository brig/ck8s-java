package ca.vanzyl.ck8s.secrets;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SecretsManager
{

    private final ImmutablesYamlMapper mapper;

    public SecretsManager()
    {
        this.mapper = new ImmutablesYamlMapper();
    }

    /**
     * @throws SecretsManagerException if the input is invalid
     */
    public List<Secret> load(String input)
    {
        try {
            return mapper.read(input, Secrets.class).list();
        }
        catch (IOException e) {
            throw new SecretsManagerException(e.getMessage());
        }
    }

    /**
     * @throws SecretsManagerException if the write fails
     */
    public String serialize(Secrets secrets)
    {
        try {
            return mapper.write(secrets);
        }
        catch (IOException e) {
            throw new SecretsManagerException(e.getMessage());
        }
    }
}
