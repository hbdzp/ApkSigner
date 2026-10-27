#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  printf 'Usage: %s VERSION_TAG REPOSITORY ASSETS_DIRECTORY\n' "$0" >&2
  exit 2
fi
tag=$1
repository=$2
assets_dir=$3
shopt -s nullglob
assets=("$assets_dir"/*)
if [[ ${#assets[@]} -ne 4 ]]; then
  printf 'Expected four release assets, found %s\n' "${#assets[@]}" >&2
  exit 1
fi

args=(--repo "$repository" --verify-tag --generate-notes --title "ApkSigner $tag")
if [[ "$tag" == *-* ]]; then
  args+=(--prerelease)
fi
gh release create "$tag" "${assets[@]}" "${args[@]}"
