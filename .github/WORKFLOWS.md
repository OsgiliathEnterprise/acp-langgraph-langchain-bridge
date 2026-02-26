# GitHub Actions CI/CD Workflows

This project uses GitHub Actions for continuous integration and testing.

## Workflows

### CI Build & Test (`ci.yml`)

Automated build, testing, and reporting pipeline triggered on:
- Push to `main`, `master`, or `develop` branches
- Pull requests to `main`, `master`, or `develop` branches
- Manual trigger via `workflow_dispatch`

#### Jobs

##### 1. Build & Test (Primary)
- **JDK**: Java 21 (Temurin distribution)
- **Build**: Gradle build without tests
- **Test**: Full test suite including:
  - JUnit 5 tests
  - Cucumber BDD tests
  - Spring Boot integration tests
- **Reports**: 
  - JUnit XML reports
  - HTML test reports
  - Artifacts retention: 30 days
- **PR Integration**: Automatic comments on PRs with test summary

**Artifacts Generated:**
- `test-results/`: Raw JUnit XML results
- `test-reports/`: HTML formatted test reports
- `build-logs/`: Build logs on failure (7-day retention)

##### 2. Code Quality Checks
- Runs Gradle code quality checks
- Continues on error (non-blocking)
- Uploads code quality reports
- Artifacts retention: 30 days

##### 3. Dependency Vulnerability Check
- Scans dependencies for known vulnerabilities
- Continues on error (non-blocking)
- Reports CVEs and security issues
- Artifacts retention: 30 days

## Test Results

Test results are published in multiple formats:

1. **GitHub Check**: Visible in PR checks as "Test Results"
2. **Artifacts**: Download detailed HTML reports from workflow run
3. **PR Comments**: Automatic summary posted to pull requests
4. **Job Summary**: Available in workflow run details

## Accessing Reports

### During Development
1. Go to the Actions tab in your GitHub repository
2. Select the workflow run
3. Download artifacts:
   - `test-results`: Raw XML results
   - `test-reports`: HTML formatted reports
   - `code-quality-reports`: Code analysis results

### After PR Merge
- Reports are retained for 30 days
- Check the workflow history for archived results

## Local Testing

Before pushing, test locally:

```bash
# Build without tests
./gradlew build -x test

# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --stacktrace

# Generate test reports
./gradlew test --info
# Reports available in: build/reports/tests/
```

## Test Reporting Features

### Cucumber Tests
- BDD scenarios tracked in Gherkin format
- Step-by-step execution reports
- Feature coverage analysis

### JUnit Tests
- Unit test results
- Integration test results
- Test execution time tracking

### HTML Reports
- Interactive test reports
- Test class hierarchy
- Pass/fail statistics
- Execution timeline

## Configuration

### Gradle Configuration
The project uses the following test configuration in `build.gradle.kts`:

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
```

### Dependencies
- **Test Framework**: JUnit 5.14.2
- **BDD**: Cucumber 7.34.2
- **Async Testing**: Awaitility 4.2.2
- **Spring Boot Test**: Spring Boot 3.4.2

## Troubleshooting

### Tests Failing in CI but Passing Locally
1. Check for environment differences (JDK version, OS)
2. Review full test logs in artifacts
3. Check for race conditions or timing issues

### Missing Test Reports
1. Verify tests are running (check build logs)
2. Ensure JUnit XML reports are generated in `build/test-results/`
3. Check that test classes follow naming convention (`*Test.java` or `*Tests.java`)

### PR Comments Not Appearing
1. Verify `GITHUB_TOKEN` has appropriate permissions
2. Check workflow permissions in repository settings
3. Review GitHub Actions logs for script errors

## Security

- All workflows use official GitHub Actions (verified publishers)
- Dependency checks identify security vulnerabilities
- Test results do not contain sensitive data (checked before publication)
- Artifacts are automatically cleaned up after retention period

## Performance

- **Gradle caching** reduces build time
- **JDK caching** speeds up setup
- **Parallel test execution** for faster feedback
- Estimated CI runtime: 5-10 minutes per job

## Contributing

When adding new tests:
1. Ensure tests follow JUnit 5 conventions
2. Include descriptive test names
3. Use Cucumber scenarios for BDD tests
4. Verify tests pass locally before pushing
5. Check CI workflow results on PR

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [JUnit 5 Documentation](https://junit.org/junit5/)
- [Cucumber Documentation](https://cucumber.io/)
- [Gradle Testing Guide](https://docs.gradle.org/current/userguide/testing.html)

