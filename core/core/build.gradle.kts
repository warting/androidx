/**
 * This file was created using the `create_project.py` script located in the
 * `<AndroidX root>/development/project-creator` directory.
 *
 * Please use that script when creating a new project, rather than copying an existing project and
 * modifying its settings.
 */
import androidx.build.SoftwareType
import androidx.stableaidl.StableAidlBuildTypeDslExtension

plugins {
    id("AndroidXPlugin")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("androidx.stableaidl")
}

dependencies {
    // Atomically versioned.
    constraints {
        implementation(project(":core:core-ktx"))
        implementation(project(":core:core-testing"))
    }

    api("androidx.annotation:annotation:1.8.1")
    api("androidx.annotation:annotation-experimental:1.4.1")
    api("androidx.lifecycle:lifecycle-runtime:2.6.2")
    api("androidx.versionedparcelable:versionedparcelable:1.1.1")
    api("androidx.core:core-viewtree:1.0.0")
    api("androidx.lifecycle:lifecycle-common:2.6.2")

    api(libs.guavaListenableFuture)
    api(libs.jspecify)
    api(libs.kotlinCoroutinesCore)
    api(libs.kotlinStdlib)

    implementation("androidx.collection:collection:1.4.2")
    implementation("androidx.concurrent:concurrent-futures:1.0.0")
    implementation("androidx.interpolator:interpolator:1.0.0")
    implementation("androidx.tracing:tracing:1.2.0")

    // We don't ship this as a public artifact, so it must remain a project-type dependency.
    annotationProcessor(project(":versionedparcelable:versionedparcelable-compiler"))

    androidTestImplementation("androidx.activity:activity:1.0.0")
    androidTestImplementation("androidx.appcompat:appcompat:1.1.0")
    androidTestImplementation("androidx.lifecycle:lifecycle-common:2.9.0")
    androidTestImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.6.2")
    androidTestImplementation(project(":internal-testutils-runtime"))
    androidTestImplementation(project(":internal-testutils-lifecycle"))
    androidTestImplementation(project(":internal-testutils-fonts"))
    androidTestImplementation(project(":internal-testutils-mockito"))

    androidTestImplementation("org.hamcrest:hamcrest-library:1.3")
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlinStdlib)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.testCore)
    androidTestImplementation(libs.testRunner)
    androidTestImplementation(libs.testRules)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(libs.mockitoCore)
    androidTestImplementation(libs.testUiautomator)
    androidTestImplementation(libs.findbugs)
    androidTestImplementation(libs.guavaAndroid)
    androidTestImplementation(libs.hamcrestCore)
    androidTestImplementation(libs.kotlinCoroutinesCore)
    androidTestImplementation(libs.testMonitor)

    // Including both dexmakers allows support for all API levels plus final mocking support on
    // API 28+. The implementation is swapped based on the finality of the mock type. This
    // delegation is handled manually inside androidx.core.util.mockito.CustomMockMaker.
    androidTestImplementation(libs.dexmakerMockito)
    androidTestImplementation(libs.dexmakerMockitoInline)

    testImplementation(libs.junit)
    testImplementation(libs.testExtJunit)
    testImplementation(libs.testCore)
    testImplementation(libs.testRunner)
    testImplementation(libs.truth)
}

android {
    compileSdk = 36
    buildFeatures {
        aidl = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    androidResources {
        noCompress += "ttf"
    }
    buildTypes.configureEach {
        consumerProguardFiles("proguard-rules.pro")
        extensions.configure<StableAidlBuildTypeDslExtension> {
            version = 1
        }
    }
    packaging {
        resources {
            // Drop the file from external dependencies, preferring the local file in androidTest
            pickFirsts += "mockito-extensions/org.mockito.plugins.MockMaker"
            pickFirsts += "mockito-extensions/org.mockito.plugins.StackTraceCleanerProvider"
        }
    }
    namespace = "androidx.core"
}

androidx {
    name = "Core"
    type = SoftwareType.PUBLISHED_LIBRARY
    mavenVersion = LibraryVersions["CORE"]
    inceptionYear = "2015"
    description = "Provides backward-compatible implementations of Android platform APIs and " +
            "features."
    failOnDeprecationWarnings = false
    enableRobolectric()
    samples(project(":core:core:core-samples"))
}
