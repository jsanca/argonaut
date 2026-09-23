#!/usr/bin/env sh
# Mutation provider selection stays in project configuration. This package only
# executes the binding and makes the execution, scope, raw report, and safely
# normalized findings traceable without making a quality judgment.
set -u
usage() { echo 'Usage: mutate.sh --task <task-id> --objective <text> [--executor <identity>] [--evidence-dir <directory>]' >&2; }
task_id=''; objective=''; executor='unavailable'; evidence_dir=''
while [ "$#" -gt 0 ]; do case "$1" in --task) task_id=${2-}; shift 2;; --objective) objective=${2-}; shift 2;; --executor) executor=${2-}; shift 2;; --evidence-dir) evidence_dir=${2-}; shift 2;; --help) usage; exit 0;; *) usage; exit 2;; esac; done
case "$task_id" in *[!A-Za-z0-9._-]*|'') echo 'error: --task is required and must be safe for a path' >&2; exit 2;; esac
[ -n "$objective" ] || { echo 'error: --objective is required' >&2; exit 2; }
root=${OSK_CAPABILITY_WORKSPACE_ROOT:-}; [ -n "$root" ] || root=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
config="$root/.osk/mutation-testing.yaml"; [ -f "$config" ] || { echo "error: mutation-testing requires $config" >&2; exit 2; }
config_value() { sed -n "s/^$1:[[:space:]]*//p" "$config" | head -n 1 | sed 's/^"//;s/"$//'; }
[ "$(config_value schema)" = 'osk.mutation-testing/v0alpha1' ] || { echo 'error: unsupported mutation-testing configuration schema' >&2; exit 2; }
targets_file=$(mktemp "${TMPDIR:-/tmp}/osk-mutation-targets.XXXXXX"); results_file=''; rows_file=''; temporary=''
cleanup(){ rm -f "$targets_file" "$results_file" "$rows_file" "$temporary"; }; trap cleanup EXIT HUP INT TERM
trim(){ printf '%s' "$1" | sed 's/^[[:space:]]*//;s/^"//;s/"$//'; }
id=''; path=''; scope=''; provider=''; tool=''; version='unavailable'; command=''; artifact=''; format=''; seen=false
emit_target(){
  [ -n "$id" ] && [ -n "$path" ] && [ -n "$scope" ] && [ -n "$provider" ] && [ -n "$tool" ] && [ -n "$command" ] && [ -n "$artifact" ] && [ -n "$format" ] || { echo 'error: each target requires id, path, scope, and provider id/tool/command/primaryArtifact/format' >&2; exit 2; }
  case "$id" in *[!A-Za-z0-9._-]*|'') echo "error: invalid target id $id" >&2; exit 2;; esac; case "$path" in /*|*'..'*) echo "error: target path must be repository-relative" >&2; exit 2;; esac; case "$artifact" in /*|*'..'*) echo "error: primaryArtifact must be target-relative" >&2; exit 2;; esac
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$path" "$scope" "$provider" "$tool" "$version" "$command" "$artifact|$format" >> "$targets_file"
}
while IFS= read -r line || [ -n "$line" ]; do case "$line" in
 '  - id:'*) [ "$seen" = true ] && emit_target; id=$(trim "${line#'  - id:'}"); path=''; scope=''; provider=''; tool=''; version='unavailable'; command=''; artifact=''; format=''; seen=true;;
 '    path:'*) path=$(trim "${line#'    path:'}");; '    scope:'*) scope=$(trim "${line#'    scope:'}");;
 '      id:'*) provider=$(trim "${line#'      id:'}");; '      tool:'*) tool=$(trim "${line#'      tool:'}");; '      version:'*) version=$(trim "${line#'      version:'}");; '      command:'*) command=$(trim "${line#'      command:'}");; '      primaryArtifact:'*) artifact=$(trim "${line#'      primaryArtifact:'}");; '      format:'*) format=$(trim "${line#'      format:'}");; esac; done < "$config"
[ "$seen" = true ] || { echo 'error: configuration requires a target' >&2; exit 2; }; emit_target
[ "$(cut -f1 "$targets_file" | sort | uniq -d | wc -l | tr -d ' ')" = 0 ] || { echo 'error: target ids must be unique' >&2; exit 2; }
if [ -z "$evidence_dir" ]; then evidence_dir="$root/docs/engineering/agents/reviews"; elif [ "${evidence_dir#/*}" = "$evidence_dir" ]; then evidence_dir="$root/$evidence_dir"; fi
timestamp=$(date -u +%Y%m%dT%H%M%SZ); started=$(date -u +%Y-%m-%dT%H:%M:%SZ); package="$evidence_dir/$task_id/mutation-evidence/$timestamp"; raw="$package/raw"; summary="$package/mutation-testing.md"; mkdir -p "$raw"
escape_sed(){ printf '%s' "$1" | sed 's/[\\/&]/\\&/g'; }; workspace_pattern=$(escape_sed "$root"); home_pattern=$(escape_sed "${HOME:-}")
normalize(){ sed -e "s|$workspace_pattern|<WORKSPACE>|g" -e "s|$home_pattern|<HOME>|g" -e 's|/private/tmp/|<TEMP>/|g' -e 's|/tmp/|<TEMP>/|g' -e 's|/var/folders/[^/]*/[^/]*/T/|<TEMP>/|g' -e 's|/home/runner/work/|<TEMP>/|g' -e 's|/github/workspace/|<TEMP>/|g' "$1" > "$2"; }
capture(){ d=$1; shift; temporary=$(mktemp "${TMPDIR:-/tmp}/osk-mutation.XXXXXX"); "$@" > "$temporary" 2>&1 || true; normalize "$temporary" "$d"; rm -f "$temporary"; temporary=''; }
digest(){ if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'; else sha256sum "$1" | awk '{print $1}'; fi; }
revision=$(git -C "$root" rev-parse HEAD 2>/dev/null || printf unavailable); branch=$(git -C "$root" branch --show-current 2>/dev/null || printf unavailable)
capture "$raw/working-tree-status.txt" git -C "$root" status --short; capture "$raw/working-tree-unstaged.diff" git -C "$root" diff --binary; capture "$raw/working-tree-staged.diff" git -C "$root" diff --cached --binary; capture "$raw/untracked-files.txt" git -C "$root" ls-files --others --exclude-standard
normalize_report(){ source=$1; kind=$2; dest=$3; case "$kind" in
 pitest-xml) node - "$source" > "$dest" <<'EOF'
const fs=require('fs'); const x=fs.readFileSync(process.argv[2],'utf8'); const all=[...x.matchAll(/<mutation\b[^>]*\bstatus=['"]([^'"]+)['"][^>]*>/g)].map(m=>m[1]); const count=s=>all.filter(x=>x===s).length; const generated=all.length; const killed=count('KILLED'); const survived=count('SURVIVED'); const timeout=count('TIMED_OUT'); const other={}; for(const s of all) if(!['KILLED','SURVIVED','TIMED_OUT'].includes(s)) other[s]=(other[s]||0)+1; console.log(JSON.stringify({generated,killed,survived,timeout,score:null,providerSpecificStates:other},null,2));
EOF
 ;; stryker-json) node - "$source" > "$dest" <<'EOF'
