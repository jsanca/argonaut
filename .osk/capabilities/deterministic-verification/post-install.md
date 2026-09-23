Project adaptation required.

This capability requires project-owned verification configuration.

To finish setup, give the following task to your engineering agent:

------------------------------------------------------------

Configure the installed OSK deterministic-verification capability for this workspace.

Inspect the repository before making changes.

Identify the workspace's independently verifiable project targets, then determine each target's repository-relative working directory and canonical build/test procedures from authoritative workspace evidence. Consider, as applicable:

- OSK project knowledge
- README and project documentation
- existing verification and build scripts
- CI workflows
- build manifests
- task runners
- repository and module structure

Configure the project-owned multi-target binding:

`.osk/deterministic-verification.yaml`

Do not modify the canonical Capability implementation at:

`.osk/capabilities/deterministic-verification/verify.sh`

Do not select commands from filenames alone when stronger project evidence exists.

Do not guess material engineering decisions.

If required configuration cannot be established responsibly from workspace evidence, stop and escalate the unresolved decision to the engineer.

After configuration, execute deterministic-verification and confirm that it produces target-scoped valid evidence.

------------------------------------------------------------
