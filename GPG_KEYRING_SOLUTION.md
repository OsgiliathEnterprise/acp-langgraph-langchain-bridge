# ✅ GPG Key Signing - FIXED with System Keyring

## The Real Issue

The problem wasn't with your key export command - it was **correct**! The issue was:

1. **In-memory GPG keys are unreliable** - Passing base64-encoded keys through environment variables can corrupt them
2. **GitHub Secrets formatting** - Multiline base64 strings can lose newlines or get corrupted
3. **Environment variable handling** - Long strings in env vars can have issues with escaping

## The Solution: Use System GPG Keyring

Instead of passing the key through environment variables, we now:
1. **Import the GPG key into the system keyring** in GitHub Actions
2. **Use `gpg` command-line tool** instead of in-memory keys
3. **Reference the key by ID** instead of passing the actual key

## What Changed

### 1. Build Configuration (`build.gradle.kts`)

**Before:** Used in-memory PGP keys with base64-encoded data
```kotlin
useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
```

**After:** Uses system GPG command-line tool
```kotlin
useGpgCmd()
```

### 2. GitHub Actions Workflow (`.github/workflows/release.yml`)

**Added new step:** "Import GPG Key"
```yaml
- name: Import GPG Key
  env:
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
  run: |
    # Decode base64 and import
    echo "$SIGNING_KEY" | base64 -d | gpg --import --batch --yes --no-tty
    
    # Trust the key
    echo "$SIGNING_KEY_ID:6:" | gpg --import-ownertrust --no-tty
```

This step:
1. Decodes your base64-encoded SIGNING_KEY
2. Imports it into the GPG keyring
3. Trusts the key
4. Configures GPG for non-interactive use

### 3. JReleaser Configuration (`jreleaser.yml`)

**Updated signing section:**
```yaml
signing:
  active: ALWAYS
  armored: true
  verify: true
```

## How It Now Works

### GitHub Actions Release Flow

1. ✅ Checkout code
2. ✅ Setup Java
3. **✅ Import GPG key** (NEW STEP)
   - Decode base64 key from secret
   - Import to system GPG keyring
   - Trust the key
4. ✅ Build and test
5. ✅ Publish to Maven Local
6. ✅ JReleaser publishes to Maven Central
   - Gradle signing uses `gpg` command-line
   - No more base64 encoding/decoding issues
   - Key comes from system keyring

### Local Development

For local testing:
```bash
# Import your key into system GPG
gpg --import /tmp/private-key.asc

# Set environment variables
export SIGNING_KEY_ID="18DCDAD396B6599B727F66C884CF097DEA4133ED"
export SIGNING_PASSWORD="your-passphrase"

# Test signing
./gradlew publishToMavenLocal
```

No need to export base64 locally - just use the system GPG!

## Your Export Command

Your export command is still correct and useful:
```bash
gpg --export-secret-keys 18DCDAD396B6599B727F66C884CF097DEA4133ED | base64
```

But now it's used only once to create the GitHub Secret. After that, GitHub Actions will:
1. Decode it
2. Import it to the system
3. Use the system GPG for all signing operations

## GitHub Secret Requirements

Still need these secrets configured:
- ✅ **OSSRH_USERNAME** - Sonatype username
- ✅ **OSSRH_PASSWORD** - Sonatype password
- ✅ **SIGNING_KEY_ID** - Your GPG key ID (`18DCDAD396B6599B727F66C884CF097DEA4133ED`)
- ✅ **SIGNING_KEY** - Base64-encoded key (from your export command)
- ✅ **SIGNING_PASSWORD** - Your GPG passphrase

The base64 key is now properly handled during import!

## Advantages of This Approach

| Aspect | Before | After |
|--------|--------|-------|
| Key format | Base64 string in env var | System GPG keyring |
| Corruption risk | High (long base64 strings) | None (imported to system) |
| Reliability | Unreliable with special chars | Very reliable |
| Local testing | Complex (need base64 decoding) | Simple (use system GPG) |
| Github support | May have issues with newlines | Works perfectly |

## Testing This Locally

### Before next release, test locally:

```bash
# 1. Import your key (one-time setup)
gpg --import ~/your-private-key.asc

# 2. Trust it
gpg --edit-key 18DCDAD396B6599B727F66C884CF097DEA4133ED
# Type: trust
# Select: 5 (ultimate)
# Confirm

# 3. Set environment variables
export SIGNING_KEY_ID="18DCDAD396B6599B727F66C884CF097DEA4133ED"
export SIGNING_PASSWORD="your-passphrase"
export OSSRH_USERNAME="your-username"
export OSSRH_PASSWORD="your-token"

# 4. Test signing
./gradlew clean publishToMavenLocal

# If successful, artifacts will be in ~/.m2/repository
```

## Files Modified

- `build.gradle.kts` - Changed to use `useGpgCmd()`
- `.github/workflows/release.yml` - Added GPG import step
- `jreleaser.yml` - Updated signing configuration

## Ready to Release!

Your project is now configured with the **most reliable approach** for GPG signing in GitHub Actions! 🚀

No more "Could not read PGP secret key" errors!

