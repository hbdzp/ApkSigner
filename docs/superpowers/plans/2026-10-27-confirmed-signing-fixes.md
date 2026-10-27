# Confirmed signing defects implementation plan

**Goal:** Fix the verified command execution, Java configuration, signing result and directory chooser defects without implementing unconfirmed Issue diagnoses or feature requests.

**Architecture:** Keep the existing UI and signing facade. Pass executable arguments separately, read each process output once, and propagate failures through `CommandResult`. Use environment variables for signing passwords and never log their values.

**Tech stack:** Kotlin/JVM 17, Compose Desktop, kotlinx.coroutines, Gradle, kotlin.test/JUnit.

## 1. Process execution and tool configuration

- [x] Add real child-process regression tests for retained stdout/stderr, quiet execution, paths containing spaces and configured JAVA_HOME.
- [x] Run the tests against the original implementation and record failures.
- [x] Replace string splitting with argument lists; capture merged output once and retain exit errors.
- [x] Load Java before tools; use platform-specific java/java.exe validation and pass JAVA_HOME during every apksigner check.
- [x] Validate native tool files directly rather than invoking the shell-only `command` builtin.

## 2. Signing and inspection

- [x] Add fake SDK executable tests that verify actual arguments, environment, output files and failures.
- [x] Use `env:` password references, detect supported signing flags from `sign --help`, and reject explicitly selected unsupported schemes.
- [x] Propagate zipalign errors; require successful output files, use unique temporary alignment files, and remove them after signing.
- [x] Execute `aapt dump badging` as arguments and parse its retained output in Kotlin.
- [x] Verify unselected schemes remain explicitly disabled on supported tools.
- [x] Fix SignType initialization order discovered by the regression suite; test first access through V1 in an isolated class loader.

## 3. Batch results and chooser completion

- [x] Add tests for mixed results, duplicate messages and unexpected execution states.
- [x] Return a batch error when any item fails, while retaining every item's result once.
- [x] Add an event-thread test for exceptions thrown while selecting a directory.
- [x] Complete exceptional continuations using public coroutine APIs and keep dialogs on the event thread.

## 4. Validation

- [x] Run `bash ./gradlew test` and compile the complete application: 31 tests passed, including a real APK round trip with Build Tools 34.0.0 and Java 17 on macOS.
- [x] Inspect `git diff --check`, the final diff and regression coverage.
- [x] Document test outcomes and platform limits in `docs/confirmed-issues-fixes.md`; keep changes available for user review.

Review correction: `SignInfoBean.toString()` already omits passwords. The confirmed leak is the full signing command logged by `ApkSigner`.
