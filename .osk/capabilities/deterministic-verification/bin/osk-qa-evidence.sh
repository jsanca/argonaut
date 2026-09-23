#!/usr/bin/env sh
# Repository-owned deterministic evidence runner. It is intentionally limited
# to the existing build/test battery and runs without the OSK CLI.
set -u

usage() {
  cat <<'EOF'
Usage:
  verify.sh --task <task-id> --objective <text> [options]

Options:
  --scope <FULL|PARTIAL|TARGETED|CHANGED-CODE>  Evidence scope (default: PARTIAL)
  --targets <paths-or-modules>                    Scope detail (default: project build and test commands)
  --checks <build,test>                           Comma-separated configured checks (default: build,test)
  --executor <identity>                           Executor label (default: unavailable)
  --evidence-dir <directory>                      Evidence root (default: docs/engineering/agents/reviews)
  --help                                          Show this help

Configured checks are intentionally limited to project build and test commands. Coverage,
mutation, CRAP, static analysis, dependency analysis, and security scanning are
recorded as not executed rather than represented as passing.

Project configuration:
  .osk/deterministic-verification.yaml

  schema: osk.deterministic-verification/v0alpha2
  targets:
    - id: app
      path: .
      buildCommand: <project-owned batch build command>
      testCommand: <project-owned batch test command>
EOF
}

task_id=''
objective=''
scope='PARTIAL'
targets='project-declared verification targets'
checks='build,test'
executor='unavailable'
evidence_dir=''

while [ "$#" -gt 0 ]; do
  case "$1" in
    --task) task_id=${2-}; shift 2 ;;
    --objective) objective=${2-}; shift 2 ;;
    --scope) scope=${2-}; shift 2 ;;
    --targets) targets=${2-}; shift 2 ;;
    --checks) checks=${2-}; shift 2 ;;
    --executor) executor=${2-}; shift 2 ;;
    --evidence-dir) evidence_dir=${2-}; shift 2 ;;
    --help) usage; exit 0 ;;
    *) echo "error: unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [ -z "$task_id" ] || [ -z "$objective" ]; then
  echo 'error: --task and --objective are required' >&2
  usage >&2
  exit 2
fi
case "$task_id" in *[!A-Za-z0-9._-]*|'') echo 'error: task id may contain only letters, numbers, dot, underscore, and hyphen' >&2; exit 2 ;; esac
case "$scope" in FULL|PARTIAL|TARGETED|CHANGED-CODE) ;; *) echo 'error: --scope must be FULL, PARTIAL, TARGETED, or CHANGED-CODE' >&2; exit 2 ;; esac
case ",$checks," in ,build,|,test,|,build,test,|,test,build,) ;; *) echo 'error: --checks accepts build, test, build,test, or test,build' >&2; exit 2 ;; esac

root=${OSK_CAPABILITY_WORKSPACE_ROOT:-}
if [ -z "$root" ]; then
  root=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
fi
config="$root/.osk/deterministic-verification.yaml"
if [ ! -f "$config" ]; then
  echo "error: deterministic-verification requires project configuration at $config" >&2
  exit 2
