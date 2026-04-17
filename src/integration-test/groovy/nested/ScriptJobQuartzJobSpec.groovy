package nested

import grails.testing.mixin.integration.Integration
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class ScriptJobQuartzJobSpec extends Specification {

    @Autowired
    Scheduler quartzScheduler

    // grails-quartz registers the job artefact on startup; find whatever key it chose.
    JobKey registeredJobKey

    def setup() {
        registeredJobKey = quartzScheduler
            .getJobKeys(GroupMatcher.anyGroup())
            .find { it.name.contains('ScriptJobQuartzJob') }
        assert registeredJobKey : "ScriptJobQuartzJob was not registered in the Quartz scheduler"
    }

    def "quartz job drives the full execution lifecycle"() {
        given: "a committed ScriptJobExecution"
        def id = ScriptJobExecution.withNewTransaction {
            new ScriptJobExecution(
                status: ScriptJobExecutionStatus.SUBMITTED,
                submittedAt: new Date()
            ).save(failOnError: true).id
        }

        when: "the quartz job is triggered with the execution id"
        quartzScheduler.triggerJob(
            registeredJobKey,
            new JobDataMap([scriptJobExecutionId: id, fail: fail])
        )

        // Poll until the job completes or 5 seconds elapse
        def deadline = System.currentTimeMillis() + 5000
        ScriptJobExecution sje = null
        while (System.currentTimeMillis() < deadline) {
            sje = ScriptJobExecution.withNewSession { ScriptJobExecution.get(id) }
            if (sje?.status in [ScriptJobExecutionStatus.COMPLETED, ScriptJobExecutionStatus.FAILED]) break
            Thread.sleep(100)
        }

        then:
        sje.status == expectedStatus
        sje.startedAt != null
        sje.completedAt != null

        cleanup:
        ScriptJobExecution.withNewTransaction { ScriptJobExecution.get(id)?.delete(flush: true) }

        where:
        fail  | expectedStatus
        false | ScriptJobExecutionStatus.COMPLETED
        true  | ScriptJobExecutionStatus.FAILED
    }
}
