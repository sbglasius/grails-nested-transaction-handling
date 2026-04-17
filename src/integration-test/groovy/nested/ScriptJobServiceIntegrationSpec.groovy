package nested

import grails.testing.mixin.integration.Integration
import spock.lang.Specification

/**
 * Demonstrates the nested transaction visibility bug:
 *
 * ScriptJobService.execute() is @Transactional, so the ScriptJobExecution INSERT
 * lives in an uncommitted outer transaction. The REQUIRES_NEW methods in
 * ScriptJobExecutionService open a fresh transaction and call get(id) — but
 * READ_COMMITTED isolation means they cannot see the outer transaction's uncommitted
 * row. get(id) returns null, the sje?.with{} block is a no-op, and the status
 * never advances beyond SUBMITTED.
 *
 * Both tests below will FAIL, proving the customer's issue.
 */
@Integration
class ScriptJobServiceIntegrationSpec extends Specification {

  ScriptJobService scriptJobService

  def "job execution lifecycle is tracked from submitted to completed"() {
    given:
      def sje = ScriptJobExecution.withNewTransaction {
        new ScriptJobExecution(
          status: ScriptJobExecutionStatus.SUBMITTED,
          submittedAt: new Date()
        ).save(failOnError: true)
      }

    when:
      scriptJobService.execute(sje.id, false)
    and:
      ScriptJobExecution.withNewTransaction { sje.refresh() }
    then:
      verifyAll(sje) {
        status == ScriptJobExecutionStatus.COMPLETED
        startedAt != null
        completedAt != null
        !error 
      }

    cleanup:
      ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(sje.id)?.delete(flush: true) }
  }

  def "job execution lifecycle is tracked from submitted to failed"() {
    given:
      def sje = ScriptJobExecution.withNewTransaction {
        new ScriptJobExecution(
          status: ScriptJobExecutionStatus.SUBMITTED,
          submittedAt: new Date()
        ).save(failOnError: true)
      }

    when:
      scriptJobService.execute(sje.id, true)
    and:
      ScriptJobExecution.withNewTransaction { sje.refresh() }

    then:
      verifyAll(sje) {
        status == ScriptJobExecutionStatus.PENDING_RETRY
        startedAt != null
        completedAt != null
        error == 'Eeek'
      }

    cleanup:
      ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(sje.id)?.delete(flush: true) }
  }
}
