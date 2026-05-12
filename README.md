# Refactoring Exercise Android App

A modern Android application built with **Kotlin** and **Jetpack Compose**, featuring a comprehensive CI/CD pipeline with automated testing, code quality checks, and release management.

## 📱 Features

- Modern Kotlin/Compose UI
- Counter functionality with ViewModel
- Comprehensive unit tests
- Automated CI/CD with GitHub Actions
- Code formatting and quality checks
- Test coverage reporting
- Security scanning
- Automated releases

## 📚 Documentation

### Quick Links

- **[GITHUB_CI_CD.md](./GITHUB_CI_CD.md)** - Complete guide to GitHub Actions workflows and CI/CD infrastructure
- **[CI_CD_GUIDE.md](./CI_CD_GUIDE.md)** - Detailed guide for local development and troubleshooting

### For GitHub-ReadyConfig

See [GITHUB_CI_CD.md](./GITHUB_CI_CD.md) for:
- Workflow descriptions
- How to trigger releases
- Where to view results
- Security scanning details
- Troubleshooting guide

## 🔄 CI/CD Pipeline Overview

This project uses **GitHub Actions** for comprehensive continuous integration and deployment.

### Main Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **Android CI/CD** | Push/PR to main | Build, test, format check, coverage |
| **Security Scan** | Push/PR/Daily | Vulnerability scanning with Trivy |
| **Lint & Analysis** | Push/PR | Code quality and Android Lint |
| **Release Build** | Version tags (v*) | Release APK creation |

### Quick Start

```bash
# Format and test locally
./gradlew spotlessApply test

# Push to trigger CI/CD
git push origin main

# Create a release
git tag v1.0.0
git push origin v1.0.0
```

## 🛠️ Code Quality Tools

### Spotless (Code Formatting)
- Ensures consistent Kotlin code style
- Uses ktlint formatter
- Integrated in all workflows
- Local commands:
  ```bash
  ./gradlew spotlessApply   # Auto-fix formatting
  ./gradlew spotlessCheck   # Check without fixing
  ```

### Jacoco (Test Coverage)
- Measures code coverage per test run
- Generates HTML reports
- Local command:
  ```bash
  ./gradlew jacocoTestReport
  ```

### Android Lint
- Static analysis for Android issues
- Runs automatically in CI
- Local command:
  ```bash
  ./gradlew lint
  ```

## 📊 Project Structure

```
app/src/
├── main/
│   └── java/com/refactoring/excercise/
│       ├── MainActivity.kt          # Main UI with Compose
│       ├── CounterViewModel.kt      # Counter business logic
│       └── ui/theme/                # Theme configuration
└── test/
    └── java/com/refactoring/excercise/
        └── CounterViewModelTest.kt  # Unit tests
```

## ✅ Running Locally

Before pushing your changes:

```bash
# Format code
./gradlew spotlessApply

# Run tests
./gradlew test

# Check code quality
./gradlew spotlessCheck lint

# Generate coverage report
./gradlew jacocoTestReport

# Build debug APK
./gradlew assembleDebug
```

## 📖 CI/CD Details

For comprehensive information about the CI/CD infrastructure, see:

1. **[GITHUB_CI_CD.md](./GITHUB_CI_CD.md)** - Main documentation
   - All workflow explanations
   - How to trigger each workflow
   - Where to view results
   - Troubleshooting

2. **[CI_CD_GUIDE.md](./CI_CD_GUIDE.md)** - Developer guide
   - Local setup instructions
   - Common workflows
   - Code quality tool details
   - Future enhancements

## 🚀 Making a Release

```bash
git tag v1.0.0
git push origin v1.0.0
# Automated release build starts
# Download APK from GitHub Releases
```

## 🔒 Security

- Trivy vulnerability scanner runs on every push
- Results available in GitHub Security tab
- Daily automated scans scheduled

## 🐛 Troubleshooting

See [GITHUB_CI_CD.md - Troubleshooting](./GITHUB_CI_CD.md#troubleshooting) for common issues and solutions.

---

For deployment to Google Play or other stores, see future enhancements in [GITHUB_CI_CD.md](./GITHUB_CI_CD.md#future-enhancements).