const report=require(process.argv[2]); const all=[]; for(const f of Object.values(report.files||{})) for(const m of f.mutants||[]) all.push(m.status||'Unknown'); const count=s=>all.filter(x=>x===s).length; const other={}; for(const s of all) if(!['Killed','Survived','Timeout'].includes(s)) other[s]=(other[s]||0)+1; console.log(JSON.stringify({generated:all.length,killed:count('Killed'),survived:count('Survived'),timeout:count('Timeout'),score:null,providerSpecificStates:other},null,2));
EOF
 ;; *) return 1;; esac; }
results_file=$(mktemp "${TMPDIR:-/tmp}/osk-mutation-results.XXXXXX"); rows_file=$(mktemp "${TMPDIR:-/tmp}/osk-mutation-rows.XXXXXX"); tab=$(printf '\t'); overall=PASS
while IFS="$tab" read -r id path scope provider tool version command artifact_format; do artifact=${artifact_format%|*}; format=${artifact_format#*|}; target="$root/$path"; target_raw="$raw/$id"; mkdir -p "$target_raw"; log="$target_raw/provider.log"; temporary=$(mktemp "${TMPDIR:-/tmp}/osk-mutation.XXXXXX"); started_epoch=$(date +%s); if [ -d "$target" ] && sh -c "cd \"$target\" && $command" > "$temporary" 2>&1; then status=PASS; else status=FAIL; fi; ended_epoch=$(date +%s); duration=$((ended_epoch-started_epoch)); normalize "$temporary" "$log"; rm -f "$temporary"; temporary=''; source="$target/$artifact"; primary="$target_raw/primary-$(basename "$artifact")"; normalized="$target_raw/normalized-mutation.json"; finding='VALID EVIDENCE'; if [ "$status" = PASS ] && [ -f "$source" ]; then normalize "$source" "$primary"; normalize_report "$source" "$format" "$normalized" || { status=FAIL; finding='NORMALIZATION FAILED'; }; else status=FAIL; finding='PRIMARY ARTIFACT MISSING'; fi; [ "$status" = PASS ] || overall=FAIL; printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$scope" "$provider/$tool@$version" "$status" "$duration" "$finding" >> "$results_file"; n='—'; p='—'; [ -f "$normalized" ] && n=$(printf '[normalized](raw/%s/normalized-mutation.json)' "$id"); [ -f "$primary" ] && p=$(printf '[primary](raw/%s/%s) SHA-256 %s' "$id" "$(basename "$primary")" "$(digest "$primary")"); printf '| `%s` | %s | `%s` | `%s` | %ss | %s | %s |\n' "$id" "$scope" "$provider/$tool" "$status" "$duration" "$n" "$p" >> "$rows_file"; done < "$targets_file"
ended=$(date -u +%Y-%m-%dT%H:%M:%SZ)
cat > "$summary" <<EOF
# Mutation Testing Evidence — $task_id

> Capability execution evidence and mutation findings, not a mutation-quality, gate, release, or product-acceptance decision.

## Identity, execution, and provenance

- Capability: mutation-testing version 0.1.0
- Objective: $objective
- Executor: $executor
- Started (UTC): $started
- Ended (UTC): $ended
- Aggregate execution: **$overall**
- Workspace: <WORKSPACE>
- Branch/revision: $branch / $revision
- Binding: .osk/mutation-testing.yaml (osk.mutation-testing/v0alpha1)
- Evidence path normalization: workspace, current home, and known temporary/runner roots normalized before persistence.
- Working-tree status: [raw](raw/working-tree-status.txt) SHA-256 $(digest "$raw/working-tree-status.txt")

## Target provider evidence

| Target | Declared execution scope | Provider/tool | Execution | Duration | Normalized findings | Primary/raw report |
| --- | --- | --- | --- | --- | --- | --- |
$(cat "$rows_file")

## Normalization basis

- pitest-xml: counts provider mutation statuses KILLED, SURVIVED, and TIMED_OUT; all other observed statuses remain in providerSpecificStates.
- stryker-json: counts Killed, Survived, and Timeout; all other observed statuses remain in providerSpecificStates.
- score is intentionally null in the common model: providers have differing denominator/policy treatment. Consumers must inspect the native artifact for provider score semantics.

## What this supports and does not support

PASS establishes that the declared provider completed, its primary report was retained, and supported findings were derived under this recorded scope. Surviving mutants, timeouts, or a low/zero score are findings, not Capability execution failure. This does not establish test adequacy, a quality threshold, release acceptance, or a semantic dependency on coverage-measurement or deterministic-verification.

## Limits

- Scope is declared by the project binding and must be read with every measurement.
- Provider-native status taxonomies and report semantics are intentionally retained rather than forced into false symmetry.
- This is bounded raw-evidence retention and known host-path normalization, not general DLP, secret scanning, provider discovery, or incremental orchestration.
EOF
printf '%s\n' "mutation-testing: $overall — $summary"; [ "$overall" = PASS ] || exit 1
