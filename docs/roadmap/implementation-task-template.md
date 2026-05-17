# Implementation Task Template

```text
Task ID:
Status:
Gate:
Requirement IDs:
ADR IDs:
Specification references:
Target module:
Allowed files:
Forbidden files:
Expected behavior:
Tests to add/update:
Documentation to update:
Commands to run:
Acceptance criteria:
Rollback notes:
```

## Rules

- Allowed files must be specific.
- Status must be one of `draft`, `ready-for-implementation`, `in-progress`,
  `complete`, `blocked`, or `human-gate-blocked`.
- Multiple roadmap tasks may be `ready-for-implementation` or `in-progress`
  when their file boundaries and dependencies do not conflict.
- At least one roadmap task must be `ready-for-implementation` or `in-progress`
  while open, non-human-gated roadmap work remains.
- Forbidden files must list product implementation areas when the task is infrastructure/design-only.
- Commands must be runnable locally.
- Acceptance criteria must be objective.
- Tasks must state coverage, Native Image, security, and documentation impacts when the change touches published modules, runtime behavior, protocol handling, generated code, build infrastructure, or agent governance.
