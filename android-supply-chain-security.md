# Shipping Android to Play with SLSA Provenance, SBOMs & CodeQL: The Supply-Chain Setup Nobody Writes About for Mobile

> *Part 1 of a two-part series. This post builds a real security and release pipeline for an Android app — from a pull-request scan all the way to a signed, provenance-attested `.aab` on Google Play. Part 2 covers the build foundation it all sits on: convention plugins. All with free, open GitHub Actions.*

There's no shortage of supply-chain security writing out there — but almost all of it is about backends, containers, or generic Java. Search for the *Android* version of "sign it, generate an SBOM, attach SLSA provenance, ship it to Play," and you mostly come up empty. That's the gap this post fills: the same supply-chain rigor the cloud-native world takes for granted, applied end-to-end to an Android app release.

And it starts with an uncomfortable truth: **most of what ships in your app isn't code you wrote.**

Think about it. Your `.aab` contains your Kotlin, sure — but also dozens of third-party libraries, each pulling in *their* dependencies, each with its own history of bugs and CVEs. Your CI pipeline runs Actions written by strangers. Your release is signed with a key that, if leaked, lets anyone impersonate you. Your secrets live in environment variables that one careless commit could expose.

Here's the part that bites teams: nobody is watching any of this by default. The typical story is that everything's "fine" right up until a customer sends a security questionnaire, or a CVE makes the news and someone asks *"wait, are we using that library?"* — and nobody knows.

The fix isn't heroics or a once-a-year audit. It's **automation**: a handful of checks that run on every push, every pull request, and every release, so security becomes part of the furniture instead of a panic. The lovely thing is that all of it is free and open, and it's just a few YAML files.

We'll go in three natural groups:

1. **Find problems** — scan your code, your secrets, and your dependencies
2. **Stay ahead** — keep things updated and give people a way to report issues
3. **Ship with proof** — sign your releases and prove where they came from

Each one comes with the *why* first, then the *how*.

---

## Group 1 — Finding Problems Automatically

The goal here is simple: catch issues while they're cheap to fix (in a pull request) instead of expensive to fix (in production). We'll use four scanners, each looking at a different angle, because no single tool sees everything.

### Layer 1: CodeQL — checking your own code

**Why:** You can introduce a security bug without realizing it — an unsafe way of handling input, a risky data flow. CodeQL is GitHub's static analysis engine: it reads your Kotlin/Java, builds a searchable model of it, and runs security queries to spot those patterns. It's like a tireless reviewer who only cares about security and never gets bored.

It runs on every push and PR, plus a nightly sweep (in case a new query is added that catches something old):

```yaml
# .github/workflows/security-scan.yml
name: Security Scan
on:
  push:         { branches: [ main, master ] }
  pull_request: { branches: [ main, master ] }
  schedule:
    - cron: '0 2 * * *'   # nightly, just in case
jobs:
  analyze:
    runs-on: ubuntu-latest
    permissions:
      actions: read
      contents: read
      security-events: write   # lets it post findings to the Security tab
    steps:
    - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4
    - uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9 # v4
      with: { java-version: '17', distribution: 'temurin', cache: gradle }
    - name: Initialize CodeQL
      uses: github/codeql-action/init@7211b7c8077ea37d8641b6271f6a365a22a5fbfa # v4
      with:
        languages: java-kotlin
        queries: security-and-quality
    - name: Build
      run: ./gradlew assembleDebug --no-daemon
    - name: Perform CodeQL Analysis
      uses: github/codeql-action/analyze@7211b7c8077ea37d8641b6271f6a365a22a5fbfa # v4
```

CodeQL needs to *build* your app (that `./gradlew assembleDebug` step) so it can see the real, compiled code. The findings show up in your repo's **Security tab** — not buried in CI logs where nobody looks.

### Layer 2: Gitleaks — catching leaked secrets

**Why:** The single fastest way to get burned is committing a secret — an API key, a signing password — into git. And here's the trap people miss: deleting it in a later commit doesn't help, because *it's still in the history forever.* Anyone who clones the repo can dig it out.

Gitleaks scans for things that look like secrets. The crucial detail is `fetch-depth: 0`, which tells it to download the **entire history**, not just the latest commit — so a key buried five commits back still gets caught:

