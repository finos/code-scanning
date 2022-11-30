<img src="https://github.com/finos/finos-landscape/blob/master/hosted_logos/finos-security-scanning.svg" alt="FINOS Security Scanning" width="200"/>

[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)
[![Renovate](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com)
[![Gradle CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-gradle.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-gradle.yml)
[![Maven CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-maven.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-maven.yml)
[![Node.js CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-node.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-node.yml)
[![Poetry CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-python.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-python.yml)
[![Rust CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-rust.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-rust.yml)
[![Scala CI](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-scala.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/cve-scanning-scala.yml)
[![Static code analysis](https://github.com/finos/security-scanning/actions/workflows/semgrep.yml/badge.svg)](https://github.com/finos/security-scanning/actions/workflows/semgrep.yml)

## Table of contents
- [The problem](#The-problem)
- [The solution](#The-solution)
- [Project Layout](#Project-Layout)
- [Enabling CVE scanning in your project](#Enabling-CVE-scanning-in-your-project)
  - [NodeJS](#NodeJS)
  - [Python](#Python)
  - [Maven](#Maven)
  - [Gradle](#Gradle)
  - [Scala](#Scala)
  - [Rust](#Rust)
  - [.NET](#net)
  - [Docker](#Docker)
  - [Other languages and build platforms](#Other-languages-and-build-platforms)
- [Dependency update tool](#Dependency-update-tool)
- [Static code analysis](#Static-code-analysis)
- [License reporting and scanning](#License-reporting-and-scanning)
- [Roadmap](#Roadmap)
- [Contributing](#Contributing)
- [License](#License)

## The problem

Given the wide range of platforms, languages and build systems used by FINOS projects, finding one solution that secures a codebase is not an easy task, especially considering the incredible amount of libraries available in public library repositories, which can be easily used, embedded, integrated and re-published; this proliferation of artifacts have dramatically influenced software development:

- On average, 95% of the code shipped in a software artifact is composed of upstream libraries (aka dependencies), built, released and managed by external teams, communities and companies that the consumer has no control/influence over.
- A developer has very little awareness of the codebase quality and software development process in the upstream dependencies of a project, unless going through code scrutiny, which is difficult and time consuming
- Every programming language and build tool has a different way of consuming dependencies, making security tool adoption harder and rarer; as a consequence, more security vulnerabilities are released into public library repositories, which leads to the exponential growth of vulnerabilities and risk for all consumers using these libraries

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

## Project Layout

In this codebase you'll find a folder for each of the build platforms listed below. Each folder includes a `Hello World` project, with a build descriptor that:

1. Pulls in a CVE
2. Configures a CVE scanning tool that is specific to the build tool
3. Defines a list of ignored warnings/errors caused by the CVE scanning

In the `.github/workflows` folder you'll find a GitHub Action for each of these projects, that you can simply copy/paste into your GitHub repository and edit to align to your project layout:
- Update the name of the GitHub Action file to `cve-scanning.yml` (and `semgrep.yml`), which allows FINOS to monitor which projects are/aren't adopting the tool; the file name is also reflected in the `on: / push: / path:` section of the action
- If the build files are located in the root project folder, remove all `working-directory` configurations
- Adapt runtime versions (ie Node, Python, JVM, etc) with the ones used in your projects

## Enabling CVE scanning in your project

1. Identify the language(s) and build system(s) used in the repository you want to scan.
2. Checkout your repository locally.
3. Find - from the list below - which sections applies to you.
4. Follow the instructions to run the scan locally, make sure that the scan runs successfully and generates a list of CVEs.
5. Investigate CVEs, one by one; the majority of CVEs can be addressed by updating a given library to a newer version; in some cases, you'll find out that you're using a certain library in an unsecure way; in some other cases, you may stumble on false positives (that is, a CVE that doesn't apply to your codebase) and therefore you'd have to ignore the error by updating the ignore list file.
6. Copy the related GitHub Action (in `.github/workflows`) into your project; make sure to call them `cve-scanning.yml`, so that FINOS monitoring tools can find easily find it.
7. From the GitHub `Actions` tab, you can select the `CVE Scanning` action and `Create status badge`, which will allow you to copy Markdown code for your `README.md` file that shows a badge with the result of the last action run; this is quite useful for consumers to see that code is scanned and that no CVEs were spotted in the main codebase branch.
8. Push the changes to GitHub and checkout the Github Action run and output.

## Supported languages

### NodeJS

The NodeJS sample project uses [AuditJS](https://www.npmjs.com/package/auditjs), a library built by Sonatype which provides a very good alternative to `npm audit`; you can read more about their comparison on https://blog.sonatype.com/compare-npm-audit-versus-auditjs .

The [project descriptor](https://github.com/finos/security-scanning/blob/readme-improvement/node/package.json) pulls the `chokidar 2.0.3` dependency, which contains some CVEs that are ignored into the list of ignored errors.

To run `AuditJS` locally:
1. Access the folder that contains the `package.json` file
2. Cleanup the codebase from previous runs - `rm -rf node_modules package-lock.json yarn.lock`
3. Install (only runtime) dependencies - `npm ci --prod` ; if using yarn, the command should be `yarn install --production --frozen-lockfile`
4. Run AuditJS - `npx --yes auditjs ossi`
5. If you want to ignore errors, create an [allow-list.json](node/allow-list.json) file and append ` --whitelist allow-list.json` to the command on step 4

The GitHub action can be copied from [here](.github/workflows/cve-scanning-node.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

### Python

The Python sample project uses the [`safety` library](https://pyup.io/safety/), which checks `requirements.txt` entries against the [NVD database](https://nvd.nist.gov/).

The python sample project defines a dependency on [`insecure-package`](https://pypi.org/project/insecure-package/), which pulls a CVE that is ignored in the `safety-policy.yml`, in order to demo how to manage false positives.

To run `Safety` locally:
1. Access the folder containing the `requirements.txt` file
2. Make sure you're running Python 3.x using `python --version`, otherwise the version of `safety` that you're able to use would be quite outdated
3. Install safety with `pip install safety`
4. Run safety with `safety check --full-report -r requirements.txt`
5. If you want to ignore errors, create a [safety-policy.yml](python/safety-policy.yml) and append ` --policy-file safety-policy.yml` to the command on step 4

If you're using [Poetry](https://python-poetry.org/), you can simply export your libaries into a `requirements.txt` file and then follow the steps above, using:
```
poetry install
poetry export --without-hashes -f requirements.txt --output requirements.txt
```

The GitHub action can be copied from [here](.github/workflows/cve-scanning-python.yml) into your repo under `.github/workflows/cve-scanning-python.yml`; make sure to adapt the code to your [project layout](#project-layout).

### Maven

The Maven sample project uses the [OWASP Dependency Check plugin for Maven](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) to scan runtime dependencies for known vulnerabilities.

To run the `Maven Dependency Check Plugin` locally:
1. Access the folder containing the `pom.xml` file (it supports multi-module builds)
2. Run `mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7`
3. If you want to ignore errors, create an [allow-list.xml](allow-list.xml) and append ` -DsuppressionFile="allow-list.xml"` to the command on step 2

The GitHub action can be copied from [here](.github/workflows/cve-scanning-maven.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

### Gradle

The Gradle sample project uses the [OWASP Dependency Check plugin for Gradle](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html). Sadly, Gradle [doesn't allow to invoke plugins without altering the build manifest](https://discuss.gradle.org/t/invoking-tasks-provided-by-a-plugin-without-altering-the-build-file/27235), namely `build.gradle`; follow instructions below to know how to add security scanning in your project.

To run the `Gradle Dependency Check Plugin` locally:
1. Access the folder containing the `build.gradle` file
2. Copy the [allow-list.xml](allow-list.xml) file into your project and remove all `<suppress>` items
3. Copy the `dependencyCheck` setup from [build.gradle](gradle/build.gradle) file into your `build.gradle` file
4. Run `./gradlew dependencyCheckAnalyze`

The `build.gradle` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the (famous) [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `allow-list.xml` file, used to manage false positives.

The GitHub action can be copied from [here](.github/workflows/cve-scanning-gradle.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

### Scala

The Scala sample project uses the [OWASP Dependency Check plugin for SBT](https://github.com/albuch/sbt-dependency-check) to scan runtime dependencies for known vulnerabilities.

To run the `Scala Dependency Check Plugin` locally:
1. Access the root project folder
2. Copy `dependencyCheckFailBuildOnCVSS` and `dependencyCheckSuppressionFiles` configurations from [build.sbt](scala/build.sbt) file in your project
3. Copy the `sbt-dependency-check` plugin definition from [plugins.sbt](scala/project/plugins.sbt) into your project
4. Run `sbt dependencyCheck`

The `build.sbt` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the (famous) [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `allow-list.xml` file, used to manage false positives.

The GitHub action can be copied from [here](.github/workflows/cve-scanning-scala.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

To keep your library dependencies, sbt plugins, and Scala and sbt versions up-to-date, checkout [Scala Steward](https://github.com/scala-steward-org/scala-steward).

### Rust

The Rust sample project uses [Cargo audit](https://crates.io/crates/cargo-audit) to scan runtime dependencies for known vulnerabilities.

To run `Cargo Audit` locally:
1. Access the root project folder
2. Install Cargo audit with `cargo install --force cargo-audit`
3. Run the scan with `cargo audit`
4. Append `--ignore RUSTSEC-2020-0071` to the command on step 3

The GitHub action can be copied from [here](.github/workflows/cve-scanning-rust.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

For more information about Cargo audit configuration, visit [https://docs.rs/cargo-audit/0.17.0/cargo_audit/config/index.html](https://docs.rs/cargo-audit/0.17.0/cargo_audit/config/index.html)

### .NET

The .NET sample project uses the [dotnet](https://learn.microsoft.com/en-us/dotnet/core/tools/dotnet-list-package) CLI to scan runtime dependencies for known vulnerabilities.

To run `dotnet` locally:
1. Access the root project folder, where your `.csproj` file is defined
2. Install [.NET CLI](https://learn.microsoft.com/en-us/dotnet/core/tools/)
3. Run the scan with `dotnet list package --vulnerable --include-transitive`

The GitHub action can be copied from [here](.github/workflows/cve-scanning-dotnet.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

Unfortunately there is no way yet to ignore warnings and errors for `dotnet`, although it may be possible to add some bash logic into the GitHub Action to achieve it.

### Docker

Docker scanning can be very useful to check if downstream Docker images are affected by vulnerabilities; it also scans for OS components and provides a solution for projects using C and C++ code.

There are many CLI tools that perform a docker image scanning; the easiest one is [docker scan](https://docs.docker.com/engine/scan/), as you'll probably have the `docker` command installed in your local environment, assuming you're already working with Docker.

For GitHub Actions, we are using [trivy][trivy.dev], wrapped into [this GitHub Action](https://github.com/crazy-max/ghaction-container-scan).

To run locally, [follow instructions on how to install trivy locally](https://github.com/aquasecurity/trivy#get-trivy), then run:
1. `docker build -f Dockerfile -t user/image-name:latest`
2. `trivy image user/image-name:latest`

The GitHub action can be copied from [here](.github/workflows/cve-scanning-docker.yml) into your repo under `.github/workflows/cve-scanning.yml`; make sure to adapt the code to your [project layout](#project-layout).

Unfortunately there is no way yet to ignore warnings and errors, although it may be possible to add some bash logic into the GitHub Action to achieve it.

### Other languages and build platforms
If your project is built using other languages or build platforms, checkout the [list of analyzers](https://jeremylong.github.io/DependencyCheck/analyzers/index.html) offered by the OWASP Dependency Check plugin.

There is also a [GitHub Dependency Check Action](https://github.com/dependency-check/Dependency-Check_Action) that uses a [nightly build of the CVE database](https://hub.docker.com/r/owasp/dependency-check-action), along with the Dependency check plugin.

## Dependency update tool

Keeping dependency versions up to date is a hard task, given the big amount of downstream libraries used by today's software projects and their frequent release cadence; adopting a tool to automate it can drastically save time for developers.

Github ships with Dependabot, which can be easily enabled on every repository, but we found [Renovate](https://www.mend.io/free-developer-tools/renovate/) to be easier and more powerful to use, you can read [this comparison article](https://blog.frankel.ch/renovate-alternative-dependabot/), if you're interested.

Assuming that Renovate is running on your project, the CVE scanning tools mentioned above would generate way less alerts, making it easier to manage project's security; as a result, we strongly advise to enable Renovate first, then add CVE scanning tools.

In order to enable Renovate:
1. Email help@finos.org and request enabling the Github App on your FINOS repositories
2. Merge the Pull Request that gets generated after step 1

Renovate will create a GitHub issue (titled `Renovate Dashboard`) with the recap of the actions that it will take and a one Pull Request for each depdendency version update; please note that:
- The list of Pull Requests sent daily is limited to 10.
- In order to ignore an update, simply close its related Pull Request; Renovate won't ask for the update anymore, unless requested via the `Renovate Dashboard` issue.

Note that Renovate can be configured to group multiple updates together, using the [`groupName` feature](https://docs.renovatebot.com/configuration-options/#groupname), which can save a lot of developers time, expecially on large codebases.

## Static code analysis
To identify bugs in the hosted source code, that is, code that is written and hosted in your own repository, there are several tools out there; the one that proved to work well for us is https://semgrep.dev , and we designed a GitHub Action in `.github/workflows/semgrep.yml` that continuously scans the code upon every change.

Semgrep supports a [long list of programming languages](https://semgrep.dev/docs/supported-languages/) and defines a [rich list of rulesets](https://semgrep.dev/explore) that tests the code against.

It also provides ways to [ignore false positives](https://semgrep.dev/docs/ignoring-files-folders-code/) by:
1. adding a `// nosemgrep` (or `# nosemgrep`) comment on top of the code block that causes the error
2. adding a `.semgrepignore` file with a list of file names that should be ignored during the scan

In order to use it, you need to
1. Sign up for free on https://semgrep.dev and generate a token
2. Create a GitHub Secret called `SEMGREP_APP_TOKEN`, with the token earlier created as value. If you want to enable scanning on a FINOS hosted repository, please email [help@finos.org](mailto:help@finos.org) and they will take care of setting the `SEMGREP_APP_TOKEN` secret on the GitHub repository.
3. Run `semgrep scan --error --config auto`

In order to test it locally, make sure to:
1. [Install Semgrep](https://semgrep.dev/docs/getting-started/)
2. Signup to [semgrep.dev](semgrep.dev)
3. Generate a token, using the `Settings` menu option
4. (optional) `export SEMGREP_APP_TOKEN=<your personal semgrep token>` - to aggregate results on FINOS (private) dashboard
5. Run `semgrep scan --error --config auto` from the root folder

## License reporting and scanning

To enforce compliance of open source projects, it is crucial to validate that inbound libraries adopt a license that is "compatible" with the outbound one in terms of rights and obligations; for FINOS, the outbound license used is the [Apache License v2.0](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)).

There are hundreds of different open source licenses, some of which have conflicting clauses (you can learn more on [tldrlegal.com](https://tldrlegal.com)), and it's sometimes hard to understand the consequences of adopting a library with a different license than the outbound one, especially without having some legal background or knowledge.

For this reason, we are working on automated tasks to continuously scan licenses being pulled within FINOS projects; such tools should be able to either:
- Run a scanning process that takes as input the list of allowed licenses and the packages to ignore (preferred)
- Build a report of licenses that can be manually reviewed and checked

Right now, we have managed to automate [license scanning on Maven](.github/workflows/license-scanning-maven.yml), [Python](.github/workflows/license-scanning-python.yml) and [Node.js](.github/workflows/license-scanning-node.yml) and our intention is to cover also other languages/platforms with the same mechanisms.

For more info about compliance requirements at FINOS, checkout our [Contribution Compliance Requirements](https://community.finos.org/docs/governance/Software-Projects/contribution-compliance-requirements) and [License Categories](https://community.finos.org/docs/governance/Software-Projects/license-categories) pages.

## Roadmap
1. ~~Add documentation into [community.finos.org](community.finos.org)~~
2. ~~Publish post on FINOS blog~~ - https://www.finos.org/blog/introducing-finos-security-scanning
3. Push for adoption across FINOS projects
4. Add license reporting and scanning features
5. Add support for C#
6. Add support for [mill](https://github.com/com-lihaoyi/mill)

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
