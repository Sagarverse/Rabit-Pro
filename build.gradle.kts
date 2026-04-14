// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.register("tests") {
    group = "verification"
    description = "Runs all unit tests for the project."
    dependsOn(":app:test")
}