package nested

import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation


@Transactional
class ScriptJobExecutionService {

  @Transactional(readOnly = true)
  ScriptJobExecution getScriptJobExecution(Serializable id) {
    ScriptJobExecution.get(id)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithStarted(Serializable id, int attempt) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status = ScriptJobExecutionStatus.EXECUTING
      startedAt = new Date()
      retryCount = attempt
      save(failOnError: true)
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithCompleted(Serializable id) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status = ScriptJobExecutionStatus.COMPLETED
      completedAt = new Date()
      save(failOnError: true)
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithFailed(Serializable id, String errorMessage) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status = ScriptJobExecutionStatus.FAILED
      completedAt = new Date()
      error = errorMessage
      save(failOnError: true)
    }
  }

}