```yaml
# .github/workflows/secret-scan.yml
jobs:
  gitleaks:
    runs-on: ubuntu-latest
    permissions: { contents: read }
    steps:
    - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4
      with:
        fetch-depth: 0          # scan ALL history, not just the latest commit
    - uses: gitleaks/gitleaks-action@ff98106e4c7b2bc287b24eaf42907196329070c7 # v2
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Layer 3: Trivy — finding vulnerable dependencies

**Why:** Remember the opening line — most of your app is other people's code. When a vulnerability is discovered in a library you depend on, *you* are now vulnerable, even though you wrote nothing wrong. Trivy checks your dependencies against databases of known vulnerabilities and tells you if you're shipping something with a known hole.

This one actually **fails the build** on serious (`CRITICAL`/`HIGH`) findings — but with one humane setting that keeps the team from hating it:

```yaml
# .github/workflows/dependency-scan.yml
- name: Run Trivy filesystem scan
  uses: aquasecurity/trivy-action@ed142fd0673e97e23eac54620cfb913e5ce36c25 # v0.36.0
  with:
    scan-type: 'fs'
    scan-ref: '.'
    severity: 'CRITICAL,HIGH'
    ignore-unfixed: true     # don't block on problems that have NO fix yet
    exit-code: '1'           # fail the build when something fixable is found
    format: 'sarif'
    output: 'trivy-results.sarif'
- name: Upload Trivy SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@7211b7c8077ea37d8641b6271f6a365a22a5fbfa # v4
  with:
    sarif_file: 'trivy-results.sarif'
    category: 'trivy-fs'
```

That `ignore-unfixed: true` line is the difference between a tool people trust and one they disable in frustration. There's no point blocking someone's merge over a vulnerability that has no fix available yet — they literally can't do anything about it. So we only block on things that are actually actionable.

### Layer 4: MobSF — the Android specialist

**Why:** General scanners don't know Android. They won't flag an accidentally-exported component, insecure crypto, a hardcoded secret, or a weak SSL/TLS config — but those are exactly the kinds of mistakes that hurt mobile apps. `mobsfscan` is built specifically for this:

```yaml
# .github/workflows/mobsf-scan.yml
- name: Run mobsfscan
  uses: MobSF/mobsfscan@ec2927a8cfab6626a67f26b223be3aba52a34b70 # main
  with:
    args: '. --sarif --output mobsfscan.sarif || true'
- name: Upload mobsfscan SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@7211b7c8077ea37d8641b6271f6a365a22a5fbfa # v4
  with:
    sarif_file: mobsfscan.sarif
    category: mobsfscan
```

See that `|| true`? It means MobSF *reports* its findings to the Security tab but doesn't *fail* the build. That's a deliberate, friendly default — mobile scanners can be noisy with false positives, and you don't want a shaky tool blocking everyone's work on day one. Once you've reviewed its findings and trust it, you can remove `|| true` and make it a hard gate.

**The shape of Group 1:** four scanners, four blind spots covered — your code (CodeQL), your secrets (Gitleaks), your dependencies (Trivy), and your platform (MobSF). Together they form a net with small holes.

---

## Group 2 — Staying Ahead of Trouble

Finding problems is half the job. The other half is not falling behind in the first place, and making it easy for people to tell you when something's wrong.

### Dependabot — updates that don't drown you

**Why:** Scanners tell you what's *currently* vulnerable. Dependabot helps you not get there: it watches your dependencies and opens pull requests when newer (often safer) versions are released. Left unconfigured, though, it'll flood you with 40 separate PRs every Monday and you'll start ignoring them — which defeats the purpose.

The fix is **grouping**: bundle related updates into a few sensible PRs you'll actually review:

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule: { interval: "weekly" }
    open-pull-requests-limit: 10
    groups:
      androidx: { patterns: ["androidx.*"] }
      compose:  { patterns: ["androidx.compose*"] }
      kotlin:   { patterns: ["org.jetbrains.kotlin*", "org.jetbrains.kotlinx*"] }
  - package-ecosystem: "github-actions"   # also keep your CI Actions fresh
    directory: "/"
    schedule: { interval: "weekly" }
```

Don't skip that second entry. It tells Dependabot to also update your **GitHub Actions** — which matters a great deal for the supply-chain story coming up next.

### SECURITY.md — a friendly front door for reporting

**Why:** Imagine a well-meaning security researcher finds a flaw in your app. If you haven't told them how to reach you privately, their only option is to open a public issue — broadcasting the vulnerability to the world before you can fix it. A tiny `SECURITY.md` file prevents that by giving them a private path:

```markdown
# Security Policy

## Reporting a Vulnerability
Report privately by emailing security@yourdomain.
Do **not** open a public GitHub issue for security-sensitive reports.
You can expect an initial response within 5 working days.
```

GitHub automatically surfaces this in your repo's Security tab and its "Report a vulnerability" flow. It costs you five minutes and is the cheapest insurance you'll ever buy against a problem being aired in public.

---

## Group 3 — Shipping With Proof

This is the group most tutorials skip entirely, and it's honestly what separates "we run a linter" from a release you can genuinely stand behind. The theme here is *trust*: can you, and your users, be sure that the thing you shipped is really the thing you built?

