plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.11"
}

val coverageExclusions = listOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.class",
    "**/Manifest*.*",
    "**/*_Hilt*.class",
    "**/Hilt_*.class",
    "**/*_Factory.class",
    "**/*_Provide*Factory*.class",
    "**/*Module_*Factory.class",
    "**/di/**",
    "**/*ComposableSingletons*.*",
    "**/ComposableSingletons*.*",
)

val coverageClassDirs = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
    exclude(coverageExclusions)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
