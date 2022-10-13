# FINOS Security Scanning

[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)
[![Gradle CI](https://github.com/finos/security-scanning/actions/workflows/gradle.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/gradle.yml)
[![Maven CI](https://github.com/finos/security-scanning/actions/workflows/maven.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/maven.yml)
[![Node.js CI](https://github.com/finos/security-scanning/actions/workflows/node.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/node.yml)
[![Poetry CI](https://github.com/finos/security-scanning/actions/workflows/python.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/python.yml)
[![Rust CI](https://github.com/finos/security-scanning/actions/workflows/rust.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/rust.yml)
[![Scala CI](https://github.com/finos/security-scanning/actions/workflows/scala.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/scala.yml)
[![Static code analysis](https://github.com/finos/security-scanning/actions/workflows/semgrep.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/semgrep.yml)

## The problem

Given the wide range of platforms, languages and build systems used by FINOS projects, finding one solution that secures a codebase is not an easy task, especially considering the incredible amount of libraries available in public library repositories, which can be easily used, embedded, integrated and re-published; this proliferation of artifacts have dramatically influenced software development:

- On average, 95% of the code shipped in a software artifact is composed of downstream libraries (aka dependencies), built, released and managed by external teams, communities and companies that the consumer has no control/influence over.
- A developer has very little awareness of the codebase quality and software development process in the downstream dependencies of a project, unless going through code scrutiny, which is difficult and time consuming
- Every programming language and build tool has a different way of consuming downstream dependencies, making security tool adoption harder and rarer; as a consequence, more security vulnerabilities are released into public library repositories, which leads to the exponential growth of vulnerabilities and risk for all consumers using these libraries

## The solution

Let's first recap requirements, based on the considerations made above; a security scanning should be:
- Proactive (triggered periodically, ie every day) and reactive (triggered on code changes)
- Compatible with all languages and build platforms adopted by FINOS hosted projects
- Easy to operate by project teams, git-based, without the need for external dashboards
- Integrated into FINOS project onboarding process and [FINOS CVE Disclosure Policy](https://community.finos.org/docs/governance/software-projects/cve-responsible-disclosure/)
- Monitorable by FINOS Staff, allowing us to provide a proactive support to our projects

The proactive/reactive approach is crucial to enforce security, as it guarantees - granted that changes are always submitted via Pull Requests - that the code will always be free of CVEs (or at most for less than a day):
- The reactive setup will fail any Pull Request where code change introduces a new CVE from the (updated) dependency list
- The proactive setup will notify (ie on a daily schedule) the team if a new CVE - that affects a library in the dependency list - have been published

Based on the requirements discussed above, we tried to consolidate a list of technical specifications:
- 6 supported build platforms - Maven, Gradle, Python (and Poetry), Scala (with SBT), NodeJS and Rust
- CVE scanning
  - Can be configured to only scan runtime dependencies
  - Scans direct and transitive dependencies
  - Ability to ignore warnings/errors, using a git-hosted file
  - Ability to run as part of CI/CD (GitHub actions)
- Static code analysis
  - Ability to run as part of CI/CD (GitHub actions)
- Documentation that describes how to use the scanning and what to expect

It's worth emphasizing the importance of having a mechanism for ignoring warnings and errors, which may well be false positives.  Without it, developers will eventually disable any security tool. And it’s important to store it in the codebase so that changes can be easily done by developers using the Git collaboration workflow.

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
      - run: npm install --prod
      - run: npx --yes auditjs ossi --whitelist allow-list.json
```

Also create a `allow-list.json` file in the project root, check an example in the `node` folder.

If you want to test it, add a dependency against `"chokidar" : "2.0.3"`(https://pypi.org/project/insecure-package/), re-run the `npx --yes auditjs ossi --whitelist allow-list.json` command mentioned in the GitHub Action above and expect the build to fail.

### Python
For Python projects we recommend using the [`safety` library](https://pyup.io/safety/) library, which checks `requirements.txt` entries against NVD DB.

To enable the CVE scanning on your repository, follow these simple steps:

1. Create a `safety-policy.yml` file in your project's root folder, which will define which errors/warnings to suppress as false positives; you can find an example [in the python folder](https://github.com/finos/security-scanning/blob/main/python/safety-policy.yml)
2. Create a new file called `.github/workflows/cve-scanning.yml` and paste this content:

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

Make sure to create a `allow-list.xml` file, which will define which errors/warnings to suppress as false positives; you can find a sample file in the root of this project.

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
2. Create a GitHub Secret called `SEMGREP_APP_TOKEN`, with the token earlier created as value. If you want to enable scanning on a FINOS hosted repository, please email [help@finos.org](mailto:help@finos.org) and they will take care of setting the `SEMGREP_APP_TOKEN` secret on the GitHub repository.
3. Run `semgrep scan --error --config auto`

In order to test it locally, make sure to:
1. [Install Semgrep](https://semgrep.dev/docs/getting-started/)
2. Signup to [semgrep.dev](semgrep.dev)
3. Generate a token, using the `Settings` menu option
4. `export SEMGREP_APP_TOKEN=<your personal semgrep token>`
5. Run `semgrep scan --error --config auto` from the root folder

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
