#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  printf 'Usage: %s ARTIFACTS_DIRECTORY OUTPUT_DIRECTORY\n' "$0" >&2
  exit 2
fi
input=$1
output=$2
mkdir -p "$output"
shopt -s nullglob

copy_installer() {
  local platform=$1 extension=$2
  local files=("$input/ApkSigner-$platform"/*."$extension")
  if [[ ${#files[@]} -ne 1 ]]; then
    printf 'Expected exactly one %s installer, found %s\n' "$platform" "${#files[@]}" >&2
    return 1
  fi
  local filename=${files[0]##*/}
  cp -- "${files[0]}" "$output/${filename%.$extension}-$platform.$extension"
}

copy_installer macos-arm64 dmg
copy_installer macos-x64 dmg
copy_installer windows-x64 msi
copy_installer linux-x64 deb
