/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.LibraryVariant
import com.android.builder.core.BuilderConstants
import com.google.common.collect.ImmutableMap
import net.ltgt.gradle.errorprone.ErrorProneBasePlugin
import net.ltgt.gradle.errorprone.ErrorProneToolChain
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportLibraryPlugin implements Plugin<Project> {
    private static final String INSTRUMENTATION_RUNNER =
            "android.support.test.runner.AndroidJUnitRunner";

    @Override
    public void apply(Project project) {
        SupportLibraryExtension supportLibraryExtension =
                project.getExtensions().create("supportLibrary", SupportLibraryExtension, project);

        project.apply(ImmutableMap.of("plugin", "com.android.library"));
        project.apply(ImmutableMap.of("plugin", ErrorProneBasePlugin.class));

        LibraryExtension library =
                project.getExtensions().findByType(LibraryExtension.class);

        library.compileSdkVersion project.ext.currentSdk

        // We use a non-standard manifest path.
        AndroidSourceSet mainSet = library.getSourceSets().findByName("main");
        mainSet.manifest.srcFile 'AndroidManifest.xml'

        library.defaultConfig {
            // Update the version meta-data in each Manifest.
            addManifestPlaceholders(["support-version": project.rootProject.ext.supportVersion])

            // Set test related options.
            testInstrumentationRunner INSTRUMENTATION_RUNNER
        }

        // We use a non-standard test directory structure.
        library.sourceSets.androidTest {
            root 'tests'
            java.srcDir 'tests/src'
            res.srcDir 'tests/res'
            manifest.srcFile 'tests/AndroidManifest.xml'
        }

        // Always lint check NewApi as fatal.
        library.lintOptions {
            abortOnError true
            ignoreWarnings true

            // Write output directly to the console (and nowhere else).
            textOutput 'stderr'
            textReport true
            htmlReport false
            xmlReport false

            // Format output for convenience.
            explainIssues true
            noLines false
            quiet true

            // Always fail on NewApi.
            error 'NewApi'
        }

        if (project.rootProject.ext.usingFullSdk) {
            // Library projects don't run lint by default, so set up dependency.
            project.tasks.release.dependsOn project.tasks.lint
        }

        // Java 8 is only fully supported on API 24+ and not all Java 8 features are binary
        // compatible with API < 24, so use Java 7 for both source AND target.
        library.compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_7
            targetCompatibility JavaVersion.VERSION_1_7
        }

        // Create sources jar for release builds
        library.getLibraryVariants().all(new Action<LibraryVariant>() {
            @Override
            public void execute(LibraryVariant libraryVariant) {
                if (!libraryVariant.getBuildType().getName().equals(BuilderConstants.RELEASE)) {
                    return; // Skip non-release builds.
                }

                Jar sourceJar = project.getTasks().create("sourceJarRelease", Jar.class);
                sourceJar.setClassifier("sources");
                sourceJar.from(library.getSourceSets().findByName("main").getJava().getSrcDirs());
                project.getArtifacts().add("archives", sourceJar);
            }
        });

        // Set uploadArchives options.
        Upload uploadTask = (Upload) project.getTasks().getByName("uploadArchives");
        project.afterEvaluate {
            uploadTask.repositories {
                mavenDeployer {
                    repository(url: project.uri(project.rootProject.ext.supportRepoOut))
                }
            };
            uploadTask.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {
                @Override
                public void execute(MavenDeployer mavenDeployer) {
                    mavenDeployer.getPom().project {
                        name supportLibraryExtension.getName()
                        description supportLibraryExtension.getDescription()
                        url 'http://developer.android.com/tools/extras/support-library.html'
                        inceptionYear supportLibraryExtension.getInceptionYear()

                        licenses {
                            license {
                                name 'The Apache Software License, Version 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                                distribution 'repo'
                            }

                            supportLibraryExtension.getLicenses().each {
                                SupportLibraryExtension.License supportLicense ->
                                    license {
                                        name supportLicense.name
                                        url supportLicense.url
                                        distribution 'repo'
                                    }
                            }
                        }

                        scm {
                            url "http://source.android.com"
                            connection "scm:git:https://android.googlesource.com/platform/frameworks/support"
                        }
                        developers {
                            developer {
                                name 'The Android Open Source Project'
                            }
                        }
                    }
                }
            });
        }

        final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
        library.getBuildTypes().create("errorProne")
        library.getLibraryVariants().all(new Action<LibraryVariant>() {
            @Override
            void execute(LibraryVariant libraryVariant) {
                if (libraryVariant.getBuildType().getName().equals("errorProne")) {
                    libraryVariant.getJavaCompile().setToolChain(toolChain);

                    // TODO(aurimas): remove this once all these warnings are fixed.
                    libraryVariant.getJavaCompile().options.compilerArgs += [
                            '-Xep:MissingCasesInEnumSwitch:WARN',
                            '-Xep:TypeParameterUnusedInFormals:WARN',
                            '-Xep:MissingOverride:WARN',
                            '-Xep:ArrayToString:WARN',
                            '-Xep:MislabeledAndroidString:WARN',
                            '-Xep:SelfEquals:WARN',
                            '-Xep:RectIntersectReturnValueIgnored:WARN',
                            '-Xep:FallThrough:WARN',
                            '-XDcompilePolicy=simple' // Workaround for b/36098770
                    ]
                }
            }
        })
    }
}
