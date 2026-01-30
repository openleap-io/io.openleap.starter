package io.openleap.starter.core.lock.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface LockRepository {

    /**
     * Acquire a session advisory lock, blocking until available
     */
    void acquireSessionLock(Connection conn, long lockKey) throws SQLException;

    /**
     * Try to acquire a session advisory lock; return true if successful, false if already held
     */
    boolean tryAcquireSessionLock(Connection conn, long lockKey) throws SQLException;

    /**
     * Release a session advisory lock; return true if successful, false if not held
     */
    boolean releaseSessionLock(Connection conn, long lockKey) throws SQLException;

}
