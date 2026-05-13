package ca.vanzyl.ck8s.command;

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Collections;
import java.util.Map;

public class CommandTaskParams
{

    private final Variables variables;

    public CommandTaskParams(Variables variables)
    {
        this.variables = variables;
    }

    public boolean debug(boolean defaultValue)
    {
        return variables.getBoolean("debug", defaultValue);
    }

    public Map<String, String> envars()
    {
        return variables.getMap("env", Collections.emptyMap());
    }

    public String run()
    {
        return variables.assertString("run");
    }

    public boolean saveOutput()
    {
        return variables.getBoolean("saveOutput", false);
    }

    public boolean saveError()
    {
        return variables.getBoolean("saveErrorOutput", true);
    }

    public String responseFile()
    {
        return variables.getString("responseFile");
    }

    public Long timeout() {
        Number result = variables.getNumber("timeout", null);
        if (result == null) {
            return null;
        }

        return result.longValue();
    }
}
