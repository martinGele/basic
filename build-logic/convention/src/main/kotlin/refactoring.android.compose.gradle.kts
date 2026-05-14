import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

extensions.findByType(ApplicationExtension::class)?.apply {
    buildFeatures {
        compose = true
    }
}
extensions.findByType(LibraryExtension::class)?.apply {
    buildFeatures {
        compose = true
    }
}

dependencies {
    add("implementation", platform(libs.androidx.compose.bom))
    add("implementation", libs.androidx.compose.ui)
    add("implementation", libs.androidx.compose.ui.graphics)
    add("implementation", libs.androidx.compose.ui.tooling.preview)
    add("implementation", libs.androidx.compose.material3)
    add("debugImplementation", libs.androidx.compose.ui.tooling)
    add("debugImplementation", libs.androidx.compose.ui.test.manifest)
    add("androidTestImplementation", platform(libs.androidx.compose.bom))
    add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
}
