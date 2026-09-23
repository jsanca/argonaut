#!/usr/bin/env sh
# Stable entry point for the materialized deterministic-verification Capability.
set -eu

capability_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
workspace_root=$(CDPATH= cd -- "$capability_dir/../../.." && pwd)
export OSK_CAPABILITY_WORKSPACE_ROOT="$workspace_root"
exec /bin/sh "$capability_dir/bin/osk-qa-evidence.sh" "$@"
