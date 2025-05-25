plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    implementation(libs.clikt)

    implementation(project(":core"))
    implementation(project(":lex"))
    implementation(project(":parser"))
    implementation(project(":sema"))
}

private val driverClass = "koc.driver.Kocpiler"

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = driverClass
}

tasks.named<JavaExec>("run") {
    workingDir = project.rootDir
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = driverClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") { it/*.toString()*/.name }
    }
}