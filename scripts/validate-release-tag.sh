#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  printf 'Usage: %s VERSION_TAG BUILD_GRADLE_FILE\n' "$0" >&2
  exit 2
fi
tag=$1
build_file=$2
if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[-0-9A-Za-z.]+)?$ ]]; then
  printf 'Invalid version tag: %s\n' "$tag" >&2
  exit 1
fi

if ! version=$(awk -F '"' '/^[[:space:]]*packageVersion[[:space:]]*=/ { n++; value=$2 } END { if (n == 1) print value; else exit 1 }' "$build_file"); then
  printf 'Could not find exactly one packageVersion in %s\n' "$build_file" >&2
  exit 1
fi
tag_version=${tag#v}
tag_version=${tag_version%%-*}
if [[ "$tag_version" != "$version" ]]; then
  printf 'Tag %s does not match packageVersion %s\n' "$tag" "$version" >&2
  exit 1
fi
