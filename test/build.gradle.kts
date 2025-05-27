plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":driver"))
    implementation(project(":lex"))
    implementation(project(":parser"))
    implementation(project(":sema"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    // Use the built-in JUnit support of Gradle.
    useJUnitPlatform()
}