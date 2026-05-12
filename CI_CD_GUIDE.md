# CI/CD Pipeline Guide

This document provides a comprehensive guide to the CI/CD infrastructure for the Refactoring Exercise Android App.

## Overview

The project uses GitHub Actions for continuous integration and deployment. Multiple workflows automate testing, building, code quality checks, and releases.

## Workflows

### 1. Android CI/CD (`android-ci.yml`)

**Triggers**: Push and Pull Requests to `main` or `master` branches.

**Purpose**: Main build and test pipeline.

**Steps**:
- Checks out the code
- Sets up JDK 17 with Gradle caching
- Runs unit tests
- Checks code formatting with Spotless (ktlint)
- Generates Jacoco test coverage reports
- Builds debug APK
- Uploads artifacts (debug APK and coverage reports)

**Artifacts**:
- `debug-apk`: Debug APK for testing
- `coverage-report`: Jacoco HTML coverage reports

### 2. Security Scan (`security-scan.yml`)

**Triggers**: Push and Pull Requests to `main` or `master`, plus daily scheduled runs.

**Purpose**: Vulnerability scanning and security analysis.

**Steps**:
- Scans filesystem with Trivy for known CVEs
- Uploads results to GitHub Security tab

**Why**: Identifies security vulnerabilities in dependencies and code.

### 3. Lint and Static Analysis (`lint-analysis.yml`)

**Triggers**: Push and Pull Requests to `main` or `master`.

**Purpose**: Code quality and style enforcement.

**Steps**:
- Runs Spotless formatter check
- Runs Android Lint analysis
- Uploads lint results

**Why**: Ensures code consistency and catches potential Android-specific issues.

### 4. Release Build (`release.yml`)

**Triggers**: Push of version tags matching `v*` pattern (e.g., `v1.0.0`).

**Purpose**: Build and publish release artifacts.

**Steps**:
- Checks out the code
- Runs all tests
- Builds release APK
- Creates GitHub Release with APK attached
- Uploads release artifact

**How to Use**:
```bash
git tag v1.0.0
git push origin v1.0.0
```

**Result**: Creates a GitHub Release with the APK ready for distribution.

## Code Quality Tools

### Spotless

- **Purpose**: Ensures consistent code formatting
- **Configuration**: Uses ktlint for Kotlin and Gradle files
- **Indent**: 4 spaces, 4-space continuation indent
- **Commands**:
  - `./gradlew spotlessCheck` - Check formatting
  - `./gradlew spotlessApply` - Apply formatting

### Jacoco

- **Purpose**: Measures test code coverage
- **Configuration**: Generates HTML and XML reports
- **Output**: Reports in `app/build/reports/jacoco/jacocoTestReport/`
- **Command**: `./gradlew jacocoTestReport`

### Android Lint

- **Purpose**: Static analysis for Android-specific issues
- **Command**: `./gradlew lint`
- **Output**: XML reports in `app/build/reports/lint-results*.xml`

## Local Development

### Running CI Checks Locally

Before pushing, run these commands to ensure your code passes CI:

```bash
./gradlew spotlessApply
./gradlew test
./gradlew spotlessCheck
./gradlew lint
./gradlew jacocoTestReport
./gradlew assembleDebug
```

### Faster Iteration

For faster iteration, run only the tests you're working on:

```bash
./gradlew test --tests="com.refactoring.excercise.CounterViewModelTest"
```

## Viewing Results

### On GitHub

1. Go to **Actions** tab in the repository
2. Click on a workflow run
3. View logs or download artifacts
4. Security results appear in **Security** tab

### Locally

Coverage reports generate HTML files that can be opened in a browser:

```bash
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Troubleshooting

### Build Fails Due to Formatting

Run `./gradlew spotlessApply` to auto-fix formatting issues.

### Tests Fail in CI but Pass Locally

Ensure you're using the same JDK version (17) as the CI pipeline.

### Coverage Report Missing

The report is only generated if tests pass. Ensure `./gradlew test` succeeds.

## Best Practices

1. **Commit Messages**: Use descriptive commit messages that explain changes
2. **Code Review**: Wait for CI to pass before requesting review
3. **Testing**: Write tests for new features and bug fixes
4. **Formatting**: Use `spotlessApply` before committing
5. **Versioning**: Follow semantic versioning for releases
6. **Documentation**: Update README and comments for significant changes

## Future Enhancements

- [ ] Google Play Store integration for automated deployment
- [ ] Firebase testing lab for instrumented tests
- [ ] Performance benchmarking in CI
- [ ] Automated version bumping
- [ ] Custom Gradle plugins for optimization
