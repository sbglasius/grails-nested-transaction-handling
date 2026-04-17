package nested

import org.quartz.JobExecutionContext

class ScriptJobQuartzJob {

    ScriptJobService scriptJobService

    static triggers = {}

    void execute(JobExecutionContext context) {
        def dataMap = context.mergedJobDataMap
        Long id = dataMap.getLong('scriptJobExecutionId')
        boolean fail = dataMap.getBoolean('fail')
        scriptJobService.execute(id, fail)
    }
}
