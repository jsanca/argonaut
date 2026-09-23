#!/usr/bin/env sh
# A deliberately narrow coverage-measurement capability. Provider selection is
# project-owned; this runner only executes the declared binding, retains its
# primary artifact, and normalizes the two categories proven useful here.
set -u

usage() {
  cat <<'EOF'
Usage: measure.sh --task <task-id> --objective <text> [--targets <text>] [--executor <identity>] [--evidence-dir <directory>]

The project-owned binding is .osk/coverage-measurement.yaml. Each target must
declare a provider id/tool/command/primaryArtifact/format. Supported formats:
  jacoco-xml, istanbul-json-summary

This capability reports measurement evidence only. A successful provider run or
coverage percentage is not a coverage-quality, gate, or acceptance decision.
EOF
}

task_id=''; objective=''; targets='project-declared coverage targets'; executor='unavailable'; evidence_dir=''
while [ "$#" -gt 0 ]; do
  case "$1" in
    --task) task_id=${2-}; shift 2 ;;
    --objective) objective=${2-}; shift 2 ;;
    --targets) targets=${2-}; shift 2 ;;
    --executor) executor=${2-}; shift 2 ;;
    --evidence-dir) evidence_dir=${2-}; shift 2 ;;
    --help) usage; exit 0 ;;
    *) echo "error: unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done
case "$task_id" in *[!A-Za-z0-9._-]*|'') echo 'error: --task is required and may contain only letters, numbers, dot, underscore, and hyphen' >&2; exit 2 ;; esac
[ -n "$objective" ] || { echo 'error: --objective is required' >&2; exit 2; }

root=${OSK_CAPABILITY_WORKSPACE_ROOT:-}
if [ -z "$root" ]; then root=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd); fi
config="$root/.osk/coverage-measurement.yaml"
[ -f "$config" ] || { echo "error: coverage-measurement requires $config" >&2; exit 2; }

config_value() { sed -n "s/^$1:[[:space:]]*//p" "$config" | head -n 1 | sed 's/^"//;s/"$//'; }
[ "$(config_value schema)" = 'osk.coverage-measurement/v0alpha1' ] || { echo 'error: unsupported coverage-measurement configuration schema' >&2; exit 2; }

targets_file=$(mktemp "${TMPDIR:-/tmp}/osk-coverage-targets.XXXXXX")
cleanup() { rm -f "$targets_file" "${results_file:-}" "${rows_file:-}" "${temporary:-}"; }
trap cleanup EXIT HUP INT TERM
trim_value() { printf '%s' "$1" | sed 's/^[[:space:]]*//;s/^"//;s/"$//'; }
target_id=''; target_path=''; provider_id=''; provider_tool=''; provider_version='unavailable'; provider_command=''; primary_artifact=''; provider_format=''; seen=false
write_target() {
  if [ -z "$target_id" ] || [ -z "$target_path" ] || [ -z "$provider_id" ] || [ -z "$provider_tool" ] || [ -z "$provider_command" ] || [ -z "$primary_artifact" ] || [ -z "$provider_format" ]; then
    echo 'error: each coverage target requires id, path, provider id/tool/command/primaryArtifact/format' >&2; exit 2
  fi
  case "$target_id" in *[!A-Za-z0-9._-]*|'') echo "error: invalid target id: $target_id" >&2; exit 2 ;; esac
  case "$target_path" in /*|*'..'*) echo "error: target path must be repository-relative: $target_path" >&2; exit 2 ;; esac
  case "$primary_artifact" in /*|*'..'*) echo "error: primaryArtifact must be target-relative: $primary_artifact" >&2; exit 2 ;; esac
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$target_id" "$target_path" "$provider_id" "$provider_tool" "$provider_version" "$provider_command" "$primary_artifact|$provider_format" >> "$targets_file"
}
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    '  - id:'*) if [ "$seen" = true ]; then write_target; fi; target_id=$(trim_value "${line#'  - id:'}"); target_path=''; provider_id=''; provider_tool=''; provider_version='unavailable'; provider_command=''; primary_artifact=''; provider_format=''; seen=true ;;
    '    path:'*) target_path=$(trim_value "${line#'    path:'}") ;;
    '      id:'*) provider_id=$(trim_value "${line#'      id:'}") ;;
    '      tool:'*) provider_tool=$(trim_value "${line#'      tool:'}") ;;
    '      version:'*) provider_version=$(trim_value "${line#'      version:'}") ;;
    '      command:'*) provider_command=$(trim_value "${line#'      command:'}") ;;
    '      primaryArtifact:'*) primary_artifact=$(trim_value "${line#'      primaryArtifact:'}") ;;
    '      format:'*) provider_format=$(trim_value "${line#'      format:'}") ;;
  esac
done < "$config"
[ "$seen" = true ] || { echo 'error: coverage-measurement requires at least one target' >&2; exit 2; }
write_target
[ "$(cut -f1 "$targets_file" | sort | uniq -d | wc -l | tr -d ' ')" = 0 ] || { echo 'error: coverage target ids must be unique' >&2; exit 2; }

if [ -z "$evidence_dir" ]; then evidence_dir="$root/docs/engineering/agents/reviews"; elif [ "${evidence_dir#/*}" = "$evidence_dir" ]; then evidence_dir="$root/$evidence_dir"; fi
timestamp=$(date -u +%Y%m%dT%H%M%SZ); executed_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
package_dir="$evidence_dir/$task_id/coverage-evidence/$timestamp"; raw_dir="$package_dir/raw"; summary="$package_dir/coverage-measurement.md"
mkdir -p "$raw_dir"
escape_sed() { printf '%s' "$1" | sed 's/[\\/&]/\\&/g'; }
workspace_pattern=$(escape_sed "$root"); home_pattern=$(escape_sed "${HOME:-}")
normalize() { sed -e "s|$workspace_pattern|<WORKSPACE>|g" -e "s|$home_pattern|<HOME>|g" -e 's|/private/tmp/|<TEMP>/|g' -e 's|/tmp/|<TEMP>/|g' -e 's|/var/folders/[^/]*/[^/]*/T/|<TEMP>/|g' -e 's|/home/runner/work/|<TEMP>/|g' -e 's|/github/workspace/|<TEMP>/|g' "$1" > "$2"; }
capture() { destination=$1; shift; temporary=$(mktemp "${TMPDIR:-/tmp}/osk-coverage.XXXXXX"); "$@" > "$temporary" 2>&1 || true; normalize "$temporary" "$destination"; rm -f "$temporary"; temporary=''; }
digest() { if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'; elif command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'; else printf unavailable; fi; }
revision=$(git -C "$root" rev-parse HEAD 2>/dev/null || printf unavailable); branch=$(git -C "$root" branch --show-current 2>/dev/null || printf unavailable)
capture "$raw_dir/working-tree-status.txt" git -C "$root" status --short
capture "$raw_dir/working-tree-unstaged.diff" git -C "$root" diff --binary
capture "$raw_dir/working-tree-staged.diff" git -C "$root" diff --cached --binary
capture "$raw_dir/untracked-files.txt" git -C "$root" ls-files --others --exclude-standard

