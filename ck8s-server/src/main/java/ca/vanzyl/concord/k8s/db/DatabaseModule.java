package ca.vanzyl.concord.k8s.db;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.walmartlabs.concord.db.DatabaseChangeLogProvider;

import javax.inject.Named;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

@Named
public class DatabaseModule implements Module {

    @Override
    public void configure(Binder binder) {
        newSetBinder(binder, DatabaseChangeLogProvider.class).addBinding().to(DBChangeLogProvider.class);
    }
}
