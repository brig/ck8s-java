package ca.vanzyl.ck8s.cloudformation;

import com.walmartlabs.concord.svm.ExecutionListener;
import com.walmartlabs.concord.svm.Frame;
import com.walmartlabs.concord.svm.Runtime;
import com.walmartlabs.concord.svm.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Singleton
public class CloudFormation implements ExecutionListener {

    private final static Logger log = LoggerFactory.getLogger(CloudFormation.class);

    private final StatementMergeStrategy mergeStrategy = new StatementMergeStrategy();

    private final List<Statement> statements = new ArrayList<>();

    public List<Statement> statements() {
        return statements;
    }

    public void statement(Statement statement) {
        addOrMergeStatement(statement);

        log.info("BRIG: {}", statement);
    }

    @Override
    public void onProcessError(com.walmartlabs.concord.svm.Runtime runtime, State state, Exception e) {

//        if (isDisabled()) {
//            return;
//        }
//
//        this.stateChangesProducer.onProcessError(runtime, state, e);
//
//        generateReport();

        log.info("{}", Serializer.serialize(this));
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        log.info("{}", Serializer.serialize(this));


//        if (isDisabled()) {
//            return;
//        }
//
//        if (isSuspended(state)) {
//            persistenceService.persistFile(PROCESS_INFO_FILENAME,
//                    out -> objectMapper.writeValue(out, this.processInfo));
//
//            return;
//        }
//
//        this.stateChangesProducer.afterProcessEnds(runtime, state, lastFrame);
//
//        generateReport();
    }

    private void addOrMergeStatement(Statement newStmt) {
        for (int i = 0; i < statements.size(); i++) {
            var existing = statements.get(i);
            var merged = mergeStrategy.tryMerge(existing, newStmt);
            if (merged.isPresent()) {
                statements.set(i, merged.get());
                return;
            }
        }

        statements.add(newStmt);
    }
}
