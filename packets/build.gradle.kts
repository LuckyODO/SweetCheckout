plugins {
    `maven-publish`
}
dependencies {
    compileOnly("com.google.code.gson:gson:2.10")
    implementation("org.jetbrains:annotations:21.0.0")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = rootProject.group.toString()
            artifactId = "packets"
            version = rootProject.version.toString()
        }
    }
}
