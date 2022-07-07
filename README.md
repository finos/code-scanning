# security-scanning

## The problem
**How can we keep all FINOS hosted codebase secure?** This is a pretty wide question, so let's try to decompose it.

On average, 99% of the code shipped in a software is composed by downstream libraries (aka dependencies), built, released and managed but teams, communities and companies we know nothing about. A developer has very little chances to be aware of the codebase quality and software development processes across all downstream dependencies of a project. To make it worse, every programming language AND build tool may have different ways to consume downstream dependencies, therefore there is not one common way to check the security of software that is being consumed.

The good news is that [NVD](https://nvd.nist.gov/) and other similar initiatives have built an important knowledge base on which known vulnerabilities (or CVEs) affect which library, so the first (security) challenge that every developer is nowadays facing is: how can I check the list of my (direct and transitive) dependencies against the database of CVEs ?

The other 1% is code written by the developer, which can also be affected by bugs, but those are still unknown by everyone; this is where Static application security testing (or simply Static Analysis) comes in handy.

At [FINOS](finos.org), we're always strive to improve the security of our hosted code; right now, we're taking 2 main directions:

1. **Reactive** (to code changes) scans: everytime that code is changed, security scanning should kick in
    - If the change affects the build descriptor (and therefore the list of downstream dependencies is updated), the list should be checked against the (public) list of CVEs. IF the scan returns a negative response, the change MUST be rejected; to achieve such behaviour, enforcing [branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches) is mandatory.
    - If the change affects the rest of the code, SAST should kick in; this part is still work in progress
2. **Proactive** (to new CVEs) scans: imagine to write a software today, which consumes a downstream library that is clear from CVEs; tomorrow, a new CVE that affects this downstream library (same version as the one being used) is discovered, turning yesterday's software vulnerable. It is very important to notify developers quickly and privately, to avoid that such information can be used by malicious actors against any software adopter. This can be easily achieved by running the same reactive scan on a schedule, for example daily (see examples below).

## The solution
To tackle such aspirational goals, we've evaluated **a lot** of services and tools, and for some reason or another, it's extremely hard to find one solution that ticks all boxes. This is why, before moving forward with this hunt, we decided to create this repository.

FINOS Security Scanning sets a baseline for the security scanning that our hosted projects need:
- 5 supported build platforms - maven, gradle, python (and poetry), scala and node
- A common approach to CVE scanning mechanism, which only affects runtime dependencies; anything else is - for now - out of scope.
- Support direct and transitive dependencies
- Ability to suppress warnings and efficiently manage false positives; such suppressions MUST be part of the codebase, and treated as an extremely important code
- Ability to run such logic as part of CI/CD (GitHub actions)
- Documentation that describes how to use the scanning and what to expect

It's worth emphatizing the importance of warning/error suppression; one of the reason for this approach not to be mainstream nowadays is that developers constantly seek productivity, and often see security as a distraction rather than protection; being able to deliver an efficient scanning tool is key to keep developers happy and make sure they use and update scanning configurations.

In this codebase you'll find a folder for each of the build platforms listed below; each folder includes a "Hello World" project, with a build descriptor that delivers 
1. Pulls in a CVE
2. Configures a CVE scanning tool that is specific to the build tool
3. Defines a suppression file that ignores the error caused by the CVE

## Node
The NodeJS project uses [AuditJS](https://www.npmjs.com/package/auditjs), which limits scope only to non dev dependencies by default.

In `node/package.json` you'll notice that the last `dependency` is `chokidar: 2.0.3`, which introduces 4 CVEs; however, the CVE scanning - which you can run simply using `npm install ; npm run scan-cves` passes, because `whitelist.json` instructs the scanner to ignore such issues.

To enable the scanning on your repository, simply create a new file called `.github/workflows/cve-scanning.yml` and paste this content:

```
name: Node.js CVE Scan

on:
  pull_request:
    paths:
      - 'package.json'
      - '.github/workflows/node.yml'
  push:
    paths:
      - 'package.json'
      - '.github/workflows/node.yml'
    schedule:
      # Run every day at 5am and 5pm
      - cron: '0 5,17 * * *'

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node-version: [16.x]
    steps:
      - uses: actions/checkout@v3
      - name: Use Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v3
        with:
          node-version: ${{ matrix.node-version }}
      - run: npx --yes auditjs ossi --whitelist whitelist.json
        working-directory: node
```

You will also need to create a `whitelist.json` file in the project root, check an example in the `node` folder.

## Python
The python project is built with [Poetry](https://python-poetry.org/), see `python/pyproject.toml`.

To scan for CVEs, we use:
1. `poetry export` command, which generates a `requirements.txt` file
2. The [`safety` library](https://pyup.io/safety/), which checks `requirements.txt` entries against NVD DB.

The `python/pyproject.toml` includes a commented dependency called `insecure-package`, which introduces CVEs into the project and tests whether the scan works properly or not; the `safety-policy.yml` file will suppress such issue, in order to test the mechanism to ignore false positives.

If you want to run the check locally, you can run:
```
poetry install
poetry export --without-hashes -f requirements.txt | poetry run safety check --full-report --stdin --policy-file safety-policy.yml
```

Check `python/pyproject.toml` and `.github/workflows/poetry.yml` for more info.

## Gradle
The Gradle build uses the [Dependency Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html).

The `build.gradle` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

Simply run `./gradlew dependencyCheckAnalyze` to run the CVE scan; check `gradle/build.gradle` and `.github/workflows/gradle.yml` for more info.

## Maven
The maven project uses the [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) plugin to scan runtime dependencies for known vulnerabilities.

The `pom.xml` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

The CVE scanning is included in the `check` build phase; as such, it will be invoked when running `mvn package`; check `maven/pom.xml` and `.github/workflows/maven.yml` for more info.

## Scala
The Scala project uses the [`sbt-dependency-check` plugin](https://github.com/albuch/sbt-dependency-check) to scan incoming dependencies for CVEs.

The `build.sbt` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

Simply run `sbt dependencyCheck` to run the CVE scan; check `scala/build.sbt` folder and `.github/workflows/scala.yml` for more info.