normalize_measurement() {
  artifact=$1; format=$2; normalized=$3
  case "$format" in
    jacoco-xml)
      line=$(tr '>' '\n' < "$artifact" | grep 'counter type="LINE"' | tail -n 1 || true)
      branch_counter=$(tr '>' '\n' < "$artifact" | grep 'counter type="BRANCH"' | tail -n 1 || true)
      ;;
    istanbul-json-summary)
      node - "$artifact" > "$normalized" <<'EOF'
const summary = require(process.argv[2]);
const metric = name => {
  const v = summary.total && summary.total[name];
  return v ? { covered: v.covered, total: v.total, percentage: v.pct } : null;
};
console.log(JSON.stringify({lines: metric('lines'), branches: metric('branches')}, null, 2));
EOF
      return 0
      ;;
    *) return 1 ;;
  esac
  extract() { printf '%s' "$1" | sed -n "s/.*$2=\"\\([^\"]*\\)\".*/\\1/p"; }
  line_missed=$(extract "$line" missed); line_covered=$(extract "$line" covered)
  [ -n "$line_missed" ] && [ -n "$line_covered" ] || return 1
  line_total=$((line_missed + line_covered)); line_pct=$(awk "BEGIN { if ($line_total == 0) print 0; else printf \"%.2f\", ($line_covered * 100) / $line_total }")
  if [ -n "$branch_counter" ]; then branch_missed=$(extract "$branch_counter" missed); branch_covered=$(extract "$branch_counter" covered); branch_total=$((branch_missed + branch_covered)); branch_pct=$(awk "BEGIN { if ($branch_total == 0) print 0; else printf \"%.2f\", ($branch_covered * 100) / $branch_total }"); branch_json="{\"covered\": $branch_covered, \"total\": $branch_total, \"percentage\": $branch_pct}"; else branch_json=null; fi
  printf '{\n  "lines": {"covered": %s, "total": %s, "percentage": %s},\n  "branches": %s\n}\n' "$line_covered" "$line_total" "$line_pct" "$branch_json" > "$normalized"
}

