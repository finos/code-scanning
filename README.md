# security-comparison
What's the best security tool out there?

## Gradle
The Gradle build uses the [Dependency Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html).

The `build.gradle` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

Simply run `./gradlew dependencyCheckAnalyze` to run the CVE scan; check `gradle/build.gradle` and `.github/workflows/gradle.yml` for more info.

## Maven
The maven project uses the [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) plugin to scan runtime dependencies for known vulnerabilities.

The `pom.xml` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

The CVE scanning is included in the `check` build phase; as such, it will be invoked when running `mvn package`; check `maven/pom.xml` and `.github/workflows/maven.yml` for more info.

## Node
The NodeJS project uses [AuditJS](https://www.npmjs.com/package/auditjs), which limits scope only to non dev dependencies by default.

In `node/package.json` you'll notice that the last `dependency` is `chokidar: 2.0.3`, which introduces 4 CVEs; however, the CVE scanning - which you can run simply using `npm install ; npm run scan-cves` passes, because `whitelist.json` instructs the scanner to ignore such issues.

Check `node/package.json` and `.github/workflows/node.yml` for more info.

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

## Scala
The Scala project uses the [`sbt-dependency-check` plugin](https://github.com/albuch/sbt-dependency-check) to scan incoming dependencies for CVEs.

The `build.sbt` file defines a (commented) dependency on `struts2` version 2.3.8, which contains the CVE that led to the [equifax hack](https://nvd.nist.gov/vuln/detail/cve-2017-5638). By uncommenting it, the build is expected to fail, assuming that CVEs are not suppressed by the `suppressions.xml` file, used to manage false positives.

Simply run `sbt dependencyCheck` to run the CVE scan; check `scala/build.sbt` folder and `.github/workflows/scala.yml` for more info.
