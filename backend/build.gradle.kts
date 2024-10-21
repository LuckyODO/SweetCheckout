plugins {
    id ("com.github.johnrengelman.shadow")
}

val targetJavaVersion = 8
val shadowGroup = "top.mrxiaom.sweet.checkout.backend.libs"

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.jline:jline-terminal-jansi:3.21.0")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")

    implementation("commons-io:commons-io:2.17.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("top.mrxiaom:Java-WebSocket:1.5.7")
    implementation("com.zaxxer:HikariCP:4.0.3") { isTransitive = false }
    implementation("org.jetbrains:annotations:21.0.0")
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation(project(":packets"))
}

fun Jar.setupManifest() {
    manifest {
        attributes(
            "main-class" to "top.mrxiaom.sweet.checkout.backend.ConsoleMain"
        )
    }
}
tasks {
    shadowJar {
        archiveClassifier.set("")
        setupManifest()
    }
    build {
        dependsOn(shadowJar)
    }
    create<JavaExec>("runConsole") {
        group = "minecraft"
        mainClass.set("top.mrxiaom.sweet.checkout.backend.ConsoleMain")
        classpath = sourceSets.main.get().runtimeClasspath
        workingDir = File(projectDir, "run").also { it.mkdirs() }
        this.standardInput = System.`in`

        defaultCharacterEncoding = "UTF-8"
        systemProperties(
            "file.encoding" to "UTF-8",
            "sun.stdout.encoding" to "UTF-8",
            "sun.stderr.encoding" to "UTF-8"
        )
    }
}
