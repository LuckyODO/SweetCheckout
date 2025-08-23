plugins {
    id("com.github.gmazzo.buildconfig")
}
java.withJavadocJar()
val libraries = arrayListOf<String>()
fun DependencyHandlerScope.library(dependencyNotation: String) {
    compileOnly(dependencyNotation)
    libraries.add(dependencyNotation)
}
dependencies {
    val dependencies: Map<String, Boolean> by project.extra
    for ((dependency, _) in dependencies) {
        compileOnly(dependency)
    }
    val libraries: List<String> by project.extra
    for (lib in libraries) {
        library(lib)
    }
    compileOnly(project(":plugin:nms"))
    compileOnly(project(":packets"))
}

buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.sweet.checkout")

    val librariesVararg = libraries.joinToString(", ") { "\"$it\"" }

    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("java.time.Instant", "BUILD_TIME", "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)")
    buildConfigField("String[]", "LIBRARIES", "new String[] { $librariesVararg }")
}
