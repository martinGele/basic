plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    add("implementation", libs.hilt.android)
    add("ksp", libs.hilt.compiler)
}
