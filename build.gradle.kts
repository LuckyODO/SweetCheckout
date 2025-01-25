plugins {
    id ("com.github.johnrengelman.shadow") version "7.0.0" apply false
}
allprojects {
    group = "top.mrxiaom.sweet.checkout"
    version = "1.0.0"
}
subprojects {
    apply(plugin = "java")
    val targetJavaVersion = 8
    
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://jitpack.io")
        maven("https://repo.rosewooddev.io/repository/public/")
        maven("https://s01.oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/groups/public/")
    }

    project.extensions.configure<JavaPluginExtension> {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)
        if (JavaVersion.current() < javaVersion) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.isWarnings = false
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
}
