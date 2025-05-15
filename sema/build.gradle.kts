plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":lex"))
    implementation(project(":parser"))
    implementation(project(":utils"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}