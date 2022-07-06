lazy val root = (project in file(".")).
  settings(
    name := "hello-world",
    test in Test := {
      val _ = (g8Test in Test).toTask("").value
    },
    // Introduces a CVE, leading to a potential build fail
    libraryDependencies += "org.apache.struts" % "struts2-core" % "2.3.8",
    dependencyCheckFailBuildOnCVSS := 5,
    // Add a suppression file, to test false positive suppression
    dependencyCheckSuppressionFiles ++= List(file("../suppressions.xml")),
    scriptedLaunchOpts ++= List("-Xms1024m", "-Xmx1024m", "-XX:ReservedCodeCacheSize=128m", "-XX:MaxPermSize=256m", "-Xss2m", "-Dfile.encoding=UTF-8"),
    resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  )