fi
config_value() {
  key=$1
  sed -n "s/^$key:[[:space:]]*//p" "$config" | head -n 1 | sed 's/^"//; s/"$//'
}
schema=$(config_value schema)
targets_file=$(mktemp "${TMPDIR:-/tmp}/osk-targets.XXXXXX")
target_value() { printf '%s' "$1" | sed 's/^[[:space:]]*//; s/^"//; s/"$//'; }
write_target() {
  if [ -z "$target_id" ] || [ -z "$target_path" ] || { [ -z "$target_build" ] && [ -z "$target_test" ]; }; then
    echo "error: each v0alpha2 target requires id, path, and at least one command" >&2; rm -f "$targets_file"; exit 2
  fi
  case "$target_id" in *[!A-Za-z0-9._-]*|'') echo "error: target id is invalid: $target_id" >&2; rm -f "$targets_file"; exit 2 ;; esac
  case "$target_path" in /*|*'..'*) echo "error: target path must be repository-relative: $target_path" >&2; rm -f "$targets_file"; exit 2 ;; esac
  printf '%s\t%s\t%s\t%s\n' "$target_id" "$target_path" "$target_build" "$target_test" >> "$targets_file"
}
case "$schema" in
  osk.deterministic-verification/v0alpha1)
    build_command=$(config_value buildCommand); test_command=$(config_value testCommand)
    if [ -z "$build_command" ] || [ -z "$test_command" ]; then echo "error: $config v0alpha1 requires buildCommand and testCommand" >&2; rm -f "$targets_file"; exit 2; fi
    printf 'workspace\t.\t%s\t%s\n' "$build_command" "$test_command" > "$targets_file"
    ;;
  osk.deterministic-verification/v0alpha2)
    target_id=''; target_path=''; target_build=''; target_test=''; seen_target=false
    while IFS= read -r line || [ -n "$line" ]; do
      case "$line" in
        '  - id:'*) if [ "$seen_target" = true ]; then write_target; fi; target_id=$(target_value "${line#'  - id:'}"); target_path=''; target_build=''; target_test=''; seen_target=true ;;
        '    path:'*) target_path=$(target_value "${line#'    path:'}") ;;
        '    buildCommand:'*) target_build=$(target_value "${line#'    buildCommand:'}") ;;
        '    testCommand:'*) target_test=$(target_value "${line#'    testCommand:'}") ;;
      esac
    done < "$config"
    if [ "$seen_target" = true ]; then write_target; else echo "error: $config v0alpha2 requires at least one target" >&2; rm -f "$targets_file"; exit 2; fi
    if [ "$(cut -f1 "$targets_file" | sort | uniq -d | wc -l | tr -d ' ')" != 0 ]; then echo "error: target ids must be unique" >&2; rm -f "$targets_file"; exit 2; fi
    ;;
  *) echo "error: $config has unsupported schema $schema" >&2; rm -f "$targets_file"; exit 2 ;;
esac
if [ -z "$evidence_dir" ]; then evidence_dir="$root/docs/engineering/agents/reviews";
elif [ "${evidence_dir#/*}" = "$evidence_dir" ]; then evidence_dir="$root/$evidence_dir"; fi
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
executed_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
package_dir="$evidence_dir/$task_id/qa-evidence/$timestamp"
raw_dir="$package_dir/raw"
summary="$package_dir/deterministic-verification.md"
mkdir -p "$raw_dir"

escape_sed() { printf '%s' "$1" | sed 's/[\\/&]/\\&/g'; }
workspace_pattern=$(escape_sed "$root")
home_pattern=$(escape_sed "${HOME:-}")
normalize_evidence() {
  source_file=$1
  destination_file=$2
  sed \
    -e "s|$workspace_pattern|<WORKSPACE>|g" \
    -e "s|$home_pattern|<HOME>|g" \
    -e 's|/private/tmp/|<TEMP>/|g' \
    -e 's|/tmp/|<TEMP>/|g' \
    -e 's|/var/folders/[^/]*/[^/]*/T/|<TEMP>/|g' \
    -e 's|/home/runner/work/|<TEMP>/|g' \
    -e 's|/github/workspace/|<TEMP>/|g' \
    "$source_file" > "$destination_file"
}
capture_provenance() {
  destination=$1
  shift
  temporary=$(mktemp "${TMPDIR:-/tmp}/osk-redaction.XXXXXX")
  "$@" > "$temporary" 2>&1 || true
  normalize_evidence "$temporary" "$destination"
  rm -f "$temporary"
}

revision=$(git -C "$root" rev-parse HEAD 2>/dev/null || printf '%s' 'unavailable')
branch=$(git -C "$root" branch --show-current 2>/dev/null || printf '%s' 'unavailable')
capture_provenance "$raw_dir/working-tree-status.txt" git -C "$root" status --short
capture_provenance "$raw_dir/working-tree-unstaged.diff" git -C "$root" diff --binary
capture_provenance "$raw_dir/working-tree-staged.diff" git -C "$root" diff --cached --binary
capture_provenance "$raw_dir/untracked-files.txt" git -C "$root" ls-files --others --exclude-standard

