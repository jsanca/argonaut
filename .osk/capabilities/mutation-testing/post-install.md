# Configure Mutation Testing for this Workspace

Installing this Capability materialized its executable evidence mechanism. It did **not** select, install, configure, or execute a mutation provider.

An engineering agent must inspect project documentation, build/test configuration, CI, and existing reports; identify applicable targets; determine whether suitable mutation infrastructure already exists; then identify an appropriate provider and its installation/configuration needs. Do not select a provider merely from filenames, language markers, `pom.xml`, or `package.json`.

Keep provider states separate: identified, available, installed, configured, executable, and valid evidence produced are distinct claims. Prefer suitable project-owned infrastructure. If project configuration must change, determine whether that change is within the active task authority; otherwise stop and escalate.

Create `.osk/mutation-testing.yaml` with a repository-relative target, explicit execution scope, provider/tool identity, project-owned batch command, primary artifact, and supported artifact format. Then run the materialized entry point and inspect its evidence package.

Do not edit the canonical Capability to encode project choices. Do not store credentials or secrets in the binding or evidence. If provider choice, scope, evidence artifact, or authority is uncertain, retain that uncertainty and escalate.

```yaml
schema: osk.mutation-testing/v0alpha1
targets:
  - id: application
    path: .
    scope: <explicit full-or-bounded target scope>
    provider:
      id: project-selected-provider
      tool: project-tool
      version: project-managed
      command: <project-owned batch mutation command>
      primaryArtifact: <target-relative provider report>
      format: pitest-xml # or stryker-json
```
