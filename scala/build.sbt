// Introduces a CVE, leading to a build fail
// val struts = "org.apache.struts" % "struts2-core" % "2.3.8"
val struts = "org.apache.struts" % "struts2-core" % "6.0.0"

lazy val root = (project in file(".")).
  settings(
    name := "hello-world",
    test in Test := {
      val _ = (g8Test in Test).toTask("").value
    },
    libraryDependencies += struts,
    dependencyCheckFailBuildOnCVSS := 5,
    scriptedLaunchOpts ++= List("-Xms1024m", "-Xmx1024m", "-XX:ReservedCodeCacheSize=128m", "-XX:MaxPermSize=256m", "-Xss2m", "-Dfile.encoding=UTF-8"),
    resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  )
