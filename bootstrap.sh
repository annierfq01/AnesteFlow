#!/usr/bin/env bash
# Descarga el gradle-wrapper.jar si no existe o está vacío
set -e
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_VERSION="8.9"

if file "$WRAPPER_JAR" 2>/dev/null | grep -q "Zip"; then
  echo "✓ gradle-wrapper.jar ya es válido"
  exit 0
fi

echo "Descargando gradle-wrapper.jar v${WRAPPER_VERSION}..."
curl -sL \
  "https://services.gradle.org/distributions/gradle-${WRAPPER_VERSION}-bin.zip" \
  -o /tmp/gradle-dist.zip

echo "Extrayendo jar..."
unzip -l /tmp/gradle-dist.zip | grep "gradle-wrapper" | awk '{print $4}' | head -1 | \
  xargs -I{} unzip -j /tmp/gradle-dist.zip {} -d /tmp/ 2>/dev/null

cp /tmp/gradle-wrapper*.jar "$WRAPPER_JAR"
echo "✓ gradle-wrapper.jar instalado ($(du -h $WRAPPER_JAR | cut -f1))"
