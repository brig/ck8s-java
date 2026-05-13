package ca.vanzyl.ck8s.python;

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PythonTaskParams
{
    private final Variables variables;

    PythonTaskParams(Variables variables)
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

    public String script()
    {
        return variables.assertString("script");
    }

    public List<String> args()
    {
        return variables.getList("args", Collections.emptyList())
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public boolean venv()
    {
        return variables.getBoolean("venv", true);
    }

    public boolean reuseVenv()
    {
        return variables.getBoolean("reuseVenv", true);
    }

    public String venvName()
    {
        return variables.getString("venvName", "");
    }

    public String responseFile()
    {
        return variables.getString("responseFile");
    }
}
