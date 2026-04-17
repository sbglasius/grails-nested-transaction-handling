package nested

import groovy.transform.CompileStatic

/**
 * Defines the execution status values for ScriptJobExecution
 */
@CompileStatic
enum ScriptJobExecutionStatus {
 /**Job has been submitted but not yet started executing*/
 SUBMITTED,
 /**Job is currently executing*/
 EXECUTING,
 /**Job completed successfully*/
 COMPLETED,
 /**Job failed during execution*/
 FAILED
}