has_check() { case ",$checks," in *,"$1",*) return 0 ;; *) return 1 ;; esac; }
run_check() {
  target_id=$1; target_path=$2; check_name=$3; command=$4; log="$raw_dir/$target_id/$check_name.log"
  temporary=$(mktemp "${TMPDIR:-/tmp}/osk-redaction.XXXXXX")
  if sh -c "cd \"$root/$target_path\" && $command" > "$temporary" 2>&1; then result='PASS'; else result='FAIL'; fi
  normalize_evidence "$temporary" "$log"
  rm -f "$temporary"
  printf '%s' "$result" > "$raw_dir/$target_id/$check_name.result"
  printf '%s\t%s\t%s\t%s\t%s\n' "$target_id" "$target_path" "$check_name" "$command" "$result" >> "$results_file"
}
results_file=$(mktemp "${TMPDIR:-/tmp}/osk-results.XXXXXX")
tab=$(printf '\t')
while IFS="$tab" read -r target_id target_path target_build target_test; do
  if [ ! -d "$root/$target_path" ]; then echo "error: target path does not exist: $target_path" >&2; rm -f "$targets_file" "$results_file"; exit 2; fi
  mkdir -p "$raw_dir/$target_id"
  if has_check build && [ -n "$target_build" ]; then run_check "$target_id" "$target_path" build "$target_build"; elif [ -z "$target_build" ]; then printf '%s' 'NOT APPLICABLE' > "$raw_dir/$target_id/build.result"; printf '%s\t%s\tbuild\t%s\tNOT APPLICABLE\n' "$target_id" "$target_path" "$target_build" >> "$results_file"; else printf '%s' 'NOT EXECUTED' > "$raw_dir/$target_id/build.result"; printf '%s\t%s\tbuild\t%s\tNOT EXECUTED\n' "$target_id" "$target_path" "$target_build" >> "$results_file"; fi
  if has_check test && [ -n "$target_test" ]; then run_check "$target_id" "$target_path" test "$target_test"; elif [ -z "$target_test" ]; then printf '%s' 'NOT APPLICABLE' > "$raw_dir/$target_id/test.result"; printf '%s\t%s\ttest\t%s\tNOT APPLICABLE\n' "$target_id" "$target_path" "$target_test" >> "$results_file"; else printf '%s' 'NOT EXECUTED' > "$raw_dir/$target_id/test.result"; printf '%s\t%s\ttest\t%s\tNOT EXECUTED\n' "$target_id" "$target_path" "$target_test" >> "$results_file"; fi
done < "$targets_file"
if grep -q "${tab}FAIL$" "$results_file"; then overall='FAIL'; else overall='PARTIAL'; fi

digest() { if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'; elif command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'; else printf '%s' 'unavailable'; fi; }
result_rows=$(mktemp "${TMPDIR:-/tmp}/osk-result-rows.XXXXXX")
while IFS="$tab" read -r target_id target_path check_name command result; do
  log="$raw_dir/$target_id/$check_name.log"
  evidence='—'
  if [ -f "$log" ]; then evidence=$(printf '[log](raw/%s/%s.log) (SHA-256 `%s`)' "$target_id" "$check_name" "$(digest "$log")"); fi
  printf '| `%s` | `%s` | %s | %s | `%s` | %s |\n' "$target_id" "$target_path" "$check_name" "$result" "$command" "$evidence" >> "$result_rows"
done < "$results_file"
status_digest=$(digest "$raw_dir/working-tree-status.txt")
unstaged_digest=$(digest "$raw_dir/working-tree-unstaged.diff")
staged_digest=$(digest "$raw_dir/working-tree-staged.diff")
untracked_digest=$(digest "$raw_dir/untracked-files.txt")
dirty=false; if [ -s "$raw_dir/working-tree-status.txt" ]; then dirty=true; fi

