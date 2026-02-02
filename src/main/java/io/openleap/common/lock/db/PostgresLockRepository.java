package io.openleap.common.lock.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// TODO (itaseski): Add db integration tests?
public class PostgresLockRepository implements LockRepository {

    private static final String ACQUIRE_SESSION_LOCK_SQL = "SELECT pg_advisory_lock(?)";
    private static final String TRY_ACQUIRE_SESSION_LOCK_SQL = "SELECT pg_try_advisory_lock(?)";
    private static final String RELEASE_SESSION_LOCK_SQL = "SELECT pg_advisory_unlock(?)";

    public PostgresLockRepository() {
    }

    @Override
    public void acquireSessionLock(Connection conn, long lockKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ACQUIRE_SESSION_LOCK_SQL)) {
            ps.setLong(1, lockKey);
            // This will block the thread until the lock is available
            ps.execute();
        }
    }

    @Override
    public boolean tryAcquireSessionLock(Connection conn, long lockKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(TRY_ACQUIRE_SESSION_LOCK_SQL)) {
            ps.setLong(1, lockKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    @Override
    public boolean releaseSessionLock(Connection conn, long lockKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(RELEASE_SESSION_LOCK_SQL)) {
            ps.setLong(1, lockKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
}
