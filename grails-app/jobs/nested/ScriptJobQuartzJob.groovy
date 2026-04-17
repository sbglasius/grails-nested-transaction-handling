package nested

class ScriptJobQuartzJob {

    ScriptJobService scriptJobService

    static triggers = {}

    void execute() {
        scriptJobService.execute()
    }
}
