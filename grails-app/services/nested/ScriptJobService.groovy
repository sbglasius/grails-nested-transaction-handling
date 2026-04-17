package nested

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j


@Transactional
@Slf4j
class ScriptJobService {

    ScriptJobExecutionService scriptJobExecutionService

    void execute(Serializable id, boolean fail = false) {

        scriptJobExecutionService.markJobWithStarted(id)

        try {
          runScript(fail)
            scriptJobExecutionService.markJobWithCompleted(id)
        } catch (RuntimeException e) {
            log.warn("Execution failed: $e.message")
            scriptJobExecutionService.markJobWithFailed(id, e.message)
        }

    }

  void runScript(boolean fail) {
    if (fail) {
      throw new RuntimeException('Eeek')
    }
  }
}
