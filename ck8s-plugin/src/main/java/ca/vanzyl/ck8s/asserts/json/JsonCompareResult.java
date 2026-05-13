package ca.vanzyl.ck8s.asserts.json;

public class JsonCompareResult {

    private final boolean ok;
    private final String message;

    public JsonCompareResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static JsonCompareResult ok() {
        return new JsonCompareResult(true, null);
    }

    public static JsonCompareResult fail(String message) {
        return new JsonCompareResult(false, message);
    }

    public boolean success() {
        return ok;
    }

    public boolean failed() {
        return !ok;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "JsonCompareResult{" +
                "ok=" + ok +
                ", message='" + message + '\'' +
                '}';
    }
}
