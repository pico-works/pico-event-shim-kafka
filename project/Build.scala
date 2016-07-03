import sbt.Keys._
import sbt._

object Build extends sbt.Build {  
  val pico_disposal             = "org.pico"          %%  "pico-disposal"             % "0.6.2"
  val pico_event                = "org.pico"          %%  "pico-event"                % "0.2.0"
  val kafka_clients             = "org.apache.kafka"  %   "kafka-clients"             % "0.10.0.0"
  val kafka_server              = "org.apache.kafka"  %%  "kafka"                     % "0.10.0.0"
  val log4j                     = "log4j"             %   "log4j"                     % "1.2.17"

  val specs2_core               = "org.specs2"        %%  "specs2-core"               % "3.7.2"

  implicit class ProjectOps(self: Project) {
    def standard(theDescription: String) = {
      self
          .settings(scalacOptions in Test ++= Seq("-Yrangepos"))
          .settings(publishTo := Some("Releases" at "s3://dl.john-ky.io/maven/releases"))
          .settings(description := theDescription)
          .settings(isSnapshot := true)
    }

    def notPublished = self.settings(publish := {}).settings(publishArtifact := false)

    def libs(modules: ModuleID*) = self.settings(libraryDependencies ++= modules)

    def testLibs(modules: ModuleID*) = self.libs(modules.map(_ % "test"): _*)

    def it = self.configs(IntegrationTest).settings(Defaults.itSettings: _*)

    def itLibs(modules: ModuleID*) = self.libs(modules.map(_ % "it"): _*)
  }

  lazy val `pico-fake` = Project(id = "pico-fake", base = file("pico-fake"))
      .standard("Fake project").notPublished
      .testLibs(specs2_core)

  lazy val `pico-disposal-kafka` = Project(id = "pico-disposal-kafka", base = file("pico-disposal-kafka"))
      .standard("kafka support for pico-disposal")
      .libs(pico_disposal, kafka_clients)
      .testLibs(specs2_core)

  lazy val `pico-event-kafka` = Project(id = "pico-event-kafka", base = file("pico-event-kafka"))
      .standard("pico-event shim library for kafka")
      .dependsOn(`pico-disposal-kafka`)
      .libs(pico_event, kafka_clients)
      .testLibs(specs2_core)

  lazy val all = Project(id = "pico-kafka-project", base = file("."))
      .notPublished
      .aggregate(`pico-disposal-kafka`, `pico-event-kafka`, `pico-fake`)
}
