name := "buckyDemo"

version := "0.1"

scalaVersion := "2.12.6"

val buckyVersion = "0.13"
val http4sVersion = "0.18.15"

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

libraryDependencies ++= Seq(
  "com.itv" %% "bucky-core" % buckyVersion,
  "com.itv" %% "bucky-rabbitmq" % buckyVersion,
  "com.itv" %% "bucky-fs2" % buckyVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.apache.qpid" % "qpid-broker" % "6.0.4" % "test,it"
)
        