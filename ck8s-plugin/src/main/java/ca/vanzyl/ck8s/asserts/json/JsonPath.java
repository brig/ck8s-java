package ca.vanzyl.ck8s.asserts.json;

public class JsonPath
{

    private final String path;

    public JsonPath()
    {
        this("");
    }

    public JsonPath(String path)
    {
        this.path = path;
    }

    public JsonPath field(String fieldName)
    {
        return new JsonPath(path + "/" + fieldName);
    }

    public JsonPath index(int index)
    {
        return new JsonPath(path + "[" + index + "]");
    }

    @Override
    public String toString()
    {
        return path;
    }
}
