#!/bin/bash
# CMK cho chat (ADR 0007). LocalStack Community không persist state → cố định cả key id
# lẫn key material để sau khi restart, các DEK đã bọc trong DB vẫn giải được.
# Key material này chỉ dùng cho LocalStack dev, không phải bí mật.
set -e
KEY_ID=3e400d0b-a89a-4381-8943-a0b887e888c0
if ! awslocal kms describe-key --key-id "$KEY_ID" >/dev/null 2>&1; then
  awslocal kms create-key --tags \
    "[{\"TagKey\":\"_custom_id_\",\"TagValue\":\"$KEY_ID\"},{\"TagKey\":\"_custom_key_material_\",\"TagValue\":\"KouExPdFoCA28gf30JzN7RBuKQhxmbbB5Dms9AhEcMM=\"}]" >/dev/null
fi
awslocal kms create-alias --alias-name alias/social-chat --target-key-id "$KEY_ID" 2>/dev/null || true
echo "LocalStack KMS key alias/social-chat ready"
