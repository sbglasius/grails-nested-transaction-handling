package nested

class ScriptJobExecution {
  ScriptJobExecutionStatus status
  Date submittedAt
  Date startedAt
  Date completedAt

  static constraints = {
    submittedAt nullable: true
    startedAt nullable: true
    completedAt nullable: true
  }
}
