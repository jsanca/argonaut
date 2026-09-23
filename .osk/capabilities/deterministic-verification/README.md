# Deterministic Verification Capability

The project, rather than OSK, declares its independently verifiable targets:

```yaml
# .osk/deterministic-verification.yaml
schema: osk.deterministic-verification/v0alpha2
targets:
  - id: app
    path: .
    buildCommand: <batch build command>
    testCommand: <batch test command>
```

The Capability executes those commands sequentially from each declared path, captures target-scoped bounded raw output and provenance, and reports `PASS`, `FAIL`, or `PARTIAL`. It does not infer a language, package manager, or test framework, and does not run coverage, mutation, CRAP, security, or dependency checks. The previous v0alpha1 single-target form remains a root-target compatibility path.
