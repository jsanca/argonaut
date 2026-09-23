# Configure Coverage Measurement for this Workspace

Installing this OSK Capability materialized a repository-owned entry point. It did **not** select, install, configure, or execute a coverage provider for this project.

Before running it, an engineering agent must:

1. Inspect repository documentation, build files, CI configuration, and existing reports for project-owned coverage infrastructure.
2. Identify the applicable target or targets; do not infer a provider merely from filenames or language markers.
3. Establish the provider state separately: identified, installed, configured, and successfully executed are different claims.
4. Prefer an existing suitable project mechanism. If a project change is needed, determine whether that change is authorized by the current task; otherwise stop and escalate.
5. Create `.osk/coverage-measurement.yaml` with the selected target, provider/tool identity, command, primary provider artifact, and supported artifact format.
6. Execute the materialized entry point and inspect its retained evidence package.

Do not edit this canonical capability to encode project choices. Do not store credentials or secrets in the project binding. If provider selection, project authority, or the primary artifact is uncertain, preserve the uncertainty and escalate rather than guessing.

Example project-owned binding shape:

```yaml
schema: osk.coverage-measurement/v0alpha1
targets:
  - id: application
    path: .
    provider:
      id: project-selected-provider
      tool: project-tool
      version: project-managed
      command: <project-owned batch coverage command>
      primaryArtifact: <repository-relative provider artifact>
      format: jacoco-xml # or istanbul-json-summary
```

Run without the OSK CLI after configuration:

```sh
.osk/capabilities/coverage-measurement/measure.sh --task COVERAGE-001 --objective 'Capture coverage evidence' --evidence-dir docs/engineering/agents/reviews
```
