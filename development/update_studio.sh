#!/bin/bash

function echoAndDo() {
  echo "$@"
  eval "$@"
}

# Substitute for `sed -i` because different versions of sed differ in how to supply the argument for -i.
function sedInPlace() {
  TEMP_FILE=".sedOutput.tmp"
  sed "$1" $2 > $TEMP_FILE
  cat $TEMP_FILE > $2
  rm $TEMP_FILE
}

# Versions that the user should update when running this script
echo Getting Studio version and link
AGP_VERSION=${1:-8.12.0-alpha08}
STUDIO_VERSION_STRING=${2:-"Android Studio Narwhal Feature Drop | 2025.1.2 Canary 8"}

# Get studio version number from version name
STUDIO_IFRAME_LINK=`curl "https://developer.android.com/studio/archive.html" | grep "<iframe " | sed "s/.* src=\"\([^\"]*\)\".*/\1/g"`
echo iframe link $STUDIO_IFRAME_LINK
STUDIO_IFRAME_REDIRECT=`curl -s $STUDIO_IFRAME_LINK | grep href | sed 's/.*href="\([^"]*\)".*/\1/g'`
echo iframe redirect $STUDIO_IFRAME_REDIRECT
STUDIO_LINK=`curl -s $STUDIO_IFRAME_REDIRECT | grep -C30 "$STUDIO_VERSION_STRING" | grep Linux | tail -n 1 | sed 's/.*a href="\(.*\).*"/\1/g' | sed 's/>.*//'`
echo STUDIO_LINK: $STUDIO_LINK
if [ "$STUDIO_LINK" == "" ]; then
  echo "Error: STUDIO_LINK must not be empty. Open this script and look for parsing errors. Does studio version '$STUDIO_VERSION_STRING' exist?"
  exit 1
fi
STUDIO_VERSION=`echo $STUDIO_LINK | sed "s/.*ide-zips\/\(.*\)\/android-studio-.*/\1/g"`

# Update AGP
ARTIFACTS_TO_DOWNLOAD="com.android.tools.build:gradle:$AGP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="androidx.databinding:viewbinding:$AGP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin:$AGP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.android.settings:com.android.settings.gradle.plugin:$AGP_VERSION,"
AAPT2_VERSIONS=`curl "https://dl.google.com/dl/android/maven2/com/android/tools/build/group-index.xml" | grep aapt2-proto | sed 's/.*versions="\(.*\)"\/>/\1/g'`
AAPT2_VERSION=`echo $AAPT2_VERSIONS | sed "s/.*\($AGP_VERSION-[0-9]*\).*/\1/g"`
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.build:aapt2:$AAPT2_VERSION:linux,"
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.build:aapt2:$AAPT2_VERSION:osx,"
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.build:aapt2:$AAPT2_VERSION,"
LINT_VERSIONS=`curl "https://dl.google.com/dl/android/maven2/com/android/tools/lint/group-index.xml" | grep lint | sed 's/.*versions="\(.*\)"\/>/\1/g'`
LINT_MINOR_VERSION=`echo $AGP_VERSION | sed -r 's/[0-9]+\.(.*)/\1/g'`
LINT_VERSION=`echo $LINT_VERSIONS | sed -r "s/.*[,| ]([0-9]+\.$LINT_MINOR_VERSION).*/\1/g"`
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.lint:lint:$LINT_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.lint:lint-tests:$LINT_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.android.tools.lint:lint-gradle:$LINT_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.android.tools:ninepatch:$LINT_VERSION,"

# Update libs.versions.toml
echo Updating dependency versions
sedInPlace "s/androidGradlePlugin = \".*/androidGradlePlugin = \"$AGP_VERSION\"/g" gradle/libs.versions.toml
sedInPlace "s/androidLint = \".*/androidLint = \"$LINT_VERSION\"/g" gradle/libs.versions.toml
sedInPlace "s/androidStudio = \".*/androidStudio = \"$STUDIO_VERSION\"/g" gradle/libs.versions.toml

# update settings.gradle -- don't match the line with :$agpOverride
sedInPlace "s/com.android.settings:com.android.settings.gradle.plugin:[^$][0-9a-z\.\-]*/com.android.settings:com.android.settings.gradle.plugin:$AGP_VERSION/g" settings.gradle

# Pull all UTP artifacts for ADT version
ADT_VERSION=${3:-$LINT_VERSION}
while read line
    do
    ARTIFACT=`echo $line | sed -r 's/<([[:lower:]-]+).*/\1/g'`
    ARTIFACTS_TO_DOWNLOAD+="com.android.tools.utp:$ARTIFACT:$ADT_VERSION,"
  done < <(curl -sL "https://dl.google.com/android/maven2/com/android/tools/utp/group-index.xml" \
             | tail -n +3 \
             | sed '$ d') # Remove the last line

ATP_VERSION=${4:-0.0.9-alpha03}
ARTIFACTS_TO_DOWNLOAD+="com.google.testing.platform:android-test-plugin:$ATP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.google.testing.platform:launcher:$ATP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.google.testing.platform:android-driver-instrumentation:$ATP_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="com.google.testing.platform:core:$ATP_VERSION"

# Download all the artifacts
echoAndDo ./development/importMaven/importMaven.sh "$ARTIFACTS_TO_DOWNLOAD"
