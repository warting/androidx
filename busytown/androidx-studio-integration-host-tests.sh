#!/bin/bash
set -e

echo "Starting $0 at $(date)"

BUILD_SCRIPT="impl/build-studio-and-androidx.sh"
HOST_TEST_TASKS="test"
STUDIO_INTEGRATION_PARAMS="--ci"

"$(dirname "$0")/impl/host_test_common_test_runner.sh" "$BUILD_SCRIPT" "$TASKS" "$STUDIO_INTEGRATION_PARAMS" "$@"

echo "Completing $0 at $(date)"