Three terms come up a lot here, and they're easy to mix up — so let's get them straight before we dive in. The friendly way to remember them is to imagine **shipping a package**:

> **📦 SBOM (Software Bill of Materials)** — *the packing slip.* A machine-readable list of every component that went into your build: every library, every transitive dependency, and its exact version. When the next big CVE drops and everyone's asking *"are we affected?"*, an SBOM lets you answer in **seconds** instead of days.
>
> **✅ Provenance** — *the signed shipping label.* A tamper-evident record stating *"this exact file was built by this exact workflow, from this exact commit."* A signature proves *who* shipped it; provenance proves *how it was made* — so nobody can swap in a build they made on their laptop.
>
> **🧂 SLSA (Supply-chain Levels for Software Artifacts, say "salsa")** — *the courier's trust rating.* Not a tool, but a framework of **levels** describing how rigorous your build is. "We have provenance" is vague; "we meet **SLSA Level 3**" (generated by a hardened, isolated build you can't tamper with) is a recognized bar a security reviewer can actually trust — and one regulations increasingly demand.

In short: the SBOM says *what's inside*, provenance says *where it came from*, and SLSA says *how much you can trust that proof*. Now let's set up each one.

### Pin every Action to an exact commit (not a tag)

**Why this is a big deal:** every `uses:` line in your workflows runs *someone else's code* inside your pipeline — code that can see your secrets. Look closely and you'll notice every one is pinned to a long commit hash, not a friendly tag like `@v4`:

```yaml
uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4
```

Here's the catch with tags: a tag like `v4` can be *moved*. If an attacker compromises that Action's repository, they can quietly repoint `v4` at malicious code, and your next pipeline run happily executes it — with full access to your secrets. This isn't hypothetical; it's exactly how several real supply-chain attacks have played out (the `tj-actions/changed-files` incident being a recent, well-publicized one).

A commit hash, on the other hand, can never change. It always points at the exact same code. The `# v4` comment keeps it human-readable so you know what version you're on, and Dependabot (from Group 2) bumps the hash for you when a genuine update lands. Best of both worlds.

### Sign releases with secrets, never with committed keys

**Why:** Your signing key is your identity. Anyone who has it can publish an app that *looks* like it came from you. So it must never, ever live in the repo. Notice the signing config reads everything from environment variables — there's no key and no password anywhere in the code:

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

In CI, even the keystore *file* isn't committed — it's stored as a base64-encoded secret and decoded into a temporary folder only at build time, then handed to Gradle through environment variables:

```yaml
# .github/workflows/release.yml
- name: Decode keystore
  run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > $RUNNER_TEMP/release.keystore
- name: Build release AAB
  env:
    KEYSTORE_PATH: ${{ runner.temp }}/release.keystore
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew :app:bundleRelease
```

The secret exists only for the few seconds the build needs it, then the runner is thrown away. Nothing lingers.

### Generate an SBOM — a parts list for your app

**Why:** Remember that future moment when a CVE hits the news and someone asks *"are we affected?"* An SBOM (Software Bill of Materials) is the answer. It's a machine-readable list of every single component that went into your build — so instead of spending two days grepping through dependency trees, you search one file and answer in seconds.

CycloneDX generates it with one Gradle task:

```yaml
- name: Generate CycloneDX SBOM
  run: ./gradlew :app:cyclonedxBom
```

The resulting `bom.json` then travels with the release as an attached file, so it's there when you need it.

### Prove provenance with SLSA — the receipt for your build

**Why (and this is the capstone):** Even a signed app doesn't prove *how* it was built. Could someone have built a tampered version on their laptop and signed it? SLSA provenance answers that. It produces a signed, tamper-evident "receipt" stating: *this exact file was built by this exact workflow, from this exact commit.* Anyone can later verify the artifact wasn't swapped or rebuilt by someone else along the way.

It works by hashing the built file, then handing that hash to SLSA's official reusable workflow, which produces the signed attestation:

```yaml
- name: Hash AAB for provenance
  id: hash
  working-directory: release-artifacts
  run: echo "hashes=$(sha256sum app-release.aab | base64 -w0)" >> "$GITHUB_OUTPUT"

# ... in a separate job ...
provenance:
  needs: [build-release]
  permissions:
    actions: read
    id-token: write     # needed for keyless signing via OIDC
    contents: write
  uses: slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@v2.1.0
  with:
    base64-subjects: "${{ needs.build-release.outputs.aab_hashes }}"
    provenance-name: "app-release.aab.intoto.jsonl"
```

**One friendly exception to flag:** notice this SLSA workflow is pinned to a *tag* (`@v2.1.0`), not a commit hash — the opposite of what we preached above. That's intentional and required: SLSA's identity verification is deliberately tied to its tag, and it's documented to be used this way. So if a linter ever yells at you for not pinning this one to a SHA, you can safely tell it this is the sanctioned exception.

