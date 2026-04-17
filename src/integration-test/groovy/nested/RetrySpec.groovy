package nested

import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class RetrySpec extends Specification {

    @Autowired ScriptJobService scriptJobService
    @Autowired ScriptJobExecutionService scriptJobExecutionService

    def "job succeeds on first attempt with no retries"() {
        given:
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date(),
                maxRetries: 3
            ).save(failOnError: true).id
        }

        when:
        scriptJobService.execute(id, false)

        then:
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.status == ScriptJobExecutionStatus.COMPLETED
            assert sje.retryCount == 1
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }

    def "job retries and permanently fails after exhausting maxRetries"() {
        given: "maxRetries: 1 means 1 initial attempt + 1 retry = 2 total attempts (one 2 s back-off sleep)"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date(),
                maxRetries: 1
            ).save(failOnError: true).id
        }

        when:
        scriptJobService.execute(id, true)

        then:
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.status == ScriptJobExecutionStatus.FAILED
            assert sje.retryCount == 2       // attempt 1 + 1 retry
            assert sje.error == 'Eeek'
            assert sje.completedAt != null
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }

    def "retryCount reflects total attempts made"() {
        given: "maxRetries: 2 means 3 total attempts"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date(),
                maxRetries: 2
            ).save(failOnError: true).id
        }

        when:
        scriptJobService.execute(id, true)

        then:
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.status == ScriptJobExecutionStatus.FAILED
            assert sje.retryCount == 3
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }
}
