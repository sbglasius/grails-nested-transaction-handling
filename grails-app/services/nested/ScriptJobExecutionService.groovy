package nested

import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation


@Transactional
class ScriptJobExecutionService {

  // 2 s base, doubles each attempt, capped at 30 s — short for sandbox convenience
  private static final long BASE_BACKOFF_MS = 2_000L
  private static final long MAX_BACKOFF_MS  = 30_000L

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithStarted(Serializable id) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status      = ScriptJobExecutionStatus.EXECUTING
      startedAt   = new Date()
      completedAt = null
      nextRetryAt = null
      save(failOnError: true)
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithCompleted(Serializable id) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status      = ScriptJobExecutionStatus.COMPLETED
      completedAt = new Date()
      save(failOnError: true)
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithFailed(Serializable id, String errorMessage = 'Unknown error') {
    ScriptJobExecution sje = ScriptJobExecution.get(id)
    if (!sje) return
    sje.retryCount++
    sje.error       = errorMessage
    sje.completedAt = new Date()
    if (sje.retryCount <= sje.maxRetries) {
      sje.status      = ScriptJobExecutionStatus.PENDING_RETRY
      sje.nextRetryAt = new Date(System.currentTimeMillis() + backoffMillis(sje.retryCount))
    } else {
      sje.status = ScriptJobExecutionStatus.FAILED
    }
    sje.save(failOnError: true)
  }

  @Transactional(readOnly = true)
  List<Long> findPendingRetryIds() {
    ScriptJobExecution.findAllByStatusAndNextRetryAtLessThanEquals(
        ScriptJobExecutionStatus.PENDING_RETRY, new Date()
    )*.id
  }

  private static long backoffMillis(int retryCount) {
    Math.min(BASE_BACKOFF_MS * (1L << (retryCount - 1)), MAX_BACKOFF_MS)
  }

}
