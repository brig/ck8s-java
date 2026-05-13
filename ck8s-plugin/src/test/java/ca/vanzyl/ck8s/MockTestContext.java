package ca.vanzyl.ck8s;

import com.walmartlabs.concord.runtime.v2.model.ProcessDefinition;
import com.walmartlabs.concord.runtime.v2.model.Step;
import com.walmartlabs.concord.runtime.v2.runner.el.DefaultExpressionEvaluator;
import com.walmartlabs.concord.runtime.v2.runner.el.EvalContextFactoryImpl;
import com.walmartlabs.concord.runtime.v2.runner.el.FunctionHolder;
import com.walmartlabs.concord.runtime.v2.runner.el.resolvers.SensitiveDataProcessor;
import com.walmartlabs.concord.runtime.v2.runner.tasks.TaskProviders;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.svm.EvalResult;
import com.walmartlabs.concord.svm.Runtime;
import com.walmartlabs.concord.svm.State;
import com.walmartlabs.concord.svm.ThreadId;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class MockTestContext
        implements Context {

    private final DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator(new TaskProviders(), new FunctionHolder(), List.of(), List.of(), mock(SensitiveDataProcessor.class));
    private final EvalContextFactory evalContextFactory = new EvalContextFactoryImpl();

    private final Variables variables;
    private final Path workDir;

    public MockTestContext() {
        this (null, Map.of());
    }

    public MockTestContext(Path workDir) {
        this (workDir, Map.of());
    }

    public MockTestContext(Map<String, Object> variables) {
        this (null, variables);
    }

    public MockTestContext(Path workDir, Map<String, Object> variables) {
        this.variables = new MapBackedVariables(variables);
        this.workDir = workDir;
    }

    @Override
    public Path workingDirectory() {
        return workDir;
    }

    @Override
    public UUID processInstanceId() {
        return UUID.fromString("01618795-B956-4FC3-BD0F-8FE31FE819F1");
    }

    @Override
    public Variables variables() {
        return variables;
    }

    @Override
    public Variables defaultVariables() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public FileService fileService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public DockerService dockerService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SecretService secretService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public LockService lockService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ApiConfiguration apiConfiguration() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ProcessConfiguration processConfiguration() {
        return ProcessConfiguration.builder().build();
    }

    @Override
    public Execution execution() {
        return new Execution() {
            @Override
            public ThreadId currentThreadId() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Runtime runtime() {
                return new Runtime() {
                    @Override
                    public void spawn(State state, ThreadId threadId) {
                        throw new UnsupportedOperationException("not implemented");
                    }

                    @Override
                    public EvalResult eval(State state, ThreadId threadId) throws Exception {
                        throw new UnsupportedOperationException("not implemented");
                    }

                    @Override
                    public <T> T getService(Class<T> klass) {
                        if (klass == ExpressionEvaluator.class) {
                            return klass.cast(evaluator);
                        }
                        throw new UnsupportedOperationException("not implemented");
                    }
                };
            }

            @Override
            public State state() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public ProcessDefinition processDefinition() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Step currentStep() {
                return null;
            }

            @Override
            public String currentFlowName() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public UUID correlationId() {
                throw new UnsupportedOperationException("not implemented");
            }
        };
    }

    @Override
    public Compiler compiler() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <T> T eval(Object v, Class<T> type) {
        return evaluator.eval(EvalContext.builder().context(this).variables(variables()).build(), v, type);
    }

    @Override
    public <T> T eval(Object v, Map<String, Object> additionalVariables, Class<T> type) {
        return evaluator.eval(evalContextFactory.global(this, additionalVariables), v, type);
    }

    @Override
    public void suspend(String s) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void reentrantSuspend(String s, Map<String, Serializable> map) {
        throw new UnsupportedOperationException("not implemented");
    }
}
