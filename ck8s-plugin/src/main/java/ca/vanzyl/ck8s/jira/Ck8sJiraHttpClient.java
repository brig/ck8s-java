package ca.vanzyl.ck8s.jira;

import com.walmartlabs.concord.plugins.jira.JiraHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Ck8sJiraHttpClient extends JiraHttpClient {

    Ck8sJiraHttpClient url(String url);

    Ck8sJiraHttpClient successCode(int successCode);

    Ck8sJiraHttpClient jiraAuth(String auth);

    Map<String, Object> get() throws IOException;

    Map<String, Object> post(Map<String, Object> data) throws IOException;

    void post(File file) throws IOException;

    void put(Map<String, Object> data) throws IOException;

    void delete() throws IOException;

    List<Map<String, Object>> getList() throws IOException;

    static void assertResponse(int code, String result, int successCode) {
        if (code == successCode) {
            return;
        }

        if (code == 400) {
            throw new UnexpectedResponseException(code, "input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new UnexpectedResponseException(code, "User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new UnexpectedResponseException(code, "User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new UnexpectedResponseException(code, "Issue does not exist. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new UnexpectedResponseException(code, "Internal Server Error. Here are the full error details" + result);
        } else {
            throw new UnexpectedResponseException(code, "Error: " + result);
        }
    }

    class UnexpectedResponseException extends RuntimeException {

        private final int code;
        private final String message;

        public UnexpectedResponseException(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "UnexpectedResponseException{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
