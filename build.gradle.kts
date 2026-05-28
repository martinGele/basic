// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.cyclonedx) apply false
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                "ktlint_standard_function-naming" to "disabled",
            ),
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(mapOf("indent_size" to "4", "continuation_indent_size" to "4"))
    }
}
