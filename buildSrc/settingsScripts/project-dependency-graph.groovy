/*
 * Copyright 2023 The Android Open Source Project
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

import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.initialization.Settings

import javax.annotation.Nullable
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Tracks Gradle projects and their dependencies and provides functionality to get a subset of
 * projects with their dependencies.
 *
 * This class is shared between the main repository and the playground plugin (github).
 */
class ProjectDependencyGraph {
    private Settings settings;
    private boolean isPlayground;
    private boolean constraintsEnabled
    /**
     * A map of project path to a set of project paths referenced directly by this project.
     */
    private Map<String, Set<String>> projectReferences = new HashMap<String, Set<String>>()

    /**
     * A map of project path to a set of project paths that directly depend on the key project.
     */
    private Map<String, Set<String>> projectConsumers = new HashMap<String, Set<String>>()

    private Set<String> publishedLibraryProjects = new HashSet<>()

    /**
     * A map of all project paths to their project directory.
     */
    private Map<String, File> allProjects = new HashMap<String, File>()

    ProjectDependencyGraph(Settings settings, boolean isPlayground, boolean constraintsEnabled) {
        this.settings = settings
        this.isPlayground = isPlayground
        this.constraintsEnabled = constraintsEnabled
    }

    Set<String> allProjectPaths() {
        return allProjects.keySet()
    }

    Map<String, Set<String>> allProjectConsumers() {
        return projectConsumers
    }

    /**
     * Adds the given pair to the list of known projects
     *
     * @param projectPath Gradle project path
     * @param projectDir Gradle project directory
     */
    void addToAllProjects(String projectPath, File projectDir) {
        Set<String> cached = projectReferences.get(projectPath)
        if (cached != null) {
            return
        }
        allProjects[projectPath] = projectDir
        Set<String> parsedDependencies = extractReferencesFromBuildFile(projectPath, projectDir)
        projectReferences[projectPath] = parsedDependencies
        parsedDependencies.forEach { dependency ->
            def reverseLookupSet = projectConsumers[dependency] ?: new HashSet<String>()
            reverseLookupSet.add(projectPath)
            projectConsumers[dependency] = reverseLookupSet
        }
    }

    /**
     * Returns a set of project path that includes the given `projectPath` as well as any other project
     * that directly or indirectly depends on `projectPath`
     */
    Set<String> findAllProjectsDependingOn(String projectPath) {
        Set<String> result = new HashSet<String>()
        ArrayDeque<String> toBeTraversed = new ArrayDeque<String>()
        toBeTraversed.add(projectPath)
        while (toBeTraversed.size() > 0) {
            def path = toBeTraversed.removeFirst()
            if (result.add(path)) {
                def dependants = projectConsumers[path]
                if (dependants != null) {
                    toBeTraversed.addAll(dependants)
                }
            }
        }
        return result
    }

    /**
     * Returns a list of (projectPath -> projectDir) tuples that include the given filteredProjects
     * and all of their dependencies (including nested dependencies)
     *
     * @param projectPaths The projects which must be included
     * @return The list of project paths and their directories as a tuple
     */
    List<Tuple2<String, File>> getAllProjectsWithDependencies(Set<String> projectPaths) {
        Set<String> result = new HashSet<String>()
        projectPaths.forEach {
            addReferences(it, result)
        }
        return result.collect { projectPath ->
            File projectDir = allProjects[projectPath]
            if (projectDir == null) {
                throw new GradleException("cannot find project directory for $projectPath")
            }
            new Tuple2(projectPath, projectDir)
        }
    }

    private void addReferences(String projectPath, Set<String> target) {
        if (target.contains(projectPath)) {
            return // already added
        }
        target.add(projectPath)
        Set<String> allReferences = getOutgoingReferences(projectPath)
        allReferences.forEach {
            addReferences(it, target)
        }
    }

    private Set<String> getOutgoingReferences(String projectPath) {
        def references = projectReferences[projectPath]
        if (references == null) {
            throw new GradleException("Project $projectPath does not exist.\n" +
                    "Please check the build.gradle file for your $projectPath project " +
                    "and update the project dependencies.")
        }
        def implicitReferences = findImplicitReferences(projectPath)
        def constraintReferences = findConstraintReferences(projectPath)
        return references + implicitReferences + constraintReferences
    }

