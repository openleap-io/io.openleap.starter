package io.openleap.starter.core.lock.aspect;

import io.openleap.starter.core.lock.SessionAdvisoryLock;
import io.openleap.starter.core.lock.db.LockRepository;
import io.openleap.starter.core.lock.exception.ConcurrentExecutionException;
import io.openleap.starter.core.lock.exception.LockError;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import javax.sql.DataSource;

// TODO (itaseski): Add Spring integration tests to verify aspect and locking
/*
 * WARNING: Avoid using @DistributedLock on the same method as @Transactional.
 * * By default, the order of aspect execution may cause the lock to be released
 * BEFORE the transaction is committed. This creates a race condition where
 * another process could acquire the lock while the current database changes
 * are still uncommitted (invisible to others).
 * * Recommended Pattern: Wrap the transactional logic in a service method
 * and call it from a non-transactional method annotated with @DistributedLock,
 * or use TransactionTemplate inside the locked block to ensure the commit
 * happens before the lock release.
 */
@Aspect
public class DistributedLockAspect {

    private final DataSource dataSource;
    private final LockRepository lockRepository;

    public DistributedLockAspect(DataSource dataSource, LockRepository lockRepository) {
        this.dataSource = dataSource;
        this.lockRepository = lockRepository;
    }

    // TODO (itaseski): Support key expressions
    @Around(value = "@annotation(distributedLock)", argNames = "pjp,distributedLock")
    public Object intercept(ProceedingJoinPoint pjp, DistributedLock distributedLock) {
        String lockKey = distributedLock.key();

        // Local instantiation ensures the lock is bound to this specific execution's
        // database session, preventing cross-thread state leakage.
        SessionAdvisoryLock lock = new SessionAdvisoryLock(dataSource, lockKey, lockRepository);

        if (lock.tryLock()) {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                // ensure the lock is released even if the method throws an Exception
                lock.unlock();
            }
        } else {
            if (distributedLock.failOnConcurrentExecution()) {
                throw new ConcurrentExecutionException(LockError.PROCESS_WITH_LOCK_IS_ALREADY_RUNNING);
            }

            return null;
        }
    }

}
