import mill._, scalalib._

// `HelloWorld` must match the subdirectory name
object HelloWorld extends ScalaModule {
    def scalaVersion = "2.12.11"

    def ivyDeps = Agg(
        ivy"com.lihaoyi::sourcecode:0.2.8",
        ivy"io.getquill::quill-util:3.16.3",
        ivy"com.softwaremill.common::tagging:2.3.3",
        ivy"org.scala-lang.modules::scala-collection-compat:2.7.0",
        ivy"io.github.cquiroz::scala-java-time:2.4.0-M3"
    )

    // pass options to the scalac compiler? (not working)
    //def scalacOptions = super.scalacOptions() ++ Seq(
    //    "-feature", "-deprecation"
    //)

    // pass options to the scalac compiler? (not working)
    //def scalacOptions = Seq(
    //    "-deprecation",
    //    "-feature"
    //)

    // for the HelloWorld/test directory
    object test extends Tests {
        def ivyDeps = Agg(
            ivy"org.scalactic::scalactic:3.1.1",
            ivy"org.scalatest::scalatest:3.1.1"
        )
        def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
}


