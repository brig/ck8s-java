package ca.vanzyl.ck8s.state;

import java.io.OutputStream;
import java.io.Serializable;

public interface Entity extends Serializable {

    String entityName();

    void dump(OutputStream out);
}