results_file=$(mktemp "${TMPDIR:-/tmp}/osk-coverage-results.XXXXXX"); rows_file=$(mktemp "${TMPDIR:-/tmp}/osk-coverage-rows.XXXXXX"); tab=$(printf '\t')
overall='PASS'
while IFS="$tab" read -r target_id target_path provider_id provider_tool provider_version provider_command artifact_format; do
  primary_artifact=${artifact_format%|*}; provider_format=${artifact_format#*|}; target_dir="$root/$target_path"; target_raw="$raw_dir/$target_id"; mkdir -p "$target_raw"
  log="$target_raw/provider.log"; temporary=$(mktemp "${TMPDIR:-/tmp}/osk-coverage.XXXXXX")
  if [ -d "$target_dir" ] && sh -c "cd \"$target_dir\" && $provider_command" > "$temporary" 2>&1; then execution='PASS'; else execution='FAIL'; fi
  normalize "$temporary" "$log"; rm -f "$temporary"; temporary=''
  artifact_source="$target_dir/$primary_artifact"; primary_copy="$target_raw/primary-$(basename "$primary_artifact")"; normalized="$target_raw/normalized-measurement.json"
  if [ "$execution" = PASS ] && [ -f "$artifact_source" ]; then normalize "$artifact_source" "$primary_copy"; if normalize_measurement "$artifact_source" "$provider_format" "$normalized"; then finding='MEASURED'; else execution='FAIL'; finding='NORMALIZATION FAILED'; fi; else execution='FAIL'; finding='PRIMARY ARTIFACT MISSING'; fi
  [ "$execution" = PASS ] || overall='FAIL'
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$target_id" "$provider_id" "$provider_tool" "$provider_version" "$execution" "$finding" "$provider_format" >> "$results_file"
  primary_link='—'; normalized_link='—'; [ -f "$primary_copy" ] && primary_link=$(printf '[primary artifact](raw/%s/%s) (SHA-256 `%s`)' "$target_id" "$(basename "$primary_copy")" "$(digest "$primary_copy")"); [ -f "$normalized" ] && normalized_link=$(printf '[normalized measurement](raw/%s/normalized-measurement.json) (SHA-256 `%s`)' "$target_id" "$(digest "$normalized")")
  printf '| `%s` | `%s` / `%s` | `%s` | `%s` | %s | %s |\n' "$target_id" "$provider_id" "$provider_tool" "$execution" "$finding" "$normalized_link" "$primary_link" >> "$rows_file"
done < "$targets_file"

cat > "$summary" <<EOF
# Coverage Measurement Evidence — $task_id

> Capability execution evidence and measured findings, not a coverage policy, quality score, gate, or product-acceptance decision.

## Capability identity and intent

- Capability: <code>coverage-measurement</code> version <code>0.1.0</code>
- Objective: $objective
- Target selection: $targets
- Executor: $executor
- Executed (UTC): $executed_at
- Execution result: **$overall**
- Evidence normalization: workspace, current home, and known temporary/runner paths were normalized before persistence.

## Provenance

- Workspace: <code>&lt;WORKSPACE&gt;</code>
- Branch: <code>$branch</code>
- Revision: <code>$revision</code>
- Binding: <code>.osk/coverage-measurement.yaml</code> (<code>osk.coverage-measurement/v0alpha1</code>)
- Working-tree status: [raw snapshot](raw/working-tree-status.txt) (SHA-256 <code>$(digest "$raw_dir/working-tree-status.txt")</code>)
- Unstaged delta: [raw snapshot](raw/working-tree-unstaged.diff) (SHA-256 <code>$(digest "$raw_dir/working-tree-unstaged.diff")</code>)
- Staged delta: [raw snapshot](raw/working-tree-staged.diff) (SHA-256 <code>$(digest "$raw_dir/working-tree-staged.diff")</code>)

## Provider execution and evidence

| Target | Provider / tool | Execution | Finding | Normalized measurement | Primary/raw provider evidence |
| --- | --- | --- | --- | --- | --- |
$(cat "$rows_file")

## Derivation trace

For each <code>MEASURED</code> target, the retained primary provider artifact was parsed by the declared format adapter:

- <code>jacoco-xml</code>: final report-level <code>LINE</code> and, when present, <code>BRANCH</code> counters.
- <code>istanbul-json-summary</code>: provider's <code>total.lines</code> and <code>total.branches</code> summary fields.

The provider command log remains at <code>raw/&lt;target&gt;/provider.log</code>; the primary artifact remains adjacent to it. A normalized value without both a successful execution and retained primary artifact is not represented as valid coverage evidence.

## Limits

- Measurement is not judgment: no threshold or quality decision was applied.
- Only the two declared provider artifact formats are supported in this vertical slice.
- Provider configuration, installation, and suitability remain project-owned adaptation decisions.
- Raw evidence is bounded and normalized for known host paths; this is not generic secret or PII scanning.
EOF

printf '%s\n' "coverage-measurement: $overall — $summary"
[ "$overall" = PASS ] || exit 1
