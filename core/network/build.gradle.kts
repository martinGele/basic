plugins {
    id("refactoring.android.library")
    id("refactoring.android.hilt")
}

android {
    namespace = "com.refactoring.core.network"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
