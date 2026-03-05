#!/bin/bash
set -e

echo "Deploying ServiceHub..."

# Build and start all services
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# Wait for API health check
echo "Waiting for API to become healthy..."
for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "API is healthy!"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "ERROR: API health check failed after 30 attempts"
        docker-compose logs api
        exit 1
    fi
    sleep 2
done

echo ""
echo "Deployment complete!"
echo "  Frontend:  http://localhost:3000"
echo "  API:       http://localhost:8080/api/v1"
echo "  Swagger:   http://localhost:8080/api/docs"
echo "  MailHog:   http://localhost:8025"
