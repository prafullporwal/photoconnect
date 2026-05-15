#!/usr/bin/env bash
# =============================================================================
# generate-keys.sh — one-time RSA key pair generation for JWT signing
# =============================================================================
# Run ONCE per dev environment. Keys land in auth-service/keys/ which is
# gitignored, so each developer has their own pair locally.
#
# Phase 2 swaps this script for AWS Secrets Manager retrieval at boot.
# =============================================================================
set -euo pipefail

KEYS_DIR="${1:-auth-service/keys}"
mkdir -p "$KEYS_DIR"

PRIVATE_KEY="$KEYS_DIR/private_key.pem"
PUBLIC_KEY="$KEYS_DIR/public_key.pem"

if [[ -f "$PRIVATE_KEY" && -f "$PUBLIC_KEY" ]]; then
    echo "Keys already exist at $KEYS_DIR. Delete them first if you want to rotate."
    exit 0
fi

if ! command -v openssl &> /dev/null; then
    echo "ERROR: openssl is not installed. Install via your package manager:"
    echo "  Windows (Git Bash) ships with openssl. If missing: choco install openssl"
    echo "  macOS:  brew install openssl"
    echo "  Linux:  apt install openssl  (or your distro equivalent)"
    exit 1
fi

# PKCS#8-encoded RSA 2048-bit private key (default for genpkey)
openssl genpkey -algorithm RSA -out "$PRIVATE_KEY" -pkeyopt rsa_keygen_bits:2048
openssl rsa -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY"

chmod 600 "$PRIVATE_KEY"
chmod 644 "$PUBLIC_KEY"

echo "Generated:"
echo "  $PRIVATE_KEY  (mode 600 — keep secret)"
echo "  $PUBLIC_KEY   (mode 644 — safe to distribute)"
