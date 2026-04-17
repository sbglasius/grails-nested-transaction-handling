package nested

class ScriptJobExecution {
  ScriptJobExecutionStatus status
  Date submittedAt
  Date startedAt
  Date completedAt
  String error
  int retryCount = 0
  int maxRetries = 3
  Date nextRetryAt

  static constraints = {
    submittedAt nullable: true
    startedAt nullable: true
    completedAt nullable: true
    error nullable: true
    nextRetryAt nullable: true
  }
}
