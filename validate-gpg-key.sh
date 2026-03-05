#!/bin/bash

# PGP Key Export and Validation Script
# This script helps verify your GPG key is properly exported for Maven Central signing

echo "=== GPG Key Export Validation ==="
echo ""

# Get the key ID from user or use the provided one
KEY_ID="${1:-18DCDAD396B6599B727F66C884CF097DEA4133ED}"

echo "Using Key ID: $KEY_ID"
echo ""

# Step 1: Verify the key exists
echo "Step 1: Verifying GPG key exists..."
gpg --list-secret-keys "$KEY_ID" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Key found"
else
    echo "❌ Key NOT found"
    echo "Run: gpg --list-secret-keys to see available keys"
    exit 1
fi
echo ""

# Step 2: Export key in ASCII format
echo "Step 2: Exporting key in ASCII format..."
gpg --export-secret-keys --armor "$KEY_ID" > /tmp/private-key.asc 2>/dev/null
if [ -f /tmp/private-key.asc ]; then
    echo "✅ Key exported successfully"
    echo "   File size: $(wc -c < /tmp/private-key.asc) bytes"
else
    echo "❌ Failed to export key"
    exit 1
fi
echo ""

# Step 3: Verify the format
echo "Step 3: Verifying key format..."
head -1 /tmp/private-key.asc
if grep -q "BEGIN PGP PRIVATE KEY BLOCK" /tmp/private-key.asc; then
    echo "✅ Valid PGP private key format"
else
    echo "❌ Invalid key format"
    exit 1
fi
echo ""

# Step 4: Base64 encode (single line)
echo "Step 4: Base64 encoding (single line, no newlines)..."
cat /tmp/private-key.asc | base64 -w 0 > /tmp/private-key.base64
ENCODED_SIZE=$(wc -c < /tmp/private-key.base64)
echo "✅ Encoded successfully"
echo "   Encoded size: $ENCODED_SIZE bytes"
echo ""

# Step 5: Verify the encoded key can be decoded
echo "Step 5: Verifying encoded key is decodable..."
cat /tmp/private-key.base64 | base64 -d > /tmp/private-key-decoded.asc 2>/dev/null
if [ $? -eq 0 ]; then
    if diff -q /tmp/private-key.asc /tmp/private-key-decoded.asc > /dev/null; then
        echo "✅ Encoded key verified - decodes correctly"
    else
        echo "❌ Decoded key doesn't match original"
        exit 1
    fi
else
    echo "❌ Failed to decode base64"
    exit 1
fi
echo ""

# Step 6: Display the base64 key for copying
echo "Step 6: Your SIGNING_KEY (for GitHub Secrets):"
echo "=========================================="
cat /tmp/private-key.base64
echo ""
echo "=========================================="
echo ""
echo "IMPORTANT INSTRUCTIONS:"
echo "1. Copy the entire output above (the long base64 string)"
echo "2. Go to GitHub: Settings → Secrets and variables → Actions"
echo "3. Create/Update secret 'SIGNING_KEY'"
echo "4. Paste the ENTIRE base64 string (no line breaks or extra spaces)"
echo "5. Save the secret"
echo ""
echo "⚠️  Make sure:"
echo "   - Copy from the first character to the last (including the entire line)"
echo "   - No extra newlines before or after"
echo "   - No extra spaces or formatting"
echo ""

# Cleanup
rm -f /tmp/private-key.asc /tmp/private-key-decoded.asc /tmp/private-key.base64

echo "✅ All validations passed!"

