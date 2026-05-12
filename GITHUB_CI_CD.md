# GitHub CI/CD Infrastructure Documentation

This document explains the comprehensive CI/CD pipeline set up for the Refactoring Exercise Android App.

## Quick Start

To push to GitHub and trigger CI/CD:

```bash
# Push to main/master
git push origin main

# Create a release
git tag v1.0.0
git push origin v1.0.0
```

Check the **Actions** tab on GitHub to see workflow runs.

## Workflows Overview

### 1. **Android CI/CD** (`.github/workflows/android-ci.yml`)

The main continuous integration pipeline that runs on every push and pull request.

**When it runs:**
- Push to `main` or `master`
- Pull request to `main` or `master`

**What it does:**
1. Checks out the repository code
2. Sets up JDK 17 with Gradle caching
3. Grants execute permissions to gradlew
4. Runs unit tests
5. Checks code formatting with Spotless
6. Generates test coverage reports using Jacoco
7. Builds a debug APK
8. Uploads APK and coverage reports as artifacts

**Key jobs:**
- `build-and-test`: The main job containing all steps

**Artifacts produced:**
- `debug-apk`: The debug APK for manual testing
- `coverage-report`: HTML code coverage reports

**Why this matters:** Ensures every push and PR is tested, formatted correctly, and produces a working APK.

---

### 2. **Security Scan** (`.github/workflows/security-scan.yml`)

Automated security scanning for vulnerabilities.

**When it runs:**
- Push to `main` or `master`
- Pull request to `main` or `master`
- Daily at 2 AM UTC (scheduled)

**What it does:**
1. Sets up JDK 17
2. Runs Trivy filesystem vulnerability scanner
3. Uploads SARIF-formatted results to GitHub Security tab

**Key jobs:**
- `security-check`: Scans for vulnerabilities

**Where to view results:** GitHub **Security** → **Code scanning alerts**

**Why this matters:** Identifies known vulnerabilities in dependencies before merging code.

---

### 3. **Lint and Static Analysis** (`.github/workflows/lint-analysis.yml`)

Code quality analysis and style enforcement.

**When it runs:**
- Push to `main` or `master`
- Pull request to `main` or `master`

**What it does:**
1. Sets up JDK 17
2. Runs Spotless formatting check
3. Runs Android Lint static analysis
4. Uploads lint results as artifacts

**Key jobs:**
- `lint`: All linting checks

**Artifacts produced:**
- `lint-results`: XML lint reports

**Why this matters:** Catches code style issues, potential bugs, and Android-specific problems.

---

### 4. **Release Build** (`.github/workflows/release.yml`)

Automates building and publishing release versions.

**When it runs:**
- When a tag matching `v*` is pushed (e.g., `v1.0.0`, `v1.1.0`)

**What it does:**
1. Checks out the code
2. Sets up JDK 17
3. Runs all tests
4. Builds release APK (with minification)
5. Creates GitHub Release with the APK attached
6. Uploads release artifact

**Key jobs:**
- `build-release`: Build and release job

**Artifacts produced:**
- `release-apk`: The release APK for distribution

**How to trigger:**
```bash
git tag v1.0.0
git push origin v1.0.0
```

**Why this matters:** Automates the entire release process, ensuring consistency and providing download links on GitHub.

---

## Code Quality Tools

### Spotless

Ensures consistent code formatting using ktlint rules.

**Local usage:**
```bash
./gradlew spotlessApply    # Auto-fix all formatting issues
./gradlew spotlessCheck    # Check without fixing
```

**Configuration:** `.github/workflows/*` files use ktlint with:
- 4-space indentation
- 4-space continuation indent

**CI behavior:** Fails the build if code isn't formatted correctly.

### Jacoco

Measures test code coverage and generates reports.

**Local usage:**
```bash
./gradlew jacocoTestReport
```

**Output location:** `app/build/reports/jacoco/jacocoTestReport/html/index.html`

**View report:**
```bash
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Android Lint

Static analysis tool specific to Android development.

**Local usage:**
```bash
./gradlew lint
```

**Output location:** `app/build/reports/lint-results*.xml`

---

## Local Development Checklist

Before committing and pushing:

```bash
# 1. Format code
./gradlew spotlessApply

# 2. Run tests
./gradlew test

# 3. Verify formatting
./gradlew spotlessCheck

# 4. Run lint
./gradlew lint

# 5. Generate coverage
./gradlew jacocoTestReport

# 6. Build debug APK
./gradlew assembleDebug

# 7. Commit and push
git add .
git commit -m "Your message"
git push origin main
```

---

## Viewing Results on GitHub

### Workflow Runs

1. Go to your GitHub repository
2. Click **Actions** tab
3. View all workflow runs
4. Click a specific run to see details and logs

### Build Artifacts

1. In the workflow run details
2. Scroll down to **Artifacts** section
3. Download APKs or reports

### Security Results

1. Go to **Security** tab
2. Click **Code scanning alerts**
3. View identified vulnerabilities

### Coverage Reports

1. Download `coverage-report` artifact from Android CI/CD workflow
2. Extract and open `index.html` in browser

---

## Common Scenarios

### "My PR failed formatting check"

```bash
./gradlew spotlessApply
git add .
git commit --amend
git push origin your-branch
```

### "Tests pass locally but fail in CI"

1. Verify you're using JDK 17: `java -version`
2. Check for environment-specific code
3. Look at CI logs for specific errors

### "I want to test my changes before creating a PR"

```bash
./gradlew assembleDebug
# APK is at: app/build/outputs/apk/debug/app-debug.apk
```

### "I need to make a release"

```bash
git tag v1.0.0
git push origin v1.0.0
# Wait for release.yml to run
# Download APK from GitHub Releases
```

---

## Troubleshooting

### Build fails immediately

1. Check JDK version: `java -version` (should be 17)
2. Update Gradle wrapper: `./gradlew wrapper`
3. Clear cache: `./gradlew clean`

### Tests fail in CI but pass locally

1. Check for environment-specific code
2. Ensure you're on the same JDK version
3. Run: `./gradlew clean test`

### Coverage report not generated

1. Ensure all tests pass first
2. Check file permissions
3. Run: `./gradlew jacocoTestReport`

### Security scanner shows false positives

1. Review the vulnerability details
2. Suppress if confirmed as false positive (create issue)
3. Update dependencies if needed

---

## Secrets and Configuration

The workflows use:

- **GITHUB_TOKEN**: Automatically available for releases
- **Java 17**: Set in workflow files
- **Gradle wrapper**: Included in repository

No manual secrets need to be configured for basic CI/CD to work.

---

## Best Practices

1. **Commit early and often**: Let CI catch issues sooner
2. **Read CI failures carefully**: Detailed logs help fix issues faster
3. **Keep tests fast**: Aim for < 5 minutes for full CI run
4. **Document changes**: Include context in commits
5. **Follow versioning**: Use semantic versioning for tags (v1.0.0)
6. **Review workflows**: Understand what each workflow does

---

## Future Enhancements

Potential additions to the CI/CD system:

- [ ] Google Play Store deployment
- [ ] Firebase Test Lab integration
- [ ] Performance benchmarking
- [ ] Automated version bumping
- [ ] Slack/Discord notifications
- [ ] Custom build variants
- [ ] APK size tracking
- [ ] Screenshot testing

---

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Lint Documentation](https://developer.android.com/studio/write/lint)
- [Jacoco Coverage Tool](https://www.eclemma.org/jacoco/)
