plugins {
    `maven-publish`
}

val targetJavaVersion = 8

dependencies {
    val dependencies: List<String> by project.extra
    for (dependency in dependencies) {
        compileOnly(dependency)
    }

    compileOnly(project(":packets"))
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = rootProject.group.toString()
            artifactId = "backend"
            version = rootProject.version.toString()
        }
    }
}
