package nested

import groovy.util.logging.Slf4j
import org.quartz.JobExecutionContext

@Slf4j
class ScriptJobRetryQuartzJob {

    ScriptJobService scriptJobService
    ScriptJobExecutionService scriptJobExecutionService

    // Poll every 30 s; first fire 10 s after startup
    static triggers = {
        simple name: 'retryTrigger', startDelay: 10_000, repeatInterval: 30_000
    }

    void execute(JobExecutionContext context) {
        List<Long> ids = scriptJobExecutionService.findPendingRetryIds()
        if (ids) {
            log.info("Retrying ${ids.size()} pending job execution(s): $ids")
        }
        ids.each { id -> scriptJobService.execute(id) }
    }
}
