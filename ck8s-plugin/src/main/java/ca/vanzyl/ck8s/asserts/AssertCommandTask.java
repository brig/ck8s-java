package ca.vanzyl.ck8s.asserts;

import ca.vanzyl.ck8s.command.CommandTask;
import com.walmartlabs.concord.runtime.v2.model.TaskCall;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Named("assertCommand")
@SuppressWarnings("unused")
public class AssertCommandTask
        implements Task
{

    private final static Logger log = LoggerFactory.getLogger(AssertCommandTask.class);

    private final AssertsTask assertsTask;
    private final CommandTask delegate;
    private final String stepName;

    @Inject
    public AssertCommandTask(Context context, AssertsTask assertsTask, CommandTask delegate)
    {
        this.assertsTask = assertsTask;
        this.delegate = delegate;
        this.stepName = getStepName(context);
    }

    private static String getStepName(Context context)
    {
        TaskCall taskCall = (TaskCall) context.execution().currentStep();
        if (taskCall == null || taskCall.getOptions() == null) {
            return null;
        }

        String stepNameOrExpression = (String) taskCall.getOptions().meta().get("segmentName");
        if (stepNameOrExpression == null) {
            return null;
        }

        try {
            return context.eval(stepNameOrExpression, String.class);
        }
        catch (Exception e) {
            log.warn("Can't eval step name", e);
        }
        return stepNameOrExpression;
    }

    private static void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public TaskResult execute(Variables input)
            throws Exception
    {
        int retry = input.getInt("retry", 3);
        int delay = input.getInt("delay", 10);

        Map<String, Object> cmdInputMap = new HashMap<>(input.toMap());
        cmdInputMap.put("saveOutput", true);
        Variables cmdInput = new MapBackedVariables(cmdInputMap);

        String error;
        int counter = 0;
        while (!Thread.currentThread().isInterrupted()) {
            TaskResult.SimpleResult taskResult = (TaskResult.SimpleResult) delegate.execute(cmdInput);

            if (taskResult.ok()) {
                String output = MapUtils.assertString(taskResult.toMap(), "output");
                error = checkExpected(input, output);
                if (error == null) {
                    return TaskResult.success();
                }
            }
            else {
                error = taskResult.error();
            }

            if (counter < retry) {
                log.info("assert error: {}. retry {} in {} sec", error, (counter + 1), delay);
                sleep(delay * 1000L);
                counter++;
            }
            else {
                return TaskResult.fail(stepName + "\n" + error);
            }
        }

        return TaskResult.fail("interrupted");
    }

    private String checkExpected(Variables input, String output)
    {
        boolean interpolateExpected = !input.getBoolean("skipInterpolate", false);
        String expectedJson = input.getString("expectedJson");
        if (expectedJson != null) {
            try {
                assertsTask.assertJson(expectedJson, output, interpolateExpected);
                return null;
            }
            catch (Exception e) {
                return "Assertion '" + expectedJson + "' error: " + e.getMessage();
            }
        }

        String expectedYaml = input.getString("expectedYaml");
        if (expectedYaml != null) {
            try {
                assertsTask.assertYaml(expectedYaml, output, interpolateExpected);
                return null;
            }
            catch (Exception e) {
                return "Assertion '" + expectedYaml + "' error: " + e.getMessage();
            }
        }

        String expectedValue = input.getString("expected");
        if (expectedValue != null) {
            if (!expectedValue.equals(output.trim())) {
                return "Expected result to be '" + expectedValue + "', but is '" + output.trim() + "'";
            }
        }

        String expectedPattern = input.getString("expectedPattern");
        if (expectedPattern != null) {
            Pattern pattern = Pattern.compile(expectedPattern, Pattern.CASE_INSENSITIVE);
            if (!pattern.matcher(output.trim()).matches()) {
                return "Expected result matching the pattern '" + expectedPattern + "', but is '" + output.trim() + "'";
            }
        }

        return null;
    }
}
