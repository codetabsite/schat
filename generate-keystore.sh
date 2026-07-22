#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# generate-keystore.sh
# Run this ONCE locally to create your release keystore.
# Then upload the base64 output as a GitHub Secret.
# ─────────────────────────────────────────────────────────────────────────────
set -e

KEYSTORE_FILE="app/schat-release.jks"
KEY_ALIAS="schat-key"

echo "──────────────────────────────────────────"
echo " SChat Release Keystore Generator"
echo "──────────────────────────────────────────"
echo ""

read -rp "Keystore şifresi (KEYSTORE_PASSWORD): " KS_PASS
read -rp "Key şifresi (KEY_PASSWORD):            " KEY_PASS
read -rp "Ad Soyad (CN için):                    " CN_NAME

keytool -genkeypair \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$KS_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=$CN_NAME, OU=SChat, O=tdev, L=TR, ST=TR, C=TR"

echo ""
echo "✅ Keystore oluşturuldu: $KEYSTORE_FILE"
echo ""
echo "── GitHub Secrets olarak ekle ──────────────────────────────────────"
echo ""
echo "KEYSTORE_BASE64:"
base64 -i "$KEYSTORE_FILE"
echo ""
echo "KEYSTORE_PASSWORD : $KS_PASS"
echo "KEY_ALIAS         : $KEY_ALIAS"
echo "KEY_PASSWORD      : $KEY_PASS"
echo ""
echo "⚠️  Bu çıktıyı güvenli bir yere kaydet, keystore dosyasını .gitignore'a ekle!"
