package storage.common;

import logs.TLog;
import storage.LogsFile;

public interface LogsVisitor {

    public boolean onRead(final LogsFile logs, final TLog log);
}
