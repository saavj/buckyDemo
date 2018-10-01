name := "buckyDemo"

version := "0.1"

scalaVersion := "2.12.6"

val buckyVersion = "1.3.2"
val http4sVersion = "0.18.15"

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

scalacOptions ++= Seq("-Ypartial-unification")

libraryDependencies ++= Seq(
  "com.itv" %% "bucky-core" % buckyVersion,
  "com.itv" %% "bucky-rabbitmq" % buckyVersion,
  "com.itv" %% "bucky-fs2" % buckyVersion,
  "com.itv" %% "bucky-circe" % buckyVersion,
  "com.itv" %% "bucky-test" % buckyVersion % "test,it" exclude("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec"),
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.apache.qpid" % "qpid-broker" % "6.0.4" % "test,it",
  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "0.10.1"
)
        