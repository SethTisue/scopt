import com.typesafe.sbt.pgp.PgpKeys._

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

// these are repeated in .travis.yml
lazy val scala211 = "2.11.12"
lazy val scala212 = "2.12.7"
lazy val scala213 = "2.13.0-M5"

lazy val root = project.in(file(".")).
  aggregate(scoptJS, scoptJVM, scoptNative).
  settings(
    publish := {},
    publishLocal := {},
    skip in publish := true)

lazy val scopt = (crossProject(JSPlatform, JVMPlatform, NativePlatform) in file(".")).
  settings(
    inThisBuild(Seq(
      version := "3.7.0",
      scalaVersion := scala212,
      crossScalaVersions := Seq(scala212, scala211, scala213)
    )),
    name := "scopt",
    // site
    // to preview, preview-site
    // to push, ghpages-push-site
    siteSubdirName in SiteScaladoc := "$v/api",
    git.remoteRepo := "git@github.com:scopt/scopt.git",
    description := """a command line options parsing library""",
    scalacOptions ++= Seq("-language:existentials", "-Xfuture", "-deprecation"),
    resolvers += "sonatype-public" at "https://oss.sonatype.org/content/repositories/public",
    // scaladoc fix
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
  ).
  platformsSettings(JVMPlatform, JSPlatform)(
    libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.1" % Test
  ).
  jsSettings(
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scopt/scopt/" + sys.process.Process("git rev-parse HEAD").lineStream_!.head
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
  ).
  nativeSettings(
    sources in Test := Nil, // TODO https://github.com/monix/minitest/issues/12
    scalaVersion := scala211,
    crossScalaVersions := Nil
  )

val minitestJVMRef = ProjectRef(IO.toURI(workspaceDirectory / "minitest"), "minitestJVM")
val minitestJVMLib = "io.monix" %% "minitest" % "2.2.2"

lazy val scoptJS = scopt.js
  .settings(
    libraryDependencies ++= Seq(
      "com.eed3si9n.expecty" %%% "expecty" % "0.11.0" % Test,
      "io.monix" %%% "minitest" % "2.2.2" % Test,
    ),
    testFrameworks += new TestFramework("minitest.runner.Framework")
  )

lazy val scoptJVM = scopt.jvm.enablePlugins(SiteScaladocPlugin)
  .sourceDependency(minitestJVMRef % Test, minitestJVMLib % Test)
  .settings(
    libraryDependencies += "com.eed3si9n.expecty" %% "expecty" % "0.11.0" % Test,
    testFrameworks += new TestFramework("minitest.runner.Framework")
  )

lazy val scoptNative = scopt.native

lazy val nativeTest = project.in(file("nativeTest")).
  dependsOn(scoptNative).
  enablePlugins(ScalaNativePlugin).
  settings(scalaVersion := scala211)
