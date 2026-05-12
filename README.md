# Refactoring Exercise Android App

This is an Android application built with Kotlin and Jetpack Compose.

## CI/CD Pipeline

This project uses GitHub Actions for continuous integration and continuous deployment.

### Workflow: Android CI/CD

- **Triggers**: Runs on push and pull requests to `main` or `master` branches.
- **Jobs**:
  - **Build and Test**:
    - Checks out the code.
    - Sets up JDK 17 with Gradle caching.
    - Grants execute permission to `gradlew`.
    - Runs unit tests using `./gradlew test`.
    - Checks code formatting with Spotless (using ktlint).
    - Generates Jacoco test coverage reports.
    - Builds the debug APK using `./gradlew assembleDebug`.
    - Uploads the debug APK and coverage reports as build artifacts.

### Code Quality Tools

- **Spotless**: Ensures consistent code formatting across Kotlin and Gradle files using ktlint rules.
- **Jacoco**: Generates test coverage reports in HTML and XML formats.

### How to Use

1. Push your changes to the `main` or `master` branch.
2. The workflow will automatically run, building and testing your app.
3. Check the Actions tab in GitHub for the build status.
4. Download build artifacts from the workflow runs if needed.

For deployment to Google Play or other stores, additional steps and secrets would be required.