    /**
     * Finds sibling projects that will be needed for constraint publishing. This is necessary
     * for when androidx.constraints=true is set and automatic atomic group constraints are enabled
     * meaning that :foo:foo and :foo:foo-bar projects are required even if they don't reference
     * each other.
     *
     * @param projectPath The project path whose sibling projects will be found
     * @return The set of sibling projects that will be needed for constraint publishing
     */
    private Set<String> findConstraintReferences(String projectPath) {
        Set<String> constraintReferences = new HashSet()
        if (!constraintsEnabled || !publishedLibraryProjects.contains(projectPath)) return constraintReferences
        def lastColon = projectPath.lastIndexOf(":")
        if (lastColon == -1) return constraintReferences
        allProjectPaths().forEach {
            if (it.startsWith(projectPath.substring(0, lastColon)) && publishedLibraryProjects.contains(it)) {
                constraintReferences.add(it)
            }
        }
        return constraintReferences
    }


    /**
     * Finds implicit dependencies of a project. This is necessary because when ":foo:bar" is
     * included in Gradle, it automatically also loads ":foo".
     * @param projectPath The project path whose implicit dependencies will be found
     *
     * @return The set of implicit dependencies for projectPath
     */
    private Set<String> findImplicitReferences(String projectPath) {
        Set<String> implicitReferences = new HashSet()
        for (reference in projectReferences[projectPath]) {
            String[] segments = reference.substring(1).split(":")
            String subpath = ""
            for (int i = 0; i < segments.length; i++) {
                subpath += ":" + segments[i]
                if (allProjects.containsKey(subpath)) {
                    implicitReferences.add(subpath)
                }
            }
        }
        return implicitReferences
    }

    /**
     * Find dependency paths from sourceProjectPaths to targetProjectPath.
     * @param sourceProjectPaths The project paths whose outgoing references will be traversed
     * @param targetProjectPath The target project path that will be checked for reachability
     * @return A list of strings where each item is a representation of a dependency path, in
     *        the form of: "path1 -> path2 -> path3". This is intended to be human readable.
     */
    List<String> findPathsBetween(Set<String> sourceProjectPaths, String targetProjectPath) {
        return sourceProjectPaths.collect {
            findPathsBetween(it, targetProjectPath, sourceProjectPaths - it)
        } - null
    }

    @Nullable
    String findPathsBetween(
            String sourceProjectPath, String targetProjectPath, Set<String> visited
    ) {
        if (sourceProjectPath == targetProjectPath) {
            return targetProjectPath
        }
        if (visited.contains(sourceProjectPath)) {
            return null
        }
        Set<String> myReferences = getOutgoingReferences(sourceProjectPath)
        Set<String> subExclude = visited + sourceProjectPath
        for (String dependency : myReferences) {
            String path = findPathsBetween(dependency, targetProjectPath, subExclude)
            if (path != null) {
                return "$sourceProjectPath -> $path"
            }
        }
        return null
    }

    /**
     * Parses the build file in the given projectDir to find its project dependencies.
     *
     * @param projectPath The Gradle projectPath of the project
     * @param projectDir The project directory on the file system
     * @return Set of project paths that are dependent by the given project
     */
    private Set<String> extractReferencesFromBuildFile(String projectPath, File projectDir) {
        File buildFile = buildFileNames.findResult { buildFileName ->
            File candidate = new File(projectDir, buildFileName)
            return candidate.exists() ? candidate : null
        }
        Set<String> links = new HashSet<String>()
        if (buildFile != null) {
            def buildGradleProperty = settings.services.get(ObjectFactory).fileProperty()
                    .fileValue(buildFile)
            def contents = settings.providers.fileContents(buildGradleProperty)
                    .getAsText().get()
            for (line in contents.lines()) {
                Matcher m = projectReferencePattern.matcher(line)
                if (m.find()) {
                    // ignore projectOrArtifact dependencies in playground
                    def projectOrArtifact = m.group(1) == "projectOrArtifact"
                    if (!isPlayground || !projectOrArtifact) {
                        links.add(m.group("name"))
                    }
                }
                if (multilineProjectReference.matcher(line).find()) {
                    throw new IllegalStateException(
                            "Multi-line project() references are not supported." +
                                    "Please fix $buildFile.absolutePath"
                    )
                }
                Matcher targetProject = testProjectTarget.matcher(line)
                if (targetProject.find()) {
                    links.add(targetProject.group(1))
                }
                Matcher matcherInspection = inspection.matcher(line)
                if (matcherInspection && !isPlayground) {
                    // inspection is not supported in playground
                    links.add(matcherInspection.group(1))
                }
                if (composePlugin.matcher(line).find()) {
                    links.add(":compose:lint:internal-lint-checks")
                }
                if (publishedLibrary.matcher(line).find()) {
                    publishedLibraryProjects.add(projectPath)
                }
                Matcher publishProject = publishProjectReference.matcher(line)
                if (publishProject.find()) {
                    links.add(publishProject.group(1))
                }

                // Validate certain common DSL setters
                validateAndroidDsl(line, buildFile)
            }
        } else if (!projectDir.exists()) {
            // Remove file existence checking when https://github.com/gradle/gradle/issues/25531 is
            // fixed.
            // This option is supported so that development/simplify_build_failure.sh can try
            // deleting entire projects at once to identify the cause of a build failure
            if (System.getenv("ALLOW_MISSING_PROJECTS") == null) {
                throw new Exception("Path " + buildFile + " does not exist;" +
                        "cannot include project " + projectPath + " ($projectDir)")
            }
        }
        return links
    }

