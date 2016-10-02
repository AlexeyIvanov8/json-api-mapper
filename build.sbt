
import pl.project13.scala.sbt.JmhPlugin

// for super safe compiler plugin
//resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val commonSettings = Seq(
	organization := "com.skn",
	name := "jsonapi-mapper",
	version := "0.1",
	scalaVersion := "2.11.8"
)

lazy val root = (project in file("."))
	.settings(commonSettings:_*)
	
libraryDependencies ++= Seq(
	// https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient  for http status enumeration
	"org.apache.httpcomponents" % "httpclient" % "4.5.2",
	"com.typesafe.play" %% "play-json" % "2.5.8",
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"ch.qos.logback" % "logback-classic" % "1.1.3",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.3"
)

logBuffered in Test := false

enablePlugins(JmhPlugin)

sourceDirectory in Jmh := (sourceDirectory in Test).value
classDirectory in Jmh := (classDirectory in Test).value
dependencyClasspath in Jmh := (dependencyClasspath in Test).value
// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
compile in Jmh <<= (compile in Jmh) dependsOn (compile in Test)
run in Jmh <<= (run in Jmh) dependsOn (Keys.compile in Jmh)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))