# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A small Grails 6.2.3 sandbox project for exploring **nested transaction behavior** using Spring's `Propagation.REQUIRES_NEW`. The domain models (`ScriptJob`, `ScriptJobExecution`) and services (`ScriptJobService`, `ScriptJobExecutionService`) are the focus—the rest is standard Grails scaffolding.

## Architecture

### Transaction Pattern Under Test

`ScriptJobService` runs with the default `@Transactional` propagation. `ScriptJobExecutionService` methods are annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)` so that status updates (`markJobWithStarted`, `markJobWithCompleted`, `markJobWithFailed`) are committed to their own independent transactions—even if the outer job transaction rolls back.

```
ScriptJobService.execute()       ← outer transaction
  └─ ScriptJobExecutionService   ← REQUIRES_NEW (independent commits)
       ├─ markJobWithStarted()
       ├─ markJobWithCompleted()
       └─ markJobWithFailed()
```

### Key Files

| File | Purpose |
|---|---|
| `grails-app/domain/nested/ScriptJobExecution.groovy` | Tracks execution lifecycle (status, timestamps) |
| `grails-app/services/nested/ScriptJobExecutionService.groovy` | REQUIRES_NEW transaction methods |
| `grails-app/services/nested/ScriptJobService.groovy` | Outer `@Transactional` service; accepts `fail` flag to trigger rollback |
| `src/main/groovy/nested/ScriptJobExecutionStatus.groovy` | Enum: SUBMITTED, EXECUTING, COMPLETED, FAILED |
| `src/integration-test/groovy/nested/TransactionsSpec.groovy` | Integration spec for verifying transaction isolation |

### Database

H2 in-memory (dev/test) and file-based (production) — no external DB required. Schema is auto-managed by Hibernate.

## Common Commands

```bash
# Run the application
./gradlew bootRun

# Run all unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "nested.ScriptJobServiceSpec"

# Run integration tests
./gradlew integrationTest

# Run a single integration test class
./gradlew integrationTest --tests "nested.TransactionsSpec"

# Build
./gradlew build
```

## Test Structure

- **Unit tests** (`src/test/groovy/nested/`): Spock specs using `DomainUnitTest` and `ServiceUnitTest` mixins. Currently stubs awaiting implementation.
- **Integration tests** (`src/integration-test/groovy/nested/`): `TransactionsSpec` verifies that `REQUIRES_NEW` commits survive outer transaction rollbacks. Uses `Specification` (not `GebSpec`) — no browser required.