package io.openleap.starter.core.lock.aspect;

import io.openleap.starter.core.lock.db.LockRepository;
import io.openleap.starter.core.lock.exception.ConcurrentExecutionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

    private static final String LOCK_KEY = "test-lock-key";

    @Mock
    private DataSource dataSource;

    @Mock
    private LockRepository lockRepository;

    @Mock
    private Connection connection;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private DistributedLock distributedLock;

    private DistributedLockAspect lockAspect;

    @BeforeEach
    void setup() {
        this.lockAspect = new DistributedLockAspect(dataSource, lockRepository);

        when(distributedLock.key()).thenReturn(LOCK_KEY);
    }

    @Test
    @DisplayName("intercept should proceed and unlock when lock is successfully acquired")
    void intercept_shouldProceedAndUnlock() throws Throwable {
        // when
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(true);
        when(pjp.proceed()).thenReturn("MethodResult");

        Object result = lockAspect.intercept(pjp, distributedLock);

        // then
        assertThat(result).isEqualTo("MethodResult");

        verify(lockRepository, times(1)).releaseSessionLock(connection, (long) LOCK_KEY.hashCode());
        verify(connection, times(1)).isClosed();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("intercept should throw ConcurrentExecutionException when lock fails and failOnConcurrentExecution is true")
    void intercept_shouldThrowExceptionOnLockFailure() throws Throwable {
        // when
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(false);
        when(distributedLock.failOnConcurrentExecution()).thenReturn(true);

        // then
        assertThatThrownBy(() -> lockAspect.intercept(pjp, distributedLock))
                .isInstanceOf(ConcurrentExecutionException.class);

        verify(pjp, never()).proceed();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("intercept should return null when lock fails and failOnConcurrentExecution is false")
    void intercept_shouldReturnNullOnLockFailure() throws Throwable {
        // when
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(false);
        when(distributedLock.failOnConcurrentExecution()).thenReturn(false);

        // then
        Object result = lockAspect.intercept(pjp, distributedLock);

        assertThat(result).isNull();
        verify(pjp, never()).proceed();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("intercept should ensure unlock is called even if method execution throws an exception")
    void intercept_shouldUnlockOnMethodException() throws Throwable {
        // when
        when(dataSource.getConnection()).thenReturn(connection);
        when(lockRepository.tryAcquireSessionLock(connection, (long) LOCK_KEY.hashCode())).thenReturn(true);
        when(pjp.proceed()).thenThrow(new RuntimeException("Business Logic Error"));

        // then
        assertThatThrownBy(() -> lockAspect.intercept(pjp, distributedLock))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Business Logic Error");

        verify(lockRepository, times(1)).releaseSessionLock(connection, (long) LOCK_KEY.hashCode());
        verify(connection, times(1)).isClosed();
        verify(connection, times(1)).close();
    }

}
