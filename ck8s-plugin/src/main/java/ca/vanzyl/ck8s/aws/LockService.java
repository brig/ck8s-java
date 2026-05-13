package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.client2.*;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;
import java.util.concurrent.Callable;

@Named
public class LockService {

    private final static Logger log = LoggerFactory.getLogger(LockService.class);

    private static final int REQUEST_RETRY_COUNT = 5;
    private static final long REQUEST_RETRY_INTERVAL = 15000;

    private static final long LOCK_RETRY_INTERVAL = 5000;

    private final InstanceId instanceId;
    private final ApiClient apiClient;

    @Inject
    public LockService(InstanceId instanceId, ApiClient apiClient) {
        this.instanceId = instanceId;
        this.apiClient = apiClient;
    }

    public Lock lock(String lockName) throws ApiException {
        while (!Thread.currentThread().isInterrupted()) {
            LockResult lock = withRetry(() -> new ProcessLocksApi(apiClient).tryLock(instanceId.getValue(), lockName, "ORG"));
            boolean result = lock.getAcquired();
            if (result) {
                log.info("Lock '{}' -> ok", lockName);
                return new Lock(instanceId.getValue(), lockName, apiClient);
            }
            log.info("Lock '{}' -> locked by {}, waiting {}ms", lockName, lock.getInfo().getInstanceId(), LOCK_RETRY_INTERVAL);
            sleep(LOCK_RETRY_INTERVAL);
        }
        return null;
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T withRetry(Callable<T> c) throws ApiException {
        return ClientUtils.withRetry(REQUEST_RETRY_COUNT, REQUEST_RETRY_INTERVAL, c);
    }

    public static class Lock implements AutoCloseable {

        private final UUID instanceId;
        private final String lockName;
        private final ApiClient apiClient;

        private Lock(UUID instanceId, String lockName, ApiClient apiClient) {
            this.instanceId = instanceId;
            this.lockName = lockName;
            this.apiClient = apiClient;
        }

        public void unlock() throws ApiException {
            withRetry(() -> {
                new ProcessLocksApi(apiClient).unlock(instanceId, lockName, "ORG");
                return null;
            });
            log.info("Unlock {} -> ok", lockName);
        }

        @Override
        public void close() throws Exception {
            unlock();
        }
    }
}
