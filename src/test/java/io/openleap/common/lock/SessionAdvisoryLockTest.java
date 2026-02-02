package io.openleap.common.lock;

import io.openleap.common.lock.db.LockRepository;
import io.openleap.common.lock.exception.LockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAdvisoryLockTest {

    private static final String LOCK_KEY = "test-lock";

    @Mock
    private DataSource dataSource;

    @Mock
    private LockRepository lockRepository;

    @Mock
    private Connection connection;

    private SessionAdvisoryLock sessionLock;

    @BeforeEach
    void setup() {
        this.sessionLock = new SessionAdvisoryLock(dataSource, LOCK_KEY, lockRepository);
    }

    @Test
    @DisplayName("should acquire connection and lock when lock is called")
    void shouldAcquireConnectionAndLockWhenLockCalled() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);

        // when
        sessionLock.lock();

        // then
        verify(lockRepository, times(1)).acquireSessionLock(connection, (long) LOCK_KEY.hashCode());
        verify(connection, never()).close();
    }

    @Test
    @DisplayName("should return true and hold connection when try lock is successful")
    void shouldReturnTrueAndHoldConnectionWhenTryLockSuccessful() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(true);

        // when
        boolean result = sessionLock.tryLock();

        // then
        assertThat(result).isTrue();

        verify(connection, never()).close();
    }

    @Test
    @DisplayName("should return false and close connection when try lock fails")
    void shouldReturnFalseAndCloseConnectionWhenTryLockFails() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(false);

        // when
        boolean result = sessionLock.tryLock();

        // then
        assertThat(result).isFalse();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("should throw lock exception and close connection when database error occurs during acquisition")
    void shouldThrowLockExceptionAndCloseConnectionWhenDbErrorOccurs() throws SQLException {
        // when
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenThrow(new SQLException("DB Error"));

        // then
        assertThatThrownBy(() -> sessionLock.tryLock())
                .isInstanceOf(LockException.class)
                .hasMessageContaining("Unexpected error during advisory lock acquisition");

        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("should release lock and close connection when unlock is called")
    void shouldReleaseLockAndCloseConnectionWhenUnlockCalled() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(true);

        sessionLock.tryLock();

        // when
        sessionLock.unlock();

        // then
        verify(lockRepository, times(1)).releaseSessionLock(connection, (long) LOCK_KEY.hashCode());
        verify(connection, times(1)).isClosed();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("should ensure connection is closed even if release lock throws exception")
    void shouldEnsureConnectionIsClosedEvenIfReleaseLockThrows() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(true);

        sessionLock.tryLock();

        doThrow(new SQLException("Network error")).when(lockRepository).releaseSessionLock(connection, (long) LOCK_KEY.hashCode());

        // when
        sessionLock.unlock();

        // then
        verify(connection, times(1)).isClosed();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("should do nothing when unlock is called without active lock")
    void shouldDoNothingWhenUnlockCalledWithoutActiveLock() {
        // when
        sessionLock.unlock();

        // then
        verifyNoInteractions(lockRepository);
        verifyNoInteractions(connection);
    }

    @Test
    @DisplayName("should throw unsupported operation exception when lock interruptibly is called")
    void shouldThrowUnsupportedOperationExceptionWhenLockInterruptiblyCalled() {
        assertThatThrownBy(() -> sessionLock.lockInterruptibly())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("lockInterruptibly not supported");
    }

    @Test
    @DisplayName("should throw unsupported operation exception when try lock with timeout is called")
    void shouldThrowUnsupportedOperationExceptionWhenTryLockWithTimeoutCalled() {
        assertThatThrownBy(() -> sessionLock.tryLock(10, TimeUnit.SECONDS))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("tryLock with timeout not supported");
    }

    @Test
    @DisplayName("should throw unsupported operation exception when new condition is called")
    void shouldThrowUnsupportedOperationExceptionWhenNewConditionCalled() {
        assertThatThrownBy(() -> sessionLock.newCondition())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Condition newCondition not supported");
    }
}