    private static void validateAndroidDsl(String line, File buildFile) {
        Matcher matcherCompileSdk = compileSdk.matcher(line)
        if (matcherCompileSdk) {
            String middlePart = matcherCompileSdk.group(1)
            if (middlePart !in [" = ", "Extension = "]) {
                String compileSdkValue = matcherCompileSdk.group(2)
                if (middlePart.contains("Extension")) {
                    throw new Exception("Invalid way to set compileSdkExtension " +
                            "in $buildFile.absolutePath.\n" +
                            "It is compileSdk$middlePart$compileSdkValue, " +
                            "but should be compileSdkExtension = $compileSdkValue"
                    )
                } else {
                    throw new Exception("Invalid way to set compileSdk " +
                            "in $buildFile.absolutePath.\n" +
                            "It is compileSdk$middlePart$compileSdkValue, " +
                            "but should be compileSdk = $compileSdkValue"
                    )
                }
            }
        }
        Matcher matcherMinSdk = minSdk.matcher(line)
        if (matcherMinSdk) {
            String middlePart = matcherMinSdk.group(1)
            if (middlePart !in [" = "]) {
                throw new Exception("Invalid way to set minSdk " +
                        "in $buildFile.absolutePath.\n" +
                        "It is minSdk$middlePart${matcherMinSdk.group(2)}, " +
                        "but should be minSdk = ${matcherMinSdk.group(2)}"
                )
            }
        }
        Matcher matcherNamespace = namespace.matcher(line)
        if (matcherNamespace) {
            String middlePart = matcherNamespace.group(1)
            String quotes = matcherNamespace.group(2)
            if (middlePart != "= " || quotes != "\"") {
                String namespaceValue = matcherNamespace.group(3)
                throw new Exception("Invalid way to set namespace " +
                        "in $buildFile.absolutePath.\n" +
                        "It is namespace $middlePart$quotes$namespaceValue$quotes, " +
                        "but should be namespace = \"$namespaceValue\""
                )
            }
        }
    }

    private static Pattern projectReferencePattern = Pattern.compile(
            "(project|projectOrArtifact)\\((path: )?[\"'](?<name>\\S*)[\"'](, configuration: .*)?\\)"
    )
    private static Pattern testProjectTarget = Pattern.compile("targetProjectPath = \"(.*)\"")
    private static Pattern multilineProjectReference = Pattern.compile("project\\(\$")
    private static Pattern inspection = Pattern.compile("packageInspector\\(project, \"(.*)\"\\)")
    private static Pattern composePlugin = Pattern.compile("id\\(\"AndroidXComposePlugin\"\\)")
    private static Pattern publishedLibrary = Pattern.compile(
            "(type = SoftwareType\\.(PUBLISHED_LIBRARY|GRADLE_PLUGIN|ANNOTATION_PROCESSOR|ANNOTATION_PROCESSOR_UTILS|OTHER_CODE_PROCESSOR" +
                    "|STANDALONE_PUBLISHED_LINT|PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS" +
                    "|PUBLISHED_TEST_LIBRARY|PUBLISHED_PROTO_LIBRARY|PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY)|" +
                    "publish = Publish\\.SNAPSHOT_AND_RELEASE)"
    )
    private static Pattern publishProjectReference = Pattern.compile("\"(.*):publish\"")
    private static Pattern compileSdk = Pattern.compile("compileSdk(\\D*)([0-9]+)\$")
    private static Pattern minSdk = Pattern.compile("minSdk(\\D*)([0-9]+)\$")
    private static Pattern namespace = Pattern.compile("namespace (.*)(['\"])([^'^\"]*)['\"]\$")
    private static List<String> buildFileNames = ["build.gradle", "build.gradle.kts"]
}

ProjectDependencyGraph createProjectDependencyGraph(Settings settings, boolean constraintsEnabled) {
    return new ProjectDependencyGraph(settings, false /** isPlayground **/, constraintsEnabled)
}
// export a function to create ProjectDependencyGraph
ext.createProjectDependencyGraph = this.&createProjectDependencyGraph

ext.allProjectsConsumers = { ProjectDependencyGraph graph ->
    graph.allProjectConsumers()
}
