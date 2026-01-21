package io.openleap.starter.core.lock;

import io.openleap.starter.core.lock.db.LockRepository;
import io.openleap.starter.core.lock.exception.LockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * WARNING: Not compatible with PgBouncer 'transaction' pooling.
 * This lock requires a stable DB session. Transaction-level poolers may
 * swap the backend connection mid-execution, losing or orphaning the lock.
 * * REQUIREMENT: Use a DataSource connecting directly to Postgres (5432)
 * or a pool with 'pool_mode = session'.
 */
public class SessionAdvisoryLock implements Lock {

    private static final Logger log = LoggerFactory.getLogger(SessionAdvisoryLock.class);

    private final DataSource dataSource;
    private final long lockId;
    private final LockRepository lockRepository;
    // pinned connection for holding the session lock
    private Connection connection;

    public SessionAdvisoryLock(DataSource dataSource, String lockKey, LockRepository lockRepository) {
        this.dataSource = dataSource;
        // TODO (itaseski): Better hash function to avoid collisions
        this.lockId = lockKey.hashCode();
        this.lockRepository = lockRepository;
    }

    @Override
    public void lock() {
        if (this.connection != null) {
            return;
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            // This call blocks on the database side until the lock is acquired
            lockRepository.acquireSessionLock(conn, lockId);
            this.connection = conn;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {

                }
            }
            throw new LockException("Unexpected error during blocking advisory lock acquisition", e);
        }
    }

    @Override
    public void lockInterruptibly() {
        // TODO (itaseski): Implement if/when needed
        throw new UnsupportedOperationException("lockInterruptibly not supported");
    }

    @Override
    public boolean tryLock() {
        if (this.connection != null) {
            return true;
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            // try to acquire session level advisory lock
            if (lockRepository.tryAcquireSessionLock(conn, lockId)) {
                this.connection = conn;
                return true;
            } else {
                conn.close();
                return false;
            }
        } catch (Exception e) {
            // ensure we don't leak the connection if the DB query fails
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {

                }
            }
            throw new LockException("Unexpected error during advisory lock acquisition", e);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        // TODO (itaseski): Implement if/when needed
        throw new UnsupportedOperationException("tryLock with timeout not supported");
    }

    @Override
    public void unlock() {
        if (this.connection == null) {
            return;
        }

        try {

            boolean released = lockRepository.releaseSessionLock(this.connection, lockId);

            if (!released) {
                log.warn("Potential lock inconsistency: pg_advisory_unlock returned false for lockId [{}]. " +
                        "The session might have been reset or the lock was already released.", lockId);
            }
        } catch (Exception e) {
            // Swallow exception to prevent release-stage errors from masking successful business logic.
            // Connection cleanup handles the safety fallback.
            log.error("Failed to send unlock command to Postgres for lockId [{}]. " +
                    "The lock will be released automatically when the connection is closed.", lockId, e);
        } finally {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (SQLException sqle) {
                log.debug("Error closing connection during unlock cleanup", sqle);
            } finally {
                this.connection = null;
            }
        }
    }

    @Override
    public Condition newCondition() {
        // TODO (itaseski): Implement if/when needed
        throw new UnsupportedOperationException("Condition newCondition not supported");
    }
}
