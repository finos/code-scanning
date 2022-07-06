# security-comparison
What's the best security tool out there?

## Gradle
*TODO - add a vulnerable dependency, test, then comment out*

The Gradle build uses the [Dependency Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html).

Check `gradle/build.gradle` and `.github/workflows/gradle.yml` for more info.

## Maven
*TODO - add a vulnerable dependency, test, then comment out*

The maven project uses the [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) plugin to scan runtime dependencies for known vulnerabilities.

Check `maven/pom.xml` and `.github/workflows/maven.yml` for more info.

## Node
*TODO - add a vulnerable dependency, test, then comment out*

The NodeJS project uses `npm audit` built-in command, adding the `--omit=dev` option, to limit scope only to non dev dependencies.

Check `node/package.json` and `.github/workflows/node.yml` for more info.


## Python
The python project is built with [Poetry](https://python-poetry.org/), see `python/pyproject.toml`.

To scan for CVEs, we use:
1. `poetry export` command, which generates a `requirements.txt` file
2. The [`safety` library](https://pyup.io/safety/), which checks `requirementrs.txt` entries against NVD DB.

The `python/pyproject.toml` includes a commented dependency called `insecure-package`, which introduces CVEs into the project and tests whether the scan works properly or not.

Check `python/pyproject.toml` and `.github/workflows/poetry.yml` for more info.

## Scala
*TODO - add a vulnerable dependency, test, then comment out*

The Scala project uses the [`sbt-dependency-check` plugin](https://github.com/albuch/sbt-dependency-check) to scan incoming dependencies for CVEs.

Check `scala/project` folder and `.github/workflows/scala.yml` for more info.
