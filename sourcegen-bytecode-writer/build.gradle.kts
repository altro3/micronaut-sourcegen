plugins {
    id("io.micronaut.build.internal.sourcegen-module")
}

repositories {
    maven(
        url = uri("https://www.jetbrains.com/intellij-repository/releases/")
    )
}

dependencies {
    api(projects.sourcegenModel)
    api(libs.asm)
    api(libs.asm.commons)
    api(libs.asm.util)

    compileOnly(mn.micronaut.core.processor)

    testImplementation(mn.micronaut.core.processor)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(libs.intellij.java.decompiler)
    testImplementation(projects.testSuiteCustomGenerators)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
