package nested

import grails.gorm.transactions.Transactional
import groovy.transform.InheritConstructors
import org.springframework.transaction.annotation.Propagation


@Transactional
class ScriptJobExecutionService {

  @InheritConstructors
  static class ScriptJobExecutionException extends RuntimeException {}
  
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void markJobWithStarted(Serializable id) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status = ScriptJobExecutionStatus.EXECUTING
      startedAt = new Date()
      save(failOnError: true)
      throw new ScriptJobExecutionException('Whaat')
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
  void markJobWithFailed(Serializable id) {
    def sje = ScriptJobExecution.get(id)
    sje?.with {
      status = ScriptJobExecutionStatus.FAILED
      completedAt = new Date()
      error = 'Some error'
      save(failOnError: true)
    }
  }
  
}
