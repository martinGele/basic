# The Android Build Setup That Pays Off at Scale: Convention Plugins, Done Right

> *Part 2 of a two-part series. [Part 1](#) built a full security and release pipeline for an Android app — all the way to a signed, attested Play release. This post is the foundation underneath it: taming your build with `build-logic` and convention plugins.*

In [Part 1](#) we built a security and release pipeline that signs an Android app, generates an SBOM, attaches SLSA provenance, and ships it to Google Play. But that whole pipeline leans on an assumption: that every module in the project is configured *consistently* — same SDK levels, same signing, same checks. This post is about how you actually guarantee that.

You've probably read a "stop copy-pasting your Gradle config" post before — there are plenty of good ones. This isn't quite that. We'll set up convention plugins, yes, but we won't stop at "DRY is nice." We'll follow the thread all the way out: what this actually does to **build times on a 25-module app**, how it changes day-to-day life on a **big team**, and how it becomes the consistent foundation the Part 1 pipeline depends on. The goal isn't a snippet you copy — it's a complete, runnable reference project you can clone and ship from.

So let me start where every one of these stories starts. Let me describe a project, and tell me if it sounds familiar.

You started with one module and one tidy `build.gradle.kts`. Life was good. Then the app grew, so you split it into modules — maybe a `:core:network`, a `:core:domain`, a `:core:presentation`. Each new module needed a build file, so you did the natural thing: you copied the one you already had and tweaked it.

Fast-forward a few months. You now have four or five build files that are *almost* identical. One module is on `compileSdk = 35`, another quietly drifted to `36`. Compose is set up slightly differently in two places. You need to bump the Java version, so you go on a find-and-replace safari across the repo — and you just *know* you missed one.

This isn't a discipline problem. It's a structural one. **You have one piece of knowledge — "this is how we build an Android module" — scattered across a dozen files.** Of course it drifts. Anything copied by hand drifts.

The good news: Gradle has a clean answer, and it's the same pattern Google uses in their [Now in Android](https://github.com/android/nowinandroid) sample. It's called **convention plugins** — and by the end of this post you'll know exactly how to set them up and *why* each piece exists.

---

## The big idea (in plain English)

Here's the mental model before we touch any code.

Instead of repeating build configuration in every module, you're going to write it **once**, package it as your own little Gradle plugin, and then each module just says *"give me the standard Android setup, please."*

Think of it like a recipe. Right now every module has its own scribbled, slightly-different copy of the same recipe. We're going to write the recipe down properly, once, and have every module point to it. Change the recipe in one place, and everyone gets the update.

When we're done, an entire module's build file can look like this:

```kotlin
// What app/build.gradle.kts looks like at the end
plugins {
    id("refactoring.android.application")
    id("refactoring.android.compose")
    id("refactoring.android.hilt")
    id("refactoring.jacoco")
}
```

No SDK numbers. No Java version. No Compose dependencies. Just a plain-language declaration of *what this module is*: an Android app, with Compose, with Hilt, with coverage checks. Everything else lives in the plugins. That's the whole goal — keep that picture in your head as we build toward it.

---

## Step 1 — Create a little "build" whose only job is to hold plugins

Here's the part that surprises people: to write your own Gradle plugins, the cleanest approach is to create a **separate, tiny Gradle build** that lives inside your project. By convention it's called `build-logic`. Its only purpose in life is to produce plugins for your *real* build to use.

Why a separate build instead of just dropping code in your root `build.gradle.kts`? Because it keeps your build logic properly compiled, reusable, and isolated. It can't accidentally tangle itself up with your app code, and Gradle can cache it nicely. Don't overthink it — just know that "plugins live in their own little build called `build-logic`" is the standard, blessed way to do this.

Here's the folder layout:

```
build-logic/
├── settings.gradle.kts
└── convention/
    ├── build.gradle.kts
    └── src/main/kotlin/
        ├── refactoring.android.application.gradle.kts
        ├── refactoring.android.compose.gradle.kts
        ├── refactoring.android.hilt.gradle.kts
        └── ...
```

The `build-logic/settings.gradle.kts` sets up where to download things from, and — this is the important bit — it **reuses the exact same version catalog as your main project**. (A version catalog, `libs.versions.toml`, is just the file where you list all your dependency versions in one place. If you're not using one yet, that's a great companion habit.)

```kotlin
// build-logic/settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            // point at the SAME catalog the app uses, one level up
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

Why does sharing the catalog matter? Because otherwise you'd have versions defined in two places — your app *and* your build logic — and we're literally here to stop "the same thing defined in two places." One catalog, one truth.

Now we tell the *main* project to include this little build. Just one line does it, in your **root** `settings.gradle.kts`:

```kotlin
// settings.gradle.kts (project root)
pluginManagement {
    includeBuild("build-logic")   // <-- the magic line
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

`includeBuild("build-logic")` is the handshake. It tells Gradle: *"Before you build my app, build this little thing first, and make its plugins available to me."* That's it — your project and your build logic are now connected.

---

## Step 2 — Set up the plugin module

Inside `build-logic` we have one module, `:convention`. Its `build.gradle.kts` does two jobs.

**First**, it applies a plugin called `kotlin-dsl`. This is the one that does the heavy lifting: it lets you write plugins as ordinary `.gradle.kts` files — the same Kotlin you already write in your build files. No weird boilerplate, no separate plugin-registration ceremony.

**Second**, it lists the plugins you want to *configure* as dependencies. Here's the subtle but important part: we declare them `compileOnly`. That means "I want to call this plugin's code to configure it, but I'm not going to bundle or ship it." Your app already brings the real Android/Kotlin/Hilt plugins; we just need their APIs available while we write our configuration.

```kotlin
// build-logic/convention/build.gradle.kts
plugins {
    `kotlin-dsl`
}

group = "com.refactoring.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
}
```

If `compileOnly` feels fuzzy, here's the one-liner: *we need these to write our plugins, but the app provides the real copies at runtime, so we don't ship our own.*

---

## Step 3 — Write your very first convention plugin

Now the fun part. A convention plugin is **just a `.gradle.kts` file** in `src/main/kotlin`. And here's the neat trick: the **filename becomes the plugin's id**. So a file named `refactoring.android.application.gradle.kts` automatically becomes a plugin you apply with `id("refactoring.android.application")`. No registration, no manifest. The name *is* the id.

Here's the plugin for Android app modules. Read it like a recipe — it applies the Android Gradle Plugin, then sets the SDK levels and Java version that *every* app module should share:

```kotlin
// refactoring.android.application.gradle.kts
import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
}

extensions.configure<ApplicationExtension> {
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

What's happening here? `extensions.configure<ApplicationExtension> { }` is just the programmatic way of writing the `android { }` block you already know — we're reaching into the Android plugin and setting its options from inside *our* plugin. Any module that applies this now gets identical SDK levels and Java settings, guaranteed, because there's only one copy of these numbers in the entire project.

The library version is almost the same file — it configures `LibraryExtension` instead of `ApplicationExtension` and adds a line for consumer ProGuard rules. Yes, that's a *little* duplication between the two. That's fine! The win is that it's now one file per *type* of module, instead of the same config pasted into every individual module. We traded a dozen copies for two clear recipes.

---

## Step 4 — A smarter plugin: Compose that works in any module

Because convention plugins are real Kotlin, they can be clever. Take Compose: some of your modules are apps, some are libraries, but both might use Compose. Rather than write two near-identical plugins, we write **one** that figures out which kind of module it's in and does the right thing:

```kotlin
// refactoring.android.compose.gradle.kts
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

// Reach into the version catalog from inside the plugin
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// Turn on Compose for whichever kind of module this is
extensions.findByType(ApplicationExtension::class)?.apply {
    buildFeatures { compose = true }
}
extensions.findByType(LibraryExtension::class)?.apply {
    buildFeatures { compose = true }
}

// Add the whole Compose stack, versions managed by the BOM
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
```

Two lines deserve a friendly explanation:

- **`the<LibrariesForLibs>()`** is how a plugin reads your version catalog. It's a bit of an incantation, but the payoff is real: every module that applies this plugin gets the *exact same* Compose dependencies, all aligned by the same BOM. No more "module A is on a different Compose version than module B."
- **`findByType(...)?.apply { }`** is the "figure out what kind of module I'm in" part. If the module is an app, it configures the app extension; if it's a library, it configures the library one; if it's neither, it does nothing and doesn't crash. One plugin, every situation handled.

The Hilt plugin follows the same idea in even fewer lines: apply the KSP and Hilt plugins, then add `libs.hilt.android` and the `ksp` compiler. Apply it, and a module is Hilt-ready — no thinking required.

---

## Step 5 — Bake your team's standards right into the build

Here's where convention plugins stop being "tidy config" and become genuinely powerful. You can encode a *policy* — a rule your team cares about — and have the build enforce it automatically.

Example: "every module should have at least 40% test coverage." Normally that lives in a wiki page nobody reads, and slowly becomes a lie. Instead, we put it in a plugin. This one generates a coverage report *and* fails the build if coverage drops too low, then hooks itself into Gradle's standard `check` task so it runs in CI without anyone remembering to call it:

```kotlin
// refactoring.jacoco.gradle.kts
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()   // 40% line coverage floor
            }
        }
    }
}

