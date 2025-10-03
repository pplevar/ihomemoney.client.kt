# Publishing Plan: GitHub Packages Maven Repository

**Project**: iHomemoney Kotlin API Client
**Repository**: https://github.com/pplevar/ihomemoney.client.kt
**Package Registry**: GitHub Packages (https://maven.pkg.github.com)
**Author**: Leonid Karavaev
**License**: MIT

This document provides a complete, production-ready plan for publishing this Kotlin library to GitHub Packages with automated versioning and CI/CD.

---

## Table of Contents

1. [Checkin Project for Publish Readiness](#1-checkin-project-for-publish-readiness)
2. [Prepare for Publish](#2-prepare-for-publish)
3. [Create Publish Action with Version Increment](#3-create-publish-action-with-version-increment)
4. [Appendix: Troubleshooting](#appendix-troubleshooting)

---

## 1. Checkin Project for Publish Readiness

### 1.1 DONE Current Configuration Analysis

**Current `build.gradle.kts` Status:**
```kotlin
group = "ru.levar"
version = "1.0-SNAPSHOT"
```

**Current `settings.gradle.kts` Status:**
```kotlin
rootProject.name = "iHomemoney.clien.kt"  // Note: Typo in name
```

### 1.2 Missing Maven Publishing Metadata

The following elements are **MISSING** and required for GitHub Packages:

- [ ] `maven-publish` plugin configuration
- [ ] Publication definition with artifact coordinates
- [ ] POM metadata (description, URL, licenses, developers, SCM)
- [ ] Source JAR configuration
- [ ] Javadoc/Dokka JAR configuration
- [ ] GitHub Packages repository configuration
- [ ] Authentication mechanism for publishing

### 1.3 Artifact Naming Conventions

**Current Issues:**
- `rootProject.name = "iHomemoney.clien.kt"` has a typo ("clien" should be "client")
- Recommended artifact ID should follow Kotlin conventions: `ihomemoney-client-kt`

**Recommended Maven Coordinates:**
```
Group ID:    ru.levar
Artifact ID: ihomemoney-client-kt
Version:     1.0.0 (for first release, remove -SNAPSHOT)
```

**Published Artifact Path:**
```
https://maven.pkg.github.com/pplevar/ihomemoney.client.kt/ru/levar/ihomemoney-client-kt/1.0.0/ihomemoney-client-kt-1.0.0.jar
```

### 1.4 Prerequisites and Dependencies

**Required Tools:**
- [x] Gradle 8.x (installed via wrapper)
- [x] Kotlin 2.1.20 (configured in build.gradle.kts)
- [x] Git repository with remote access
- [ ] GitHub Personal Access Token (PAT) with `write:packages` scope
- [ ] Dokka plugin for Kotlin documentation (needs to be added)

**Required Files:**
- [x] LICENSE (MIT License exists)
- [x] README.md (comprehensive documentation exists)
- [ ] Updated build.gradle.kts with publishing configuration
- [ ] GitHub Actions workflow for automated publishing
- [ ] Version properties file or version management strategy

### 1.5 Security Considerations

**Critical Security Requirements:**

1. **GitHub Token Security**
   - NEVER commit tokens to repository
   - Use GitHub Secrets for CI/CD (`GITHUB_TOKEN` is auto-provided)
   - Use `gradle.properties` in user home for local publishing
   - Add `gradle.properties` to `.gitignore` if not already present

2. **Token Permissions Required**
   - `write:packages` - Publish packages to GitHub Packages
   - `read:packages` - Read packages from GitHub Packages
   - `repo` - Access repository metadata

3. **Local Development Token Storage**
   ```
   ~/.gradle/gradle.properties
   ```
   Content:
   ```properties
   gpr.user=pplevar
   gpr.token=ghp_YOUR_PERSONAL_ACCESS_TOKEN_HERE
   ```

4. **GitHub Actions Token**
   - Use `secrets.GITHUB_TOKEN` (automatically provided by GitHub)
   - No manual configuration needed for CI/CD
   - Token is scoped to current repository only

**Security Best Practices:**
- Tokens expire - set calendar reminders for renewal
- Use fine-grained PATs with minimal scope when possible
- Rotate tokens regularly (every 90 days recommended)
- Revoke tokens immediately if compromised
- Never log tokens in build output

### 1.6 Pre-Publish Checklist

**Code Quality:**
- [x] All tests passing (68 tests across 6 test classes)
- [x] Lint checks passing (ktlint configured)
- [x] No hardcoded credentials in code
- [x] Comprehensive test coverage with MockWebServer
- [ ] Run full build: `./gradlew clean build`

**Documentation:**
- [x] README.md complete with usage examples
- [x] LICENSE file present (MIT)
- [ ] CHANGELOG.md (recommended for version tracking)
- [x] API documentation in code comments

**Repository Setup:**
- [x] GitHub repository created
- [x] Git remote configured
- [ ] Branch protection rules (recommended for main branch)
- [ ] GitHub Actions enabled

**Version Management:**
- [ ] Remove `-SNAPSHOT` suffix for release builds
- [ ] Decide on versioning strategy (semantic versioning recommended)
- [ ] Create git tags for releases

---

## 2. Prepare for Publish

### 2.1 Fix Project Naming

**Action Required:** Fix the typo in `settings.gradle.kts`

**Current:**
```kotlin
rootProject.name = "iHomemoney.clien.kt"
```

**Updated:**
```kotlin
rootProject.name = "ihomemoney-client-kt"
```

**Execute:**
```bash
# Edit settings.gradle.kts and change the rootProject.name
# Verify the change
cat settings.gradle.kts
```

### 2.2 Update build.gradle.kts with Publishing Configuration

**Complete Updated `build.gradle.kts`:**

```kotlin
plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.20"
}

group = "ru.levar"
version = project.findProperty("version") as String? ?: "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Kotest testing framework
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
}

tasks.test {
    useJUnitPlatform()
}

// ============================================================================
// Maven Publishing Configuration
// ============================================================================

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaHtml"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "ihomemoney-client-kt"
            version = project.version.toString()

            pom {
                name.set("iHomemoney Kotlin Client")
                description.set("A Kotlin-based REST API client for the iHomemoney personal finance service")
                url.set("https://github.com/pplevar/ihomemoney.client.kt")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("pplevar")
                        name.set("Leonid Karavaev")
                        email.set("pplevar@users.noreply.github.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/pplevar/ihomemoney.client.kt.git")
                    developerConnection.set("scm:git:ssh://github.com/pplevar/ihomemoney.client.kt.git")
                    url.set("https://github.com/pplevar/ihomemoney.client.kt")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/pplevar/ihomemoney.client.kt")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// ============================================================================
// Publishing Tasks
// ============================================================================

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publishes a SNAPSHOT version to GitHub Packages"

    doFirst {
        if (!project.version.toString().endsWith("-SNAPSHOT")) {
            throw GradleException("Version must end with -SNAPSHOT for snapshot publishing")
        }
    }

    dependsOn("publish")
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes a release version to GitHub Packages"

    doFirst {
        if (project.version.toString().endsWith("-SNAPSHOT")) {
            throw GradleException("Version must not end with -SNAPSHOT for release publishing")
        }
        if (project.version.toString() == "unspecified") {
            throw GradleException("Version must be specified for release publishing")
        }
    }

    dependsOn("publish")
}
```

**Key Changes Explained:**

1. **Dokka Plugin**: Added for generating Kotlin documentation (Javadoc equivalent)
2. **maven-publish Plugin**: Core plugin for Maven publishing functionality
3. **Source & Javadoc JARs**: Required for Maven Central compatibility, good practice for GitHub Packages
4. **POM Metadata**: Complete project metadata including licenses, developers, and SCM information
5. **GitHub Packages Repository**: Configured with credential resolution from properties or environment
6. **Custom Tasks**: Helper tasks for snapshot vs release publishing with validation

### 2.3 Create Version Management File

**Option 1: Simple Version File**

Create `version.properties` in project root:
```properties
# Version configuration for iHomemoney Kotlin Client
# Format: MAJOR.MINOR.PATCH[-SNAPSHOT]
version=1.0.0
```

Update `build.gradle.kts` to read from this file:
```kotlin
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
}

version = versionProps["version"] as String? ?: "1.0.0-SNAPSHOT"
```

**Option 2: Use Git Tags (Recommended for Automation)**

Version is derived from git tags automatically in GitHub Actions (see Section 3).

### 2.4 Setup Local Gradle Properties

**Create/Update `~/.gradle/gradle.properties`:**

```bash
# Create gradle properties file in user home
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << 'EOF'
# GitHub Packages Authentication
# Replace with your actual GitHub username and token
gpr.user=pplevar
gpr.token=ghp_YOUR_PERSONAL_ACCESS_TOKEN_HERE

# Optional: Gradle daemon settings for better performance
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
EOF

# Secure the file
chmod 600 ~/.gradle/gradle.properties

echo "Created ~/.gradle/gradle.properties - REMEMBER TO UPDATE THE TOKEN!"
```

**Generate GitHub Personal Access Token:**

1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Set note: "Gradle Publishing - iHomemoney Client"
4. Select scopes:
   - `write:packages` (required for publishing)
   - `read:packages` (required for downloading)
   - `repo` (if private repository)
5. Set expiration (90 days recommended)
6. Generate and copy the token
7. Update `gpr.token` in `~/.gradle/gradle.properties`

**Security Note:** This file contains sensitive credentials. Never commit it to version control.

### 2.5 Update .gitignore

**Add to `.gitignore` (if not already present):**

```bash
# Add these lines to .gitignore
cat >> .gitignore << 'EOF'

# Gradle properties with secrets
gradle.properties
local.properties

# Version properties (if using local versioning)
# version.properties  # Uncomment if you want version file in git
EOF
```

### 2.6 Local Testing Strategy

**Step 1: Publish to Maven Local**

```bash
# Publish to local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal

# Verify publication
ls -la ~/.m2/repository/ru/levar/ihomemoney-client-kt/
```

**Expected Output:**
```
~/.m2/repository/ru/levar/ihomemoney-client-kt/
├── 1.0.0/
│   ├── ihomemoney-client-kt-1.0.0.jar
│   ├── ihomemoney-client-kt-1.0.0.pom
│   ├── ihomemoney-client-kt-1.0.0-sources.jar
│   ├── ihomemoney-client-kt-1.0.0-javadoc.jar
│   └── ... (checksum files)
└── maven-metadata-local.xml
```

**Step 2: Create Test Consumer Project**

```bash
# Create a test directory
mkdir -p /tmp/test-consumer && cd /tmp/test-consumer

# Create build.gradle.kts
cat > build.gradle.kts << 'EOF'
plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("ru.levar:ihomemoney-client-kt:1.0.0")
}

tasks.register("testDependency") {
    doLast {
        println("Testing dependency resolution...")
        configurations.runtimeClasspath.get().files.forEach {
            println("  Found: ${it.name}")
        }
    }
}
EOF

# Test dependency resolution
gradle testDependency
```

**Expected Output:**
```
Testing dependency resolution...
  Found: ihomemoney-client-kt-1.0.0.jar
  Found: retrofit-3.0.0.jar
  ... (all transitive dependencies)
```

**Step 3: Validate POM Content**

```bash
# View generated POM
cat ~/.m2/repository/ru/levar/ihomemoney-client-kt/1.0.0/ihomemoney-client-kt-1.0.0.pom

# Validate POM structure
xmllint --noout ~/.m2/repository/ru/levar/ihomemoney-client-kt/1.0.0/ihomemoney-client-kt-1.0.0.pom && echo "POM is valid XML"
```

**POM Validation Checklist:**
- [ ] Contains `<groupId>ru.levar</groupId>`
- [ ] Contains `<artifactId>ihomemoney-client-kt</artifactId>`
- [ ] Contains `<version>1.0.0</version>`
- [ ] Contains `<name>`, `<description>`, `<url>`
- [ ] Contains `<licenses>` with MIT license
- [ ] Contains `<developers>` with author info
- [ ] Contains `<scm>` with repository URLs
- [ ] Contains `<dependencies>` with all runtime dependencies

### 2.7 Dry Run Publish to GitHub Packages

**Before actual publishing, test the configuration:**

```bash
# Ensure credentials are configured
./gradlew publish --dry-run

# Check task dependencies
./gradlew publish --dry-run --info | grep -A 20 "Task.*publish"
```

**Verify Credentials Resolution:**

```bash
# Test credential resolution
./gradlew properties | grep -E "(gpr.user|GITHUB_ACTOR)"

# Should show your GitHub username from gradle.properties
```

### 2.8 First Manual Publish (Test)

**Execute Manual Publish:**

```bash
# Clean build first
./gradlew clean

# Run all tests
./gradlew test

# Publish to GitHub Packages
./gradlew publish

# Or use the custom task
./gradlew publishRelease
```

**Expected Output:**
```
> Task :generateMetadataFileForMavenPublication
> Task :generatePomFileForMavenPublication
> Task :publishMavenPublicationToGitHubPackagesRepository
> Task :publish

BUILD SUCCESSFUL in 45s
```

**Verify Publication:**

1. Go to GitHub repository: https://github.com/pplevar/ihomemoney.client.kt
2. Click "Packages" on the right sidebar
3. You should see `ihomemoney-client-kt` package listed
4. Click on the package to see versions and installation instructions

**Package URL:**
```
https://github.com/pplevar/ihomemoney.client.kt/packages/
```

### 2.9 Validation Steps After First Publish

**1. Verify Package Visibility:**
```bash
# Try to download the published package (requires authentication)
curl -H "Authorization: token YOUR_TOKEN" \
  https://maven.pkg.github.com/pplevar/ihomemoney.client.kt/ru/levar/ihomemoney-client-kt/1.0.0/ihomemoney-client-kt-1.0.0.pom
```

**2. Test Dependency Resolution:**

Create a consumer project with GitHub Packages repository:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/pplevar/ihomemoney.client.kt")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("ru.levar:ihomemoney-client-kt:1.0.0")
}
```

**3. Verify All Artifacts:**

Check that all artifacts were published:
- `ihomemoney-client-kt-1.0.0.jar` (main JAR)
- `ihomemoney-client-kt-1.0.0.pom` (Maven metadata)
- `ihomemoney-client-kt-1.0.0-sources.jar` (source code)
- `ihomemoney-client-kt-1.0.0-javadoc.jar` (documentation)

**4. Check Package Metadata:**

In GitHub UI, verify:
- Package name is correct
- Version is listed
- Installation instructions are displayed
- Package is linked to repository

---

## 3. Create Publish Action with Version Increment

### 3.1 Automated Versioning Strategy

**Semantic Versioning Approach:**

```
MAJOR.MINOR.PATCH[-SNAPSHOT]

MAJOR: Breaking changes (incompatible API changes)
MINOR: New features (backward-compatible)
PATCH: Bug fixes (backward-compatible)
```

**Version Increment Strategies:**

1. **Manual Version Tags** (Recommended for controlled releases)
   - Create git tags: `v1.0.0`, `v1.1.0`, `v2.0.0`
   - GitHub Actions triggers on tag push
   - No automatic increment, full control

2. **Automatic Patch Increment** (For frequent releases)
   - Fetch latest version from GitHub Packages
   - Increment patch version automatically
   - Suitable for continuous delivery

3. **Conventional Commits** (Advanced)
   - Parse commit messages (`feat:`, `fix:`, `BREAKING:`)
   - Determine version bump automatically
   - Requires discipline in commit messages

**Recommended: Manual Version Tags**

This plan uses manual git tags for predictable, controlled releases.

### 3.2 GitHub Actions Workflow: Publish on Tag

**Create `.github/workflows/publish.yml`:**

```yaml
name: Publish to GitHub Packages

on:
  push:
    tags:
      - 'v*.*.*'  # Trigger on version tags like v1.0.0, v1.2.3
  workflow_dispatch:  # Allow manual trigger
    inputs:
      version:
        description: 'Version to publish (e.g., 1.0.0)'
        required: true
        type: string

env:
  JAVA_VERSION: '17'
  JAVA_DISTRIBUTION: 'temurin'

jobs:
  publish:
    name: Build and Publish
    runs-on: ubuntu-latest

    permissions:
      contents: write      # For creating releases
      packages: write      # For publishing to GitHub Packages

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Fetch all history for version detection

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: Extract version from tag
        id: version
        run: |
          if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
            VERSION="${{ github.event.inputs.version }}"
          else
            VERSION=${GITHUB_REF#refs/tags/v}
          fi
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT
          echo "Publishing version: $VERSION"

      - name: Validate version format
        run: |
          VERSION="${{ steps.version.outputs.VERSION }}"
          if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "Error: Version '$VERSION' does not match semantic versioning format (X.Y.Z)"
            exit 1
          fi
          echo "Version format is valid: $VERSION"

      - name: Update version in build.gradle.kts
        run: |
          VERSION="${{ steps.version.outputs.VERSION }}"
          sed -i "s/^version = .*/version = \"$VERSION\"/" build.gradle.kts
          echo "Updated version to $VERSION in build.gradle.kts"
          cat build.gradle.kts | grep "^version = "

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run ktlint
        run: ./gradlew ktlintCheck

      - name: Build project
        run: ./gradlew clean build --no-daemon

      - name: Run tests
        run: ./gradlew test --no-daemon

      - name: Generate test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: build/reports/tests/test/
          retention-days: 30

      - name: Publish to GitHub Packages
        run: ./gradlew publish --no-daemon
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_ACTOR: ${{ github.actor }}

      - name: Create GitHub Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: v${{ steps.version.outputs.VERSION }}
          release_name: Release v${{ steps.version.outputs.VERSION }}
          body: |
            ## iHomemoney Kotlin Client v${{ steps.version.outputs.VERSION }}

            ### Installation

            Add to your `build.gradle.kts`:

            ```kotlin
            repositories {
                maven {
                    url = uri("https://maven.pkg.github.com/pplevar/ihomemoney.client.kt")
                    credentials {
                        username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                        password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }

            dependencies {
                implementation("ru.levar:ihomemoney-client-kt:${{ steps.version.outputs.VERSION }}")
            }
            ```

            ### Maven Coordinates

            ```
            Group ID:    ru.levar
            Artifact ID: ihomemoney-client-kt
            Version:     ${{ steps.version.outputs.VERSION }}
            ```

            ### Changes

            See commit history for detailed changes.
          draft: false
          prerelease: false

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: |
            build/libs/*.jar
            build/publications/maven/pom-default.xml
          retention-days: 30

  verify-publication:
    name: Verify Publication
    needs: publish
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Extract version
        id: version
        run: |
          if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
            VERSION="${{ github.event.inputs.version }}"
          else
            VERSION=${GITHUB_REF#refs/tags/v}
          fi
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT

      - name: Wait for package availability
        run: sleep 30

      - name: Verify package in GitHub Packages
        run: |
          VERSION="${{ steps.version.outputs.VERSION }}"
          PACKAGE_URL="https://maven.pkg.github.com/pplevar/ihomemoney.client.kt/ru/levar/ihomemoney-client-kt/$VERSION/ihomemoney-client-kt-$VERSION.pom"

          HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: token ${{ secrets.GITHUB_TOKEN }}" \
            "$PACKAGE_URL")

          if [ $HTTP_CODE -eq 200 ]; then
            echo "✅ Package successfully published and accessible"
            echo "📦 Package URL: $PACKAGE_URL"
          else
            echo "❌ Package verification failed (HTTP $HTTP_CODE)"
            exit 1
          fi
```

**Workflow Features:**

1. **Trigger Conditions:**
   - Automatic: On git tag push matching `v*.*.*` pattern
   - Manual: Via workflow_dispatch with version input

2. **Version Extraction:**
   - Extracts version from git tag (`v1.2.3` → `1.2.3`)
   - Validates semantic versioning format

3. **Build Pipeline:**
   - Lint checking (ktlint)
   - Full build with tests
   - Test report upload for debugging

4. **Publishing:**
   - Uses `GITHUB_TOKEN` (auto-provided, no secrets needed)
   - Publishes to GitHub Packages

5. **Release Creation:**
   - Automatic GitHub Release with installation instructions
   - Includes Maven coordinates and usage examples

6. **Verification:**
   - Separate job to verify package accessibility
   - Ensures publication succeeded

### 3.3 GitHub Actions Workflow: Snapshot Publishing (Optional)

**Create `.github/workflows/publish-snapshot.yml`:**

```yaml
name: Publish Snapshot

on:
  push:
    branches:
      - main
      - develop
  workflow_dispatch:

env:
  JAVA_VERSION: '17'
  JAVA_DISTRIBUTION: 'temurin'

jobs:
  publish-snapshot:
    name: Build and Publish Snapshot
    runs-on: ubuntu-latest

    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: Generate snapshot version
        id: version
        run: |
          BRANCH_NAME=$(echo ${GITHUB_REF#refs/heads/} | sed 's/\//-/g')
          COMMIT_SHA=${GITHUB_SHA::7}
          TIMESTAMP=$(date +%Y%m%d%H%M%S)
          VERSION="1.0.0-${BRANCH_NAME}-${TIMESTAMP}-${COMMIT_SHA}-SNAPSHOT"
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT
          echo "Publishing snapshot version: $VERSION"

      - name: Update version in build.gradle.kts
        run: |
          VERSION="${{ steps.version.outputs.VERSION }}"
          sed -i "s/^version = .*/version = \"$VERSION\"/" build.gradle.kts
          echo "Updated version to $VERSION"

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build and test
        run: ./gradlew clean build test --no-daemon

      - name: Publish snapshot to GitHub Packages
        run: ./gradlew publish --no-daemon
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_ACTOR: ${{ github.actor }}

      - name: Comment on commit (if triggered by push)
        if: github.event_name == 'push'
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.repos.createCommitComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              commit_sha: context.sha,
              body: `📦 Snapshot published: \`${{ steps.version.outputs.VERSION }}\`\n\nInstall with:\n\`\`\`kotlin\nimplementation("ru.levar:ihomemoney-client-kt:${{ steps.version.outputs.VERSION }}")\n\`\`\``
            })
```

**Snapshot Workflow Features:**

- Triggers on push to main/develop branches
- Generates unique snapshot versions with timestamp and commit SHA
- Useful for testing pre-release versions
- Optional - only needed if you want automated snapshot publishing

### 3.4 Creating and Pushing Version Tags

**Manual Release Process:**

```bash
# 1. Ensure you're on main branch with clean working directory
git checkout main
git pull origin main
git status  # Should show "nothing to commit, working tree clean"

# 2. Run full test suite locally
./gradlew clean build test

# 3. Update CHANGELOG.md (recommended)
# Add release notes for the version

# 4. Create version tag
VERSION="1.0.0"  # Change this for each release
git tag -a "v${VERSION}" -m "Release version ${VERSION}"

# 5. View tag details
git show "v${VERSION}"

# 6. Push tag to trigger GitHub Actions
git push origin "v${VERSION}"

# 7. Monitor GitHub Actions
# Go to: https://github.com/pplevar/ihomemoney.client.kt/actions
```

**Version Increment Examples:**

```bash
# Initial release
git tag -a "v1.0.0" -m "Initial release"

# Bug fix (patch increment)
git tag -a "v1.0.1" -m "Fix authentication token handling"

# New feature (minor increment)
git tag -a "v1.1.0" -m "Add transaction filtering support"

# Breaking change (major increment)
git tag -a "v2.0.0" -m "Refactor API with breaking changes"
```

**Tag Naming Conventions:**

- Always prefix with `v`: `v1.0.0` (not `1.0.0`)
- Use semantic versioning: `vMAJOR.MINOR.PATCH`
- Add descriptive message: `git tag -a "v1.0.0" -m "Description"`
- Never modify or delete published tags (creates confusion)

### 3.5 Manual Workflow Trigger

**Trigger from GitHub UI:**

1. Go to repository: https://github.com/pplevar/ihomemoney.client.kt
2. Click "Actions" tab
3. Select "Publish to GitHub Packages" workflow
4. Click "Run workflow" dropdown
5. Select branch
6. Enter version (e.g., `1.0.0`)
7. Click "Run workflow"

**Trigger from GitHub CLI:**

```bash
# Install GitHub CLI if needed
# brew install gh  # macOS
# Or download from https://cli.github.com/

# Authenticate
gh auth login

# Trigger workflow
gh workflow run publish.yml -f version=1.0.0

# Monitor workflow run
gh run watch
```

### 3.6 Rollback and Error Handling

**Rollback Strategy:**

1. **Failed Publication (Build/Test Errors):**
   ```bash
   # Fix the issue in code
   # Delete the failed tag locally
   git tag -d v1.0.0

   # Delete remote tag if it was pushed
   git push --delete origin v1.0.0

   # Create tag again after fix
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **Published Wrong Version:**
   ```bash
   # GitHub Packages doesn't support deletion of published versions
   # Best practice: Publish a new patch version with fix
   git tag -a v1.0.1 -m "Fix issues in v1.0.0"
   git push origin v1.0.1
   ```

3. **Critical Bug in Published Version:**
   ```bash
   # Publish a new patch version immediately
   git tag -a v1.0.1 -m "Critical fix for bug in v1.0.0"
   git push origin v1.0.1

   # Update release notes to mark v1.0.0 as deprecated
   # Use GitHub UI to edit the release and add warning
   ```

**Error Handling in Workflow:**

The workflow includes these safety measures:

- Version format validation (must match `X.Y.Z`)
- Test failure halts publication
- Lint failure halts publication
- Build failure halts publication
- Test reports uploaded for debugging
- Verification job ensures package is accessible

**Common Issues and Solutions:**

| Issue | Solution |
|-------|----------|
| Authentication failed | Check `GITHUB_TOKEN` permissions in Settings → Actions → General → Workflow permissions |
| Version already exists | GitHub Packages doesn't allow overwriting - increment version |
| Tests fail in CI but pass locally | Check Java version, environment differences |
| Workflow doesn't trigger | Verify tag format matches `v*.*.*` pattern |
| Package not found after publish | Wait 30-60 seconds for propagation |

### 3.7 Post-Publish Actions

**After Successful Publication:**

1. **Verify Package on GitHub:**
   - Visit: https://github.com/pplevar/ihomemoney.client.kt/packages
   - Confirm new version is listed
   - Check installation instructions

2. **Update Documentation:**
   ```bash
   # Update README.md with latest version
   sed -i 's/ihomemoney-client-kt:.*"/ihomemoney-client-kt:1.0.0"/' README.md

   # Commit documentation update
   git add README.md
   git commit -m "docs: Update version to 1.0.0 in README"
   git push origin main
   ```

3. **Update CHANGELOG.md:**
   ```markdown
   ## [1.0.0] - 2025-10-03

   ### Added
   - Initial release
   - Authentication support
   - Account management
   - Category operations
   - Transaction retrieval

   ### Dependencies
   - Retrofit 3.0.0
   - OkHttp 4.10.0
   - Kotlin Coroutines 1.6.4
   ```

4. **Announce Release:**
   - GitHub Discussions (if enabled)
   - Repository README badge
   - Social media (if applicable)

5. **Monitor Issues:**
   - Watch for bug reports
   - Respond to questions
   - Plan next version if needed

---

## Appendix: Troubleshooting

### Common Build Issues

**Issue: "Task 'publish' not found"**
```
Solution: Ensure maven-publish plugin is applied in build.gradle.kts
plugins {
    id("maven-publish")
}
```

**Issue: "Could not resolve dependencies"**
```
Solution: Ensure repositories are defined before dependencies
repositories {
    mavenCentral()
}
```

**Issue: "Dokka task failed"**
```
Solution: Add Dokka plugin and ensure Kotlin version compatibility
plugins {
    id("org.jetbrains.dokka") version "1.9.20"
}
```

### Authentication Issues

**Issue: "Unauthorized (401)"**
```
Solutions:
1. Verify token has write:packages scope
2. Check token hasn't expired
3. Ensure username matches GitHub username
4. Verify credentials in ~/.gradle/gradle.properties
```

**Issue: "Forbidden (403)"**
```
Solutions:
1. Check repository access permissions
2. Verify package namespace matches repository owner
3. Ensure GITHUB_TOKEN has packages: write permission
```

### GitHub Actions Issues

**Issue: "Workflow not triggering"**
```
Solutions:
1. Verify tag format matches trigger pattern (v*.*.*)
2. Check Actions are enabled in repository settings
3. Verify workflow file is in .github/workflows/
4. Check workflow syntax with: gh workflow view publish.yml
```

**Issue: "Permission denied in Actions"**
```
Solution: Update repository workflow permissions
Settings → Actions → General → Workflow permissions
Select: "Read and write permissions"
```

### Publication Verification

**Test Package Download:**
```bash
# Create test project
mkdir test-download && cd test-download

cat > build.gradle.kts << 'EOF'
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/pplevar/ihomemoney.client.kt")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: "pplevar"
            password = System.getenv("GITHUB_TOKEN") ?: "YOUR_TOKEN"
        }
    }
    mavenCentral()
}

configurations.create("testDownload")

dependencies {
    "testDownload"("ru.levar:ihomemoney-client-kt:1.0.0")
}

tasks.register("downloadTest") {
    doLast {
        configurations["testDownload"].files.forEach {
            println("Downloaded: ${it.name}")
        }
    }
}
EOF

./gradlew downloadTest
```

### Version Management

**Check Latest Published Version:**
```bash
# Using GitHub CLI
gh api /users/pplevar/packages/maven/ru.levar.ihomemoney-client-kt/versions \
  --jq '.[0].name'

# Using curl
curl -H "Authorization: token YOUR_TOKEN" \
  https://api.github.com/users/pplevar/packages/maven/ru.levar.ihomemoney-client-kt/versions \
  | jq '.[0].name'
```

**List All Published Versions:**
```bash
gh api /users/pplevar/packages/maven/ru.levar.ihomemoney-client-kt/versions \
  --jq '.[] | .name'
```

### Clean Build Environment

**Reset Build Environment:**
```bash
# Clean all build artifacts
./gradlew clean

# Clear Gradle cache
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/wrapper/

# Clear local Maven repository
rm -rf ~/.m2/repository/ru/levar/ihomemoney-client-kt/

# Re-download dependencies
./gradlew build --refresh-dependencies
```

### Debugging Gradle Publishing

**Enable Gradle Debug Logging:**
```bash
# Full debug output
./gradlew publish --debug > publish-debug.log 2>&1

# Info level (less verbose)
./gradlew publish --info

# Show stacktrace on errors
./gradlew publish --stacktrace
```

**Inspect Generated POM:**
```bash
# Generate POM without publishing
./gradlew generatePomFileForMavenPublication

# View generated POM
cat build/publications/maven/pom-default.xml
```

**Test Credential Resolution:**
```bash
./gradlew publish --dry-run --info | grep -A 5 "credentials"
```

---

## Summary Checklist

### Pre-Publishing (One-Time Setup)

- [ ] Fix typo in `settings.gradle.kts` (`iHomemoney.clien.kt` → `ihomemoney-client-kt`)
- [ ] Update `build.gradle.kts` with publishing configuration
- [ ] Add Dokka plugin for documentation generation
- [ ] Create `~/.gradle/gradle.properties` with GitHub credentials
- [ ] Generate GitHub Personal Access Token with `write:packages` scope
- [ ] Update `.gitignore` to exclude `gradle.properties`
- [ ] Create `.github/workflows/publish.yml` workflow file
- [ ] Test local publishing with `publishToMavenLocal`
- [ ] Verify POM metadata is complete
- [ ] Create test consumer project to verify artifacts

### Publishing a Release

- [ ] Ensure working directory is clean (`git status`)
- [ ] Run full test suite locally (`./gradlew clean build test`)
- [ ] Update `CHANGELOG.md` with release notes
- [ ] Create version tag (`git tag -a v1.0.0 -m "Release description"`)
- [ ] Push tag to GitHub (`git push origin v1.0.0`)
- [ ] Monitor GitHub Actions workflow
- [ ] Verify package appears in GitHub Packages
- [ ] Test downloading published package
- [ ] Create GitHub Release with notes
- [ ] Update README.md with latest version
- [ ] Announce release (if applicable)

### Maintenance

- [ ] Set calendar reminder for token expiration (90 days)
- [ ] Monitor GitHub Actions for workflow failures
- [ ] Respond to issues and pull requests
- [ ] Plan next version based on feedback
- [ ] Keep dependencies updated
- [ ] Maintain CHANGELOG.md for all releases

---

## Additional Resources

**GitHub Documentation:**
- [Publishing to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
- [Configuring Gradle for use with GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#publishing-a-package)
- [GitHub Actions for Java](https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-gradle)

**Gradle Documentation:**
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

**Best Practices:**
- [Semantic Versioning](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

**Document Version:** 1.0
**Last Updated:** 2025-10-03
**Author:** Claude Code (Anthropic)
**Status:** Production Ready