summary_temporary=$(mktemp "${TMPDIR:-/tmp}/osk-redaction.XXXXXX")
cat > "$summary_temporary" <<EOF
# Deterministic QA Evidence — $task_id

> Execution evidence, not a task/gate/product-acceptance decision. Consumers must assess scope, freshness, and limitations before relying on it.

## Task association

- Task: \`$task_id\`
- Objective: $objective
- Execution scope: \`$scope\`
- Targets/modules: $targets
- Executor: $executor
- Workspace: \`<WORKSPACE>\`

## Execution provenance

- Executed (UTC): $executed_at
- Branch: \`$branch\`
- Revision: \`$revision\`
- Capability configuration: \`.osk/deterministic-verification.yaml\`
- Configuration schema: \`$schema\`
- Evidence redaction/normalization: workspace, current home, and known temporary/runner paths were normalized before persistence.
- Working-tree status: [raw snapshot](raw/working-tree-status.txt) (SHA-256 \`$status_digest\`)
- Dirty working tree: \`$dirty\`
- Unstaged tracked delta: [binary-safe diff](raw/working-tree-unstaged.diff) (SHA-256 \`$unstaged_digest\`)
- Staged tracked delta: [binary-safe diff](raw/working-tree-staged.diff) (SHA-256 \`$staged_digest\`)
- Untracked-file manifest: [paths](raw/untracked-files.txt) (SHA-256 \`$untracked_digest\`)

## Results

Overall execution result: **$overall**.

| Target | Working directory | Check | Result | Project-owned command | Raw evidence |
| --- | --- | --- | --- | --- | --- |
$(cat "$result_rows")
| Coverage | POLICY THRESHOLD | no | NOT EXECUTED | No repository coverage command/policy was selected. | — |
| Mutation | EVIDENCE SIGNAL | no | NOT EXECUTED | No Go mutation tool/configuration was selected. | — |
| Change risk / CRAP | EVIDENCE SIGNAL | no | NOT EXECUTED | No Go CRAP analyzer/configuration was selected. | — |
| Static analysis | POLICY THRESHOLD | no | NOT EXECUTED | No additional analyzer/policy was selected. | — |
| Dependency / structural checks | POLICY THRESHOLD | no | NOT EXECUTED | Not selected for this execution. | — |
| Security | EVIDENCE SIGNAL | no | NOT EXECUTED | No scanner or security policy was selected. | — |

## What this evidence supports

The executed deterministic checks produced the recorded result against the revision and working-tree snapshot above. A passing build/test check establishes only that its recorded command exited successfully in this execution. It does not establish architecture correctness, security, complete coverage, mutation adequacy, or task/gate acceptance.

## Not executed and limitations

- This is a $scope execution. Unlisted categories and all listed \`NOT EXECUTED\` checks are outside its claim.
- The working tree may be dirty; compare the recorded revision and working-tree snapshot to the code under review before treating this evidence as fresh.
- When dirty, this package preserves staged and unstaged *tracked* deltas in binary-safe Git patch form. It preserves untracked paths only, not their contents; an untracked source file can therefore influence execution without being reproducible from this package. Consumers must treat that case as incomplete source provenance and request a bounded task-specific snapshot when material.
- Raw logs are bounded command output for this execution. They are evidence references, not durable project knowledge and may require redaction before external sharing.
- Review Skills may consume this package without rerunning checks when its scope and freshness are sufficient; they retain responsibility for judgment and may request more evidence.

## Reproduction

From the recorded workspace revision where practical:

\`\`\`sh
.osk/capabilities/deterministic-verification/verify.sh --task '$task_id' --objective '$objective' --scope '$scope' --targets '$targets' --checks '$checks' --executor '$executor' --evidence-dir '$evidence_dir'
\`\`\`

The command creates a new timestamped package; it does not overwrite this one.
EOF
normalize_evidence "$summary_temporary" "$summary"
rm -f "$summary_temporary"
rm -f "$targets_file" "$results_file" "$result_rows"

printf '%s\n' "deterministic-verification: $overall — $summary"
if [ "$overall" = FAIL ]; then exit 1; fi
