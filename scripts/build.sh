#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v mvn >/dev/null || { echo 'Maven is required to build. Install Maven 3.8+ and JDK 17+.' >&2; exit 1; }
mvn clean package "$@"
mkdir -p releases
cp target/placesure.jar releases/placesure.jar
printf '\nBuilt releases/placesure.jar. Run: ./scripts/run.sh --seed-demo\n'
