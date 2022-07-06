# security-comparison
What's the best security tool out there?

## Gradle
The Gradle build uses the [Dependency Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html).

The `build.gradle` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail.

Check `gradle/build.gradle` and `.github/workflows/gradle.yml` for more info.

## Maven
The maven project uses the [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) plugin to scan runtime dependencies for known vulnerabilities.

The `pom.xml` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail.

Check `maven/pom.xml` and `.github/workflows/maven.yml` for more info.

## Node
The NodeJS project uses [AuditJS](https://www.npmjs.com/package/auditjs), which limits scope only to non dev dependencies by default.

In `node/package.json.vulnerable` you'll notice one additional `dependency`, `chokidar: 2.0.3`, which introduces 2 CVEs; to test it, simply `cp node/package.json.vulnerable node/package.json` and re-run `npm install ; npx --yes auditjs ossi`.

Check `node/package.json` and `.github/workflows/node.yml` for more info.

## Python
The python project is built with [Poetry](https://python-poetry.org/), see `python/pyproject.toml`.

To scan for CVEs, we use:
1. `poetry export` command, which generates a `requirements.txt` file
2. The [`safety` library](https://pyup.io/safety/), which checks `requirements.txt` entries against NVD DB.

The `python/pyproject.toml` includes a commented dependency called `insecure-package`, which introduces CVEs into the project and tests whether the scan works properly or not.

Check `python/pyproject.toml` and `.github/workflows/poetry.yml` for more info.

## Scala
The Scala project uses the [`sbt-dependency-check` plugin](https://github.com/albuch/sbt-dependency-check) to scan incoming dependencies for CVEs.

The `build.sbt` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail.

Check `scala/build.sbt` folder and `.github/workflows/scala.yml` for more info.
