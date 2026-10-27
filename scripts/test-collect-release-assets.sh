#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

for platform in macos-arm64 macos-x64 windows-x64 linux-x64; do
  mkdir -p "$tmp/artifacts/ApkSigner-$platform"
done
printf 'arm' > "$tmp/artifacts/ApkSigner-macos-arm64/ApkSigner-1.2.2.dmg"
printf 'intel' > "$tmp/artifacts/ApkSigner-macos-x64/ApkSigner-1.2.2.dmg"
printf 'win' > "$tmp/artifacts/ApkSigner-windows-x64/ApkSigner-1.2.2.msi"
printf 'linux' > "$tmp/artifacts/ApkSigner-linux-x64/ApkSigner-1.2.2.deb"

bash "$root/scripts/collect-release-assets.sh" "$tmp/artifacts" "$tmp/release"
cmp "$tmp/artifacts/ApkSigner-macos-arm64/ApkSigner-1.2.2.dmg" "$tmp/release/ApkSigner-1.2.2-macos-arm64.dmg"
cmp "$tmp/artifacts/ApkSigner-macos-x64/ApkSigner-1.2.2.dmg" "$tmp/release/ApkSigner-1.2.2-macos-x64.dmg"
cmp "$tmp/artifacts/ApkSigner-windows-x64/ApkSigner-1.2.2.msi" "$tmp/release/ApkSigner-1.2.2-windows-x64.msi"
cmp "$tmp/artifacts/ApkSigner-linux-x64/ApkSigner-1.2.2.deb" "$tmp/release/ApkSigner-1.2.2-linux-x64.deb"
[[ $(find "$tmp/release" -type f | wc -l | tr -d ' ') == 4 ]]

rm "$tmp/artifacts/ApkSigner-linux-x64/ApkSigner-1.2.2.deb"
if bash "$root/scripts/collect-release-assets.sh" "$tmp/artifacts" "$tmp/missing" >/dev/null 2>&1; then
  echo 'Expected a missing installer to fail' >&2
  exit 1
fi
printf 'linux' > "$tmp/artifacts/ApkSigner-linux-x64/ApkSigner-1.2.2.deb"
printf 'duplicate' > "$tmp/artifacts/ApkSigner-macos-arm64/duplicate.dmg"
if bash "$root/scripts/collect-release-assets.sh" "$tmp/artifacts" "$tmp/duplicate" >/dev/null 2>&1; then
  echo 'Expected duplicate installers to fail' >&2
  exit 1
fi
printf 'packageVersion = "1.2.3"\n' > "$tmp/build.gradle.kts"
bash "$root/scripts/validate-release-tag.sh" v1.2.3 "$tmp/build.gradle.kts"
bash "$root/scripts/validate-release-tag.sh" v1.2.3-beta.1 "$tmp/build.gradle.kts"
bash "$root/scripts/validate-release-tag.sh" v1.2.3-rc-1 "$tmp/build.gradle.kts"
for invalid in v1.2.4 v1.2.3.4 v1.2.3-; do
  if bash "$root/scripts/validate-release-tag.sh" "$invalid" "$tmp/build.gradle.kts" >/dev/null 2>&1; then
    echo "Expected tag $invalid to be rejected" >&2
    exit 1
  fi
done
mkdir -p "$tmp/bin"
cat > "$tmp/bin/gh" <<'MOCK'
#!/usr/bin/env bash
printf '%s\n' "$@" > "$GH_CALL_LOG"
MOCK
chmod +x "$tmp/bin/gh"
GH_CALL_LOG="$tmp/stable-args" PATH="$tmp/bin:$PATH" \
  bash "$root/scripts/publish-release.sh" v1.2.3 hbdzp/ApkSigner "$tmp/release"
if [[ $(cat "$tmp/stable-args") == *--prerelease* ]]; then
  echo 'Stable releases must not be marked as pre-release' >&2
  exit 1
fi
GH_CALL_LOG="$tmp/prerelease-args" PATH="$tmp/bin:$PATH" \
  bash "$root/scripts/publish-release.sh" v1.2.3-beta.1 hbdzp/ApkSigner "$tmp/release"
if [[ $(cat "$tmp/prerelease-args") != *--prerelease* ]]; then
  echo 'Pre-release tags must be marked as pre-release' >&2
  exit 1
fi
if [[ $(cat "$tmp/prerelease-args") != *--verify-tag* ]]; then
  echo 'Release publication must verify the tag exists' >&2
  exit 1
fi
printf 'Release asset collection checks passed.\n'
