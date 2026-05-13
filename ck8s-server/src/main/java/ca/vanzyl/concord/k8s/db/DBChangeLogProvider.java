package ca.vanzyl.concord.k8s.db;

import com.walmartlabs.concord.db.DatabaseChangeLogProvider;
import com.walmartlabs.concord.db.MainDB;

@MainDB
public class DBChangeLogProvider implements DatabaseChangeLogProvider {

    @Override
    public String getChangeLogPath() {
        return "ca/vanzyl/concord/k8s/db/liquibase.xml";
    }

    @Override
    public String toString() {
        return "ck8s-db";
    }
}
