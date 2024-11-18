#!/bin/bash
#
# Copyright 2024 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# Merge kzip files (source files for the indexing pipeline) for the given configuration, and place
# the resulting all.kzip into $DIST_DIR.
# Most code from:
# https://cs.android.com/android/platform/superproject/main/+/main:build/soong/build_kzip.bash;

set -e

# Absolute path of the directory where this script lives
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

PREBUILTS_DIR=$SCRIPT_DIR/../../../../prebuilts

cd "$SCRIPT_DIR/../.."
if [ "$OUT_DIR" == "" ]; then
  OUT_DIR="../../out"
fi
mkdir -p "$OUT_DIR"
export OUT_DIR="$(cd $OUT_DIR && pwd)"
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"
export DIST_DIR="$DIST_DIR"


# If the SUPERPROJECT_REVISION is defined as a sha, use this as the default value
if [[ ${SUPERPROJECT_REVISION:-} =~ [0-9a-f]{40} ]]; then
  : ${KZIP_NAME:=${SUPERPROJECT_REVISION:-}}
fi

: ${KZIP_NAME:=${BUILD_NUMBER:-}}
: ${KZIP_NAME:=$(uuidgen)}


rm -rf $DIST_DIR/*.kzip
declare -r allkzip="$KZIP_NAME.kzip"
echo "Merging Kzips..."
"$PREBUILTS_DIR/build-tools/linux-x86/bin/merge_zips" "$DIST_DIR/$allkzip" @<(find "$OUT_DIR/androidx" -name '*.kzip')
