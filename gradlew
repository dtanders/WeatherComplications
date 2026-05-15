#!/usr/bin/env bash
# Gradle wrapper for bash (Git Bash / WSL)
# Reads gradle/wrapper/gradle-wrapper.properties to locate the correct Gradle version.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.properties"

dist_url=$(grep '^distributionUrl=' "$PROPS" | cut -d= -f2- | tr -d '\r' | sed 's/\\:/:/g')
dist_filename="${dist_url##*/}"                 # gradle-9.4.1-bin.zip
dist_name="${dist_filename%.zip}"               # gradle-9.4.1-bin

gradle_user_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
dists_root="$gradle_user_home/wrapper/dists/$dist_name"

hash_dir=$(ls -d "$dists_root"/*/ 2>/dev/null | head -1)
if [[ -z "$hash_dir" ]]; then
    echo "Gradle distribution not found at $dists_root" >&2
    echo "Run Android Studio or download Gradle $dist_name manually." >&2
    exit 1
fi

inner_name="${dist_name%-bin}"           # gradle-9.4.1-bin -> gradle-9.4.1
inner_name="${inner_name%-all}"          # gradle-9.4.1-all -> gradle-9.4.1

exec "${hash_dir}${inner_name}/bin/gradle" "$@"
