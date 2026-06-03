#!/bin/bash
# Run once to initialise the Gradle wrapper
set -e

echo ">>> Generating Gradle wrapper..."
gradle wrapper --gradle-version 8.8 --distribution-type bin

echo ">>> Generating dev RSA key pair for JWT..."
if [ ! -f dev-private.pem ]; then
  openssl genrsa 2048 2>/dev/null | openssl pkcs8 -topk8 -nocrypt -out dev-private.pem
  openssl rsa -in dev-private.pem -pubout -out dev-public.pem 2>/dev/null
fi

echo ">>> Copying public key to all services..."
for svc in auth-service user-service post-service interaction-service notification-service; do
  cp dev-public.pem $svc/src/main/resources/public-key.pem
done
cp dev-private.pem auth-service/src/main/resources/private-key.pem

echo ">>> Setup complete!"
echo ""
echo "Start infra:  cd ../infra && docker compose -f docker-compose.dev.yml up -d"
echo "Run service:  ./gradlew :auth-service:quarkusDev"