// Make the normal `check` task run our verification automatically
tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
```

This is the quiet superpower of convention plugins: **standards become code that runs**, instead of good intentions that erode. Every module that applies the plugin is held to the same bar, on every pull request, forever — and you never had to nag anyone.

---

## The payoff (remember that picture from the start?)

With the plugins in place, your modules go back to being simple and declarative. Your app's build file is now about *this module's specifics* — its namespace, its app id — and nothing else:

```kotlin
// app/build.gradle.kts
plugins {
    id("refactoring.android.application")
    id("refactoring.android.compose")
    id("refactoring.android.hilt")
    id("refactoring.jacoco")
}

android {
    namespace = "com.refactoring.excercise"
    defaultConfig {
        applicationId = "com.refactoring.excercise"
        // only the bits that are genuinely unique to this module
    }
}
```

And the everyday wins are the kind you actually feel:

- **Bump `compileSdk` across the whole project?** One line, in one plugin. Done.
- **Spin up a new feature module with Compose and Hilt?** Apply two plugin ids. That's the whole setup.
- **Onboarding a teammate?** They don't need to memorize your SDK levels or Compose BOM. The plugins already know.
- **Config drift?** It basically can't happen anymore. There's only one copy of each decision.

That's the real reason convention plugins are worth the afternoon it takes to set them up: your build configuration becomes **something you write once and reuse**, instead of boilerplate you copy and then watch slowly rot.

---

## Why this *really* pays off: big teams, big projects, and build time

Everything so far is nice on a solo project. But the value grows non-linearly as your team and your codebase grow. Here's where it goes from "tidy" to "I can't imagine working without it."

### It scales your *team*, not just your code

On a one-person project, config drift is annoying. On a 20-person project, it's a daily tax. Think about what normally happens without convention plugins:

- A new engineer creates a feature module and copies whoever's build file was closest. Was that the up-to-date one? Who knows.
- Five people configure Compose five subtly different ways, and now a Compose upgrade is five separate puzzles instead of one.
- A code reviewer has to actually *read* every `build.gradle.kts` in a PR, because any of them could quietly set a wrong `minSdk` or skip the coverage check.
- The one person who "understands the build" becomes a bottleneck — every build question routes through them.

Convention plugins dissolve all of that. **The build setup stops being tribal knowledge and becomes a shared, named thing anyone can apply.** A new module is `id("refactoring.android.application")` plus a couple of plugins — there's no "right way" to copy, because there's only one way. Reviewers stop scrutinizing boilerplate and focus on what's actually unique. And the "build expert" makes one change in one plugin instead of fielding the same question twenty times. You've turned "how we build things" from folklore into infrastructure the whole team trusts.

### It makes consistency *enforceable*, not aspirational

Big teams usually have standards on paper: "use Java 11," "every module needs 40% coverage," "Compose modules use the BOM." On paper, those standards rot the moment someone's in a hurry. Baked into a convention plugin, they're *automatic* — applied identically to every module, checked on every PR, impossible to forget. That Jacoco gate from Step 5 isn't a guideline anymore; it's a wall. Multiply that across dozens of modules and you've eliminated an entire category of "oops, that module was misconfigured" bugs.

### How it cuts build time across a multi-module app

This is the part people underestimate, so let me be precise about *why* it helps — convention plugins don't make the Kotlin compiler itself faster, but they unlock and protect the things that genuinely speed up a multi-module build:

- **Clean module boundaries let Gradle build in parallel.** A multi-module app's superpower is that independent modules compile at the same time, and an unchanged module isn't rebuilt at all. But that only works if modules are configured *consistently and correctly*. One module with a sloppy, hand-rolled config can serialize the build or bust incremental compilation for everything downstream. Convention plugins make every module a well-behaved citizen, so Gradle's parallelism actually kicks in.

- **Consistent config is what keeps the build cache and configuration cache working.** Gradle's build cache reuses task outputs, and the configuration cache skips re-evaluating your build scripts entirely — both huge wins on a big project. But they're fragile: subtle per-module differences and ad-hoc logic quietly cause cache misses, and you're back to building from scratch. When every module is configured the same way through the same plugins, cache hits become the norm, not the exception. **The fastest task is the one Gradle skips because nothing relevant changed.**

- **The plugin logic compiles once and is reused.** Your `build-logic` is itself built and cached. After the first run, applying `id("refactoring.android.compose")` to ten modules doesn't re-evaluate ten copies of that setup logic — it reuses the compiled, cached plugin. Compare that to ten modules each carrying their own inline Compose block that Gradle has to configure independently every time.

- **One change rebuilds the right things, not everything.** Bump `compileSdk` in one plugin and only the affected modules rebuild. Do the same change as a find-and-replace across twelve hand-written files and you risk touching files Gradle now considers "changed," invalidating caches far more widely than necessary.

So the build-time story is really two stories. The dramatic one is **human time**: onboarding drops from "read the wiki and copy a file and hope" to "apply three plugins"; a project-wide SDK bump goes from an afternoon of find-and-replace-and-pray to a one-line commit; and nobody burns hours debugging why module 14 builds differently from module 3. The quieter one is **machine time**: by keeping every module consistent and correct, you let Gradle's parallel execution, incremental compilation, and caching do their jobs — which on a large multi-module codebase is the difference between a coffee-break build and a stay-in-flow one.

---

## Where this fits

A consistent, self-enforcing build is the foundation everything else sits on — including the security and release pipeline from [Part 1](#). That pipeline could sign artifacts, generate SBOMs, and attach SLSA provenance precisely *because* every module was configured identically underneath it. Read the two posts together and you have the full picture: a build foundation that can't drift (this part) and a security layer that ships it with proof (Part 1).

*Everything here is real, working configuration: a `build-logic/convention` included build with six convention plugins (`android.application`, `android.library`, `android.compose`, `android.hilt`, `jvm.library`, `jacoco`), all sharing a single `gradle/libs.versions.toml` version catalog across the whole project.*

---

📦 **Full source code:** [github.com/martinGele/basic](https://github.com/martinGele/basic) — clone it, poke around, steal what's useful.

👋 **Found this helpful?** Let's connect on [LinkedIn](https://www.linkedin.com/in/martin-gelevski-6904a474/) — I write about Android, build tooling, and shipping software you can trust.
