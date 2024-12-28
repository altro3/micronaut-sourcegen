plugins {
    id("io.micronaut.build.internal.sourcegen-module")
    alias(mn.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.sourcegenGenerator)
    implementation(libs.managed.kotlinpoet)
    implementation(libs.managed.kotlinpoet.javapoet)
}
