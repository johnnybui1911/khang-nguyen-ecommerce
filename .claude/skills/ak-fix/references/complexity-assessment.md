# Complexity Assessment

Classify after scouting and diagnosis, so the route follows the confirmed cause, not the
wording of the request.

| Route | Indicators | Progress tracking |
|---|---|---|
| Simple (`workflow-quick.md`) | One file, clear error (type, syntax, lint), obvious fix location, root cause confirmed | None; under three steps |
| Moderate (`workflow-standard.md`) | 2-5 files, cause known but the fix spans files, or diagnosis still needs confirming | Six phases in the active plan, mirrored to the live surface when available |
| Complex (`workflow-deep.md`) | System-wide (5+ files), architecture decision, research needed, performance or security issue, cause spans layers | Nine phases with dependencies; scout, diagnose and research may run in parallel |
| Parallel (`--parallel`) | The flag was passed, or 2+ independent issues in different areas with no dependencies between them | One dependency tree per issue (scout, diagnose, fix, verify), one non-overlapping scope per agent |

`--parallel` activates parallel routing regardless of the classification.

## Optional shadow comparison

Complete the existing assessment first. Do not add a CLI/config lookup to the
default workflow. Only for a user-requested semantic experiment or trusted
user opt-in already present in context, inspect `ak eval decision --help` and
use consumer `fix-complexity` with `state: {summary: string, changed_files:
integer, root_cause_confirmed: boolean, architecture_decision: boolean,
security_sensitive: boolean}` and `legacy: {complexity:
simple|moderate|complex}`. `changed_files` is a count, never a list of
paths. Summarize non-sensitive facts; do not send source files, logs,
transcripts, or credentials. Run with `--validate-only` first; a refusal names
the failing field and its expected type.

This is shadow-only: follow the existing complexity workflow, preserve explicit
`--parallel`, and retain its result on disagreement, timeout, or unavailable
provider. The command independently checks user-scope consent. A comparison is
experimental evidence, not a measured improvement.
