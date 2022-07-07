# security-scanning

## The problem

**How can we keep FINOS' hosted codebase secure?** 

This is a pretty wide question, so let's try to decompose it:

- On average, 99% of the code shipped in a software artifact is composed of downstream libraries (aka dependencies), built, released and managed but teams, communities and companies _we know nothing about_. 
- A developer has very little awareness of the codebase quality and software development process in the downstream dependencies of a project. 
- To make it worse, every programming language AND build tool has a different way of consuming downstream dependencies.
- Therefore there is not one common way to check the security of software that is being consumed.

So the first (security) challenge that every developer is nowadays facing is: how can I check the list of my (direct and transitive) dependencies against this database of CVEs?

The good news is that [NVD](https://nvd.nist.gov/) and other similar initiatives have built an important knowledge base containing known vulnerabilities (or CVEs) affect which libraries.  

The other 1% is code written by the developer.  This code can contain vulnerabilities unknown to everyone.  This is where _static application security testing_ (or simply Static Analysis) comes in handy.

At [FINOS](finos.org), we always strive to improve the security of our hosted code.  Right now, we're moving in 2 main directions:

### Reactive

_Everytime that code is changed, security scanning should kick in_

- If the change affects the _build descriptor_ (i.e. the list of downstream dependencies may have been updated), the list should be checked against the (public) list of CVEs. 
- If that scan returns a negative response, the change ***must** be rejected.
- To achieve such behaviour, [branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches) is mandatory.
- If the change affects the rest of the code, SAST should kick in; _this part is still work in progress_.

### Proactive

_A new CVE that affecting a downstream library makes the current code vulnerable._

It is very important to notify developers quickly and privately, to prevent malicious actors taking advantage of the vulnerability. This can be easily achieved by running the same reactive scan **on a schedule**, for example daily (see examples below).

## The solution

We've evaluated **a lot** of services and tools but for some reason or other it's extremely hard to find one solution that ticks all boxes. This is why we decided to create this repository.  FINOS Security Scanning sets a baseline for the security scanning that our hosted projects need:

- 5 supported build platforms - maven, gradle, python (and poetry), scala and node.
- A common approach to CVE scanning mechanism, which only examines runtime dependencies (anything else is - for now - out of scope).
- Support direct and transitive dependencies
- Ability to suppress warnings and efficiently manage false positives.  Such suppressions MUST be part of the codebase and treated as an extremely important code
- Ability to run such logic as part of CI/CD (GitHub actions)
- Documentation that describes how to use the scanning and what to expect.

### Suppression

It's worth emphatizing the importance of warning/error suppression; one of the reasons CVE scanning isn't mainstream is that developers _constantly seek productivity_. Often, vulnerability reporting is seen as a distraction rather than a benfit. Delivering an efficient scanning tool _without false positives_ is key to keeping developers happy and making sure they use the tools.

## Code Layout

In this codebase you'll find a folder for each of the build platforms listed below.  Each folder includes a "Hello World" project, with a build descriptor that:

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
