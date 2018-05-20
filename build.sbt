// Build setup imitated from https://github.com/FasterXML/jackson-module-scala

name := "pelam-scala-csv"

description := """
Scala library for reading and writing CSV data with an optional high level
API. Supports structured access to tabular data and a form of CSV format detection.
"""

organization := "fi.pelam"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.11", "2.12.4")

scalacOptions ++= Seq()

javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8"
)

scalacOptions ++= (
  if (scalaVersion.value.startsWith("2.12")) {
    Seq.empty
  } else {
    // Explicitly target 1.8 for scala < 2.12
    Seq("-target:jvm-1.8")
  }
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  // For Java like enums with small amount of
  // boilerplate https://github.com/lloydmeta/enumeratum
  // Used in tests for row and column typing.
  "com.beachape" %% "enumeratum" % "1.5.12",
  // Used for reading resource in tests.
  "com.google.guava" % "guava" % "23.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.0" % Test,
  "junit" % "junit" % "4.12" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  // https://stackoverflow.com/a/28051194/1148030
  "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep")

)

val printTests = taskKey[Unit]("something")

printTests := {
  val tests = (definedTests in Test).value
  println("Tests!")
  tests map { t =>
    println(t.name)
  }
}

// ------------ RELEASE

// publishing
publishMavenStyle := true

releaseCrossBuild := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials_sonatype")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := {
  <url>https://github.com/pelamfi/pelam-scala-csv</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:git@github.com:pelamfi/pelam-scala-csv.git</connection>
      <developerConnection>scm:git:git@github.com:pelamfi/pelam-scala-csv.git</developerConnection>
      <url>https://github.com/pelamfi/pelam-scala-csv/blob/master/readme.md</url>
    </scm>
    <developers>
      <developer>
        <id>pelamfi</id>
        <name>Peter Lamberg</name>
        <email>pgithub@pelam.fi</email>
      </developer>
    </developers>
    <contributors>
    </contributors>
}

// use maven style tag name
releaseTagName := s"${name.value}-${(version in ThisBuild).value}"

// sign artifacts

releasePublishArtifactsAction := PgpKeys.publishSigned.value

// https://www.scala-sbt.org/sbt-pgp/usage.html
useGpg := true
pgpSecretRing := pgpPublicRing.value

// More settings in ~/.sbt/1.0/global.sbt ~/.sbt/gpg.sbt