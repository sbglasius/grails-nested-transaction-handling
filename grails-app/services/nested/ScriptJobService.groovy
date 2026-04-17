package nested

import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j


@Transactional
@Slf4j
class ScriptJobService {

  static final int DEFAULT_MAX_RETRIES = 3
  static final long BASE_BACKOFF_MS = 2_000L
  static final long MAX_BACKOFF_MS = 30_000L

  ScriptJobExecutionService scriptJobExecutionService

  // No transaction held during back-off sleeps between retries.
  // All persistence is handled by the REQUIRES_NEW methods in ScriptJobExecutionService.
  // the runScript is also handling it's own transaction
  @NotTransactional
  void execute(Serializable id, boolean fail = false) {
    ScriptJobExecution sje = scriptJobExecutionService.getScriptJobExecution(id)
    int maxRetries = sje != null ? sje.maxRetries : DEFAULT_MAX_RETRIES

    RuntimeException lastError = null
    for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
      if (attempt > 1) {
        long delay = Math.min(BASE_BACKOFF_MS * (1L << (attempt - 2)), MAX_BACKOFF_MS)
        log.info("ScriptJobExecution(${id}): Retry attempt ${attempt}/${maxRetries + 1} after ${delay} ms back-off")
        Thread.sleep(delay)
      }

      scriptJobExecutionService.markJobWithStarted(id, attempt)
      try {
        runScript(fail)
        scriptJobExecutionService.markJobWithCompleted(id)
        return
      } catch (RuntimeException e) {
        lastError = e
        log.warn("Attempt ${attempt}/${maxRetries + 1} failed: ${e.message}")
      }
    }

    scriptJobExecutionService.markJobWithFailed(id, lastError?.message ?: 'Unknown error')
  }

  void runScript(boolean fail) {
    if (fail) {
      throw new RuntimeException('Eeek')
    }
  }
}
