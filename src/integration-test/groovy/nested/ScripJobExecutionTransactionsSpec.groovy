package nested

import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
class ScripJobExecutionTransactionsSpec extends Specification {

    ScriptJobExecutionService scriptJobExecutionService
    ScriptJobService scriptJobService

    def "ScriptJobExecution status is tracked through the job execution lifecycle"() {
        given: "a job execution persisted with SUBMITTED status"
        def id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date()
            ).save(failOnError: true).id
        }

        when: "the job is marked as started"
        scriptJobExecutionService.markJobWithStarted(id)

        then: "status is EXECUTING"
        ScriptJobExecution.withNewSession { ScriptJobExecution.get(id) }.status == ScriptJobExecutionStatus.EXECUTING

        when: "the job runs and completes or fails"
        try {
            scriptJobService.execute(fail)
            scriptJobExecutionService.markJobWithCompleted(id)
        } catch (RuntimeException ignored) {
            scriptJobExecutionService.markJobWithFailed(id)
        }

        then: "status reflects the outcome"
        ScriptJobExecution.withNewSession { ScriptJobExecution.get(id) }.status == expectedStatus

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }

        where:
        fail  | expectedStatus
        false | ScriptJobExecutionStatus.COMPLETED
        true  | ScriptJobExecutionStatus.FAILED
    }
}
