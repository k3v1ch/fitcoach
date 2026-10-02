#!/bin/sh
# certbot deploy-hook: после продления сертификата nginx перечитывает его.
docker exec fitcoach-edge-1 nginx -s reload 2>/dev/null || true
