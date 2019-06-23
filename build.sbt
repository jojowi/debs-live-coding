name := "DistributedHashMap"

resolvers in ThisBuild += Resolver.bintrayRepo("stg-tud", "maven")
lazy val commonSettings = Seq(
  organization := "de.tuda.stg",
  version := "0.0.0",
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "0.4.4",
    "de.tuda.stg" %% "rescala" % "0.19.0",
    "de.tuda.stg" %% "scala-loci-lang" % "0.3.2",
    "de.tuda.stg" %% "scala-loci-serializer-upickle" % "0.3.2",
    "de.tuda.stg" %% "scala-loci-communicator-tcp" % "0.3.2",
    "de.tuda.stg" %% "scala-loci-lang-transmitter-rescala" % "0.3.2"
  ),
  scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
)

lazy val hashmap = (project in file("./"))
  .settings(commonSettings)

