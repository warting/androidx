#!/bin/bash
set -e
cd "$(dirname "$0")"

# Builds all projects that support KMP
# Must be run on Mac
export ANDROIDX_PROJECTS=KMP

# This target is for testing that clean builds work correctly
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

impl/build.sh createAllArchives

# run a separate createAllArchives task to prepare a repository
# folder in DIST.
# This cannot be merged with the buildOnServer run because
# snapshot version is not a proper release version.
DIST_DIR=$DIST_DIR/snapshots SNAPSHOT=true impl/build.sh createAllArchives
