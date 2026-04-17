package nested

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j


@Transactional
@Slf4j
class ScriptJobService {

    ScriptJobExecutionService scriptJobExecutionService

    @Transactional(noRollbackFor = [ScriptJobExecutionService.ScriptJobExecutionException])
    void execute(Serializable id, boolean fail = false) {

        try {
          scriptJobExecutionService.markJobWithStarted(id)
        } catch (e) {
          log.warn("Failed to mark job with Started", e)
          // You probably don't want to continue from here, as the entire execute transaction is marked for roll-back
        }

        try {
          runScript(fail)
            scriptJobExecutionService.markJobWithCompleted(id)
        } catch (RuntimeException e) {
            log.warn("Execution failed: $e.message")
            scriptJobExecutionService.markJobWithFailed(id)
        }

    }

  void runScript(boolean fail) {
    if (fail) {
      throw new RuntimeException('Eeek')
    }
  }
}