### Ship it: publish to Play with everything attached

**Why:** All that proof is worthless if it doesn't travel *with* the app. The final job ties the bow: it gathers the signed bundle, the SBOM, and the provenance receipt, ships the `.aab` to Google Play, tags the commit, and attaches every artifact to a GitHub Release — so the proof is downloadable right next to the thing it vouches for.

First, the bundling step collects everything into one folder (note how the SBOM is attached only if it was generated):

```yaml
# .github/workflows/release.yml — staging artifacts
- name: Stage release artifacts
  run: |
    mkdir -p release-artifacts
    cp app/build/outputs/bundle/release/app-release.aab release-artifacts/
    if [ -f app/build/reports/bom.json ]; then
      cp app/build/reports/bom.json release-artifacts/app-release.cdx.json
    fi
    ls -la release-artifacts/
```

Then the `publish` job uploads to the Play internal track, tags the release, and creates the GitHub Release with the AAB, SBOM, and provenance file all attached:

```yaml
publish:
  needs: [build-release, provenance]
  runs-on: ubuntu-latest
  permissions: { contents: write }
  steps:
  - name: Upload to Google Play
    uses: r0adkll/upload-google-play@e738b9dd8f2476ea806d921b64aacd24f34515a5 # v1
    with:
      serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
      packageName: com.refactoring.excercise
      releaseFiles: release-artifacts/app-release.aab
      track: internal
      status: completed
  - name: Tag release
    run: |
      git tag -a "v${{ needs.build-release.outputs.version_name }}" \
        -m "Release v${{ needs.build-release.outputs.version_name }}"
      git push origin "v${{ needs.build-release.outputs.version_name }}"
  - name: Create GitHub Release
    uses: softprops/action-gh-release@de2c0eb89ae2a093876385947365aca7b0e5f844 # v1
    with:
      tag_name: v${{ needs.build-release.outputs.version_name }}
      files: |
        release-artifacts/app-release.aab
        release-artifacts/app-release.cdx.*
        release-artifacts/app-release.aab.intoto.jsonl
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Notice `needs: [build-release, provenance]` — the publish job won't even start until the build *and* the provenance attestation are both done. The Play credentials live in a secret (`PLAY_SERVICE_ACCOUNT_JSON`), never in the repo. And the GitHub Release ends up holding all three of our proofs side by side: the app, its ingredients (SBOM), and its receipt (provenance). All automatic, with R8 shrinking already on.

---

## The whole picture

Here's everything the pipeline quietly does for you, on every commit and every release, with zero manual effort:

| What we're protecting | How | Does it block a merge? |
|---|---|---|
| Bugs in your own code | CodeQL | Reports to Security tab |
| Leaked secrets | Gitleaks (full history) | Yes |
| Vulnerable dependencies | Trivy (`CRITICAL`/`HIGH`) | Yes — but only fixable ones |
| Android-specific mistakes | MobSF | Reports (you can make it block later) |
| Falling behind on updates | Dependabot (grouped) | — |
| Private vulnerability reports | `SECURITY.md` | — |
| Malicious CI Actions | Pinning Actions to commit hashes | — |
| Your signing identity | Env-var signing + base64 keystore | — |
| "What's actually in this build?" | CycloneDX SBOM | — |
| "Who built this, and from what?" | SLSA provenance receipt | — |

None of this is exotic, and none of it is paid. The genuinely hard part was never the tools — it was the mindset shift: deciding that **security is part of your infrastructure, not a chore you do later**, and then writing it down so the pipeline enforces it instead of relying on anyone remembering.

This pipeline doesn't stand alone, though — it works *because* the project underneath it is consistent. Every module builds the same way, signs the same way, and is checked the same way. That consistency isn't an accident; it comes from the build foundation we'll set up in **Part 2** using convention plugins. Put the two halves together and you get a project that's both *trustworthy to ship* and *consistent to build* — automatically, by default, on every single push. And honestly? Once it's set up, you mostly forget it's there. Which is exactly the point.

---

*The pipeline described here is real: five GitHub Actions workflows (`security-scan`, `secret-scan`, `dependency-scan`, `mobsf-scan`, `release`) plus `dependabot.yml` and `SECURITY.md`, with every third-party Action pinned to a commit hash, and a release flow that produces a signed AAB, a CycloneDX SBOM, and SLSA provenance.*

---

📦 **Full source code:** [github.com/martinGele/basic](https://github.com/martinGele/basic) — all the workflows in this post live in `.github/workflows`. Clone it and use it as a starting point.

👋 **Found this helpful?** Let's connect on [LinkedIn](https://www.linkedin.com/in/martin-gelevski-6904a474/) — I write about Android, build tooling, and shipping software you can trust.
