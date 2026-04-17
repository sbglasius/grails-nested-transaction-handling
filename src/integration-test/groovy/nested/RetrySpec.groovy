package nested

import grails.testing.mixin.integration.Integration
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class RetrySpec extends Specification {

    @Autowired ScriptJobService scriptJobService
    @Autowired ScriptJobExecutionService scriptJobExecutionService
    @Autowired Scheduler quartzScheduler

    JobKey retryJobKey

    def setup() {
        retryJobKey = quartzScheduler
            .getJobKeys(GroupMatcher.anyGroup())
            .find { it.name.contains('ScriptJobRetryQuartzJob') }
        assert retryJobKey : "ScriptJobRetryQuartzJob was not registered in the Quartz scheduler"
    }

    def "failed job is scheduled for retry with exponential back-off"() {
        given: "a committed execution allowing up to 3 retries"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date(),
                maxRetries: 3
            ).save(failOnError: true).id
        }

        when: "the job runs and fails"
        scriptJobService.execute(id, true)

        then: "execution moves to PENDING_RETRY with back-off and error recorded"
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.status == ScriptJobExecutionStatus.PENDING_RETRY
            assert sje.retryCount == 1
            assert sje.nextRetryAt != null
            assert sje.nextRetryAt > new Date()
            assert sje.error == 'Eeek'
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }

    def "back-off doubles with each retry attempt"() {
        given: "an execution already at retryCount 1 (i.e. first retry just failed)"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.EXECUTING,
                submittedAt: new Date(),
                startedAt: new Date(),
                maxRetries: 3,
                retryCount: 1
            ).save(failOnError: true).id
        }

        when: "it fails again (retryCount will become 2)"
        long before = System.currentTimeMillis()
        scriptJobExecutionService.markJobWithFailed(id, 'second failure')

        then: "nextRetryAt is ~4 s in the future (2^1 × 2 s)"
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.retryCount == 2
            assert sje.status == ScriptJobExecutionStatus.PENDING_RETRY
            long expectedDelay = 4_000L
            long actualDelay = sje.nextRetryAt.time - before
            assert actualDelay >= expectedDelay - 200 && actualDelay <= expectedDelay + 1000
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }

    def "job is permanently failed after exhausting all retries"() {
        given: "an execution already at its last allowed retry"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.EXECUTING,
                submittedAt: new Date(),
                startedAt: new Date(),
                maxRetries: 2,
                retryCount: 2
            ).save(failOnError: true).id
        }

        when: "the job fails one more time"
        scriptJobExecutionService.markJobWithFailed(id, 'final failure')

        then: "status is FAILED with no further retry scheduled"
        ScriptJobExecution.withNewTransaction {
            ScriptJobExecution sje = ScriptJobExecution.get(id)
            assert sje.status == ScriptJobExecutionStatus.FAILED
            assert sje.retryCount == 3
            assert sje.nextRetryAt == null
            assert sje.error == 'final failure'
            true
        }

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }

    def "retry Quartz job re-executes overdue pending retries"() {
        given: "an execution in PENDING_RETRY with nextRetryAt in the past"
        Long id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.PENDING_RETRY,
                submittedAt: new Date(),
                startedAt: new Date(),
                maxRetries: 3,
                retryCount: 1,
                nextRetryAt: new Date(System.currentTimeMillis() - 1000)
            ).save(failOnError: true).id
        }

        when: "the retry Quartz job fires"
        quartzScheduler.triggerJob(retryJobKey)

        // Poll until execution completes (up to 5 s)
        def deadline = System.currentTimeMillis() + 5000
        ScriptJobExecution sje = null
        while (System.currentTimeMillis() < deadline) {
            sje = ScriptJobExecution.withNewSession { ScriptJobExecution.get(id) }
            if (sje?.status in [ScriptJobExecutionStatus.COMPLETED, ScriptJobExecutionStatus.FAILED]) break
            Thread.sleep(100)
        }

        then: "execution completes successfully on retry"
        sje.status == ScriptJobExecutionStatus.COMPLETED

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }
    }
}
