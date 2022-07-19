# FINOS Security Scanning

## The problem
**How can we keep FINOS' hosted codebase secure?** 

This is a pretty wide question, so let's try to decompose it:

- On average, 99% of the code shipped in a software artifact is composed of downstream libraries (aka dependencies), built, released and managed by teams, communities and companies _we know nothing about_. 
- A developer has very little awareness of the codebase quality and software development process in the downstream dependencies of a project. 
- To make it worse, every programming language AND build tool has a different way of consuming downstream dependencies.
- Therefore there is not one common way to check the security of software that is being consumed.

So the first (security) challenge that every developer faces nowadays is: how can I check the list of my (direct and transitive) dependencies against a public database of CVEs?

The good news is that [NVD](https://nvd.nist.gov/) and other similar initiatives have built an important knowledge base containing known vulnerabilities (or CVEs) affecting many popular libraries.  

The other 1% is code written by the developer.  This code can contain vulnerabilities unknown to everyone.  This is where _static application security testing_ (or simply Static Analysis) comes in handy.

At [FINOS](finos.org), we always strive to improve the security of our hosted code.  Right now, we're moving in 2 main directions:

### Reactive
_Everytime that code is changed, security scanning should kick in_

- If the change affects the _build descriptor_ (i.e. the list of downstream dependencies may have been updated), the list should be checked against the (public) list of CVEs. 
- If that scan returns a negative response, the change ***must** be rejected.
- To achieve such behaviour, [branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches) is mandatory.
- If the change affects the rest of the code, a static code analysis should kick in; there are specific tools for this tasks, read more below.

### Proactive
_A newly discovered CVE that is affecting a downstream library makes the current code vulnerable._

It is very important to notify developers quickly and privately, to prevent malicious actors from taking advantage of the vulnerability. This can be easily achieved by running the same reactive scan **on a schedule**, for example daily (see examples below).

## The solution
We've evaluated **a lot** of services and tools but for some reason or other it's extremely hard to find one solution that ticks all the boxes. This is why we decided to create this repository.  FINOS Security Scanning sets a baseline for the security scanning that our hosted projects need:

- 6 supported build platforms - Maven, Gradle, Python (and Poetry), Scala (with SBT), NodeJS and Rust
- A common approach to CVE scanning mechanism, which only examines runtime dependencies (anything else is - for now - out of scope).
- Support direct and transitive dependencies
- Ability to suppress warnings and efficiently manage false positives.  Such suppressions MUST be part of the codebase and treated as an extremely important code
- Ability to run such logic as part of CI/CD (GitHub actions)
- A tool to run static code analysis, integrated with GitHub actions for continuous runs
- Documentation that describes how to use the scanning and what to expect.

### Allowed lists
It's worth emphatizing the importance of warning/error suppression; one of the reasons CVE scanning isn't mainstream is that developers _constantly seek productivity_. Often, vulnerability reporting is seen as a distraction rather than a benfit. Delivering an efficient scanning tool _without false positives_ is key to keeping developers happy and making sure they use the tools.

## Code Layout
In this codebase you'll find a folder for each of the build platforms listed below. Each folder includes a "Hello World" project, with a build descriptor that:

1. Pulls in a CVE
2. Configures a CVE scanning tool that is specific to the build tool
3. Defines a suppression file that ignores the error caused by the CVE

The documentation below will also provide the GitHub Actions code that will enable scanning without the need to update anything in your build descriptor files.

### NodeJS
The NodeJS project uses [AuditJS](https://www.npmjs.com/package/auditjs), which limits scope only to non dev dependencies by default.

To enable the CVE scanning on your repository, simply create a new file called `.github/workflows/cve-scanning.yml` and paste this content:

```
name: Node.js CVE Scanning

on:
  pull_request:
    paths:
      - 'package.json'
      - '.github/workflows/cve-scanning.yml'
  push:
    paths:
      - 'package.json'
      - '.github/workflows/cve-scanning.yml'
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
      - run: npx --yes auditjs ossi --whitelist allow-list.json
```

Also create a `allow-list.json` file in the project root, check an example in the `node` folder.

If you want to test it, add a dependency against `"chokidar" : "2.0.3"`(https://pypi.org/project/insecure-package/), re-run the `npx --yes auditjs ossi --whitelist allow-list.json` command mentioned in the GitHub Action above and expect the build to fail.

### Python
For Python projects we recommend using the [`safety` library](https://pyup.io/safety/) library, which checks `requirements.txt` entries against NVD DB.

To enable the CVE scanning on your repository, simply create a new file called `.github/workflows/cve-scanning.yml` and paste this content:

```
name: Python CVE Scanning

on:
  pull_request:
    paths:
      - 'pyproject.toml'
      - '.github/workflows/cve-scanning.yml'
  push:
    paths:
      - 'pyproject.toml'
      - '.github/workflows/cve-scanning.yml'
    schedule:
      # Run every day at 5am and 5pm
      - cron: '0 5,17 * * *'

jobs:
  ci:
    strategy:
      fail-fast: false
      matrix:
        python-version: ["3.10"]
        poetry-version: ["1.1.11"]
        os: [ubuntu-18.04]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-python@v2
        with:
          python-version: ${{ matrix.python-version }}
      - name: Install safety
        run: pip3 install safety
      - name: Run safety check
        run: safety check --full-report -r requirements.txt --policy-file safety-policy.yml
```

If you are using [Poetry](https://python-poetry.org/), add the following steps before the `Install safety` block:
```
      - name: Run image
        uses: abatilo/actions-poetry@v2.0.0
        with:
          poetry-version: ${{ matrix.poetry-version }}
      - name: Export requirements.txt from poetry
        run: poetry export --without-hashes -f requirements.txt > requirements.txt
```

Make sure to create a `safety-policy.yml` file, which will define which errors/warnings to suppress as false positives; you can find a sample file in the `python` subfolder.

If you want to test it, add a dependency against [`insecure-package`](https://pypi.org/project/insecure-package/), re-run the `safety check` command mentioned in the GitHub Action above and expect the build to fail.

### Maven
The maven project uses the [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) plugin to scan runtime dependencies for known vulnerabilities.

To enable the CVE scanning on your repository, simply create a new file called `.github/workflows/cve-scanning.yml` and paste this content:

```
name: Maven CVE Scanning

on:
  pull_request:
    paths:
      - 'pom.xml'
      - '.github/workflows/cve-scanning.yml'
  push:
    paths:
      - 'pom.xml'
      - '.github/workflows/cve-scanning.yml'
    schedule:
      # Run every day at 5am and 5pm
      - cron: '0 5,17 * * *'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'adopt'
      - name: Build with Maven
        run: mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -DsuppressionFile="allow-list.xml"
```

Make sure to create a `allow-list.xml` file, which will define which errors/warnings to suppress as false positives; you can find a sample file in the `maven` subfolder.

If you prefer to integrate the Maven plugin in your `pom.xml`, checkout `maven/pom.xml` as example.

### Gradle
The Gradle build uses the [Dependency Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html). Sadly, Gradle [doesn't allow to invoke plugins without altering the build manifest](https://discuss.gradle.org/t/invoking-tasks-provided-by-a-plugin-without-altering-the-build-file/27235), namely `build.gradle`; follow instructions below to know how to add security scanning in your project.

The `build.gradle` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `allow-list.xml` file, used to manage false positives.

To enable the CVE scanning on your repository, follow these steps:
1. Copy the `dependencyCheck` setup from `gradle/build.gradle` file
2. Copy `allow-list.xml` file into your repository and remove all `<suppress>` entries
3. Run `./gradlew dependencyCheckAnalyze` locally
4. Copy `.github/workflows/gradle.yml` in your project and adapt it as you see fit

### Scala (with SBT)
The Scala project uses the [`sbt-dependency-check` plugin](https://github.com/albuch/sbt-dependency-check) to scan incoming dependencies for CVEs.

The `build.sbt` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `allow-list.xml` file, used to manage false positives.

To enable the CVE scanning on your repository, follow these steps:
1. Copy `dependencyCheckFailBuildOnCVSS` and `dependencyCheckSuppressionFiles` configurations from `scala/build.sbt` file in your project
2. Copy the `sbt-dependency-check` plugin definition from `scala/project/plugins.sbt` into your project
3. Run `sbt dependencyCheck` locally
4. Copy `.github/workflows/scala.yml` in your project and adapt it as you see fit

To keep your library dependencies, sbt plugins, and Scala and sbt versions up-to-date, checkout [Scala Steward](https://github.com/scala-steward-org/scala-steward).

### Rust
The Rust project uses [Cargo audit](https://crates.io/crates/cargo-audit) to run CVE scans across dependencies defined in `Cargo.toml`:
1. `cargo install --force cargo-audit` - to install Cargo audit
2. `cargo audit` - to run the scan ; you can append `--ignore RUSTSEC-2020-0071` in order to ignore false positives

For more information about Cargo audit configuration, visit [https://docs.rs/cargo-audit/0.17.0/cargo_audit/config/index.html](https://docs.rs/cargo-audit/0.17.0/cargo_audit/config/index.html)

### Other languages and build platforms
If your project is built using other languages or build platforms, checkout the [list of analyzers](https://jeremylong.github.io/DependencyCheck/analyzers/index.html) offered by the OWASP Dependency Check plugin.

There is also a [GitHub Dependency Check Action](https://github.com/dependency-check/Dependency-Check_Action) that uses a [nightly build of the CVE database](https://hub.docker.com/r/owasp/dependency-check-action), along with the Dependency check plugin.

## Static code analysis
To identify bugs in the *upstream* code, that is, code that is written and hosted in your own repository, there are several tools out there; the one that works well for us is https://semgrep.dev , and we designed a GitHub Action in `.github/workflows/semgrep.yml` that continuously runs scans on every code change.

Semgrep supports a [long list of programming languages](https://semgrep.dev/docs/supported-languages/) and defines a [rich list of rulesets](https://semgrep.dev/explore) that tests the code against.

It also provides ways to [ignore false positives](https://semgrep.dev/docs/ignoring-files-folders-code/) by:
1. adding a `//nosemgrep` (or `#nosemgrep`) comment on top of the code block that causes the error
2. adding a `.semgrepignore` file with a list of file names that should be ignored during the scan

In order to use it, you need to
1. Sign up for free on https://semgrep.dev and generate a token
2. Create a GitHub Secret called `SEMGREP_APP_TOKEN`, with the token earlier created as value
3. Run `semgrep scan --error --config auto`

## Roadmap
1. Add documentation into [community.finos.org](community.finos.org)
2. Publish post on FINOS blog
3. Push for adoption across FINOS projects
4. Build a (centralized) GitHub Action to check for branch protection across GitHub repositories
5. Build a (centralized) GitHub Action to check which repositories run security-scanning and which don't
6. Generate reports about security scanning usage: a private one, with details about code and repositories, for FINOS Staff use, and a public one, with anonymized data

## Contributing
For any bug, question or enhancement request, please [create a GitHub Issue](https://github.com/finos/security-scanning/issues)
1. Fork it (<https://github.com/finos/security-scanning/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool (or [EasyCLA](https://community.finos.org/docs/governance/Software-Projects/easycla)). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*


## License

Copyright 2022 FINOS

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)