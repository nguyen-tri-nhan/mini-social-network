#!/bin/bash
# Quick smoke test for the full flow
# Run infra first: cd ../infra && docker compose -f docker-compose.dev.yml up -d

BASE_AUTH="http://localhost:8081/api/auth"
BASE_ARTICLES="http://localhost:8083/api/articles"
BASE_COMMENTS="http://localhost:8084/api"
BASE_NOTI="http://localhost:8085/api/notifications"

echo "=== 1. Signup ==="
curl -s -X POST "$BASE_AUTH/signup" \
  -H "Content-Type: application/json" \
  -d '{"username":"nhan","email":"nhan@test.com","password":"secret123","firstname":"Nhan","lastname":"Nguyen"}' | jq .

echo ""
echo "=== 2. Signin ==="
TOKEN=$(curl -s -X POST "$BASE_AUTH/signin" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"nhan","password":"secret123"}' | jq -r '.data.accessToken')
echo "Token: ${TOKEN:0:40}..."

echo ""
echo "=== 3. Create article ==="
ARTICLE_ID=$(curl -s -X POST "$BASE_ARTICLES" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"description":"Hello from load test!"}' | jq -r '.data.id')
echo "Article ID: $ARTICLE_ID"

echo ""
echo "=== 4. Get feed ==="
curl -s "$BASE_ARTICLES?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.items | length'

echo ""
echo "=== 5. Add comment ==="
curl -s -X POST "$BASE_COMMENTS/articles/$ARTICLE_ID/comments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"description":"Great post!"}' | jq .

echo ""
echo "=== 6. Vote article ==="
curl -s -X POST "$BASE_COMMENTS/articles/$ARTICLE_ID/vote" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"value":1}' | jq .

echo ""
echo "=== 7. Check notifications ==="
sleep 2  # wait for Kafka consumer
curl -s "$BASE_NOTI/unread-count" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "=== Done ==="
