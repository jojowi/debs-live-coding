name := "chat"

version := "0.1"

scalaVersion := "2.12.8"

organization in ThisBuild := "de.tuda.stg"

version in ThisBuild := "0.0.0"

scalaVersion in ThisBuild := "2.12.8"

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint")

resolvers in ThisBuild += Resolver.bintrayRepo("stg-tud", "maven")



val librariesRescala = libraryDependencies +=
  "de.tuda.stg" %%% "rescala" % "0.19.0"

val librariesUpickle = libraryDependencies +=
  "com.lihaoyi" %%% "upickle" % "0.4.4"

val librariesAkkaHttp = libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12")

val librariesAkkaJs = libraryDependencies +=
  "org.akka-js" %%% "akkajsactor" % "1.2.5.2"

val librariesDom = libraryDependencies +=
  "org.scala-js" %%% "scalajs-dom" % "0.9.7"

val librariesMultitier = libraryDependencies ++= Seq(
  "de.tuda.stg" %%% "scala-loci-lang" % "0.3.2",
  "de.tuda.stg" %%% "scala-loci-serializer-upickle" % "0.3.2",
  "de.tuda.stg" %%% "scala-loci-communicator-ws-akka" % "0.3.2",
  "de.tuda.stg" %%% "scala-loci-communicator-webrtc" % "0.3.2",
  "de.tuda.stg" %%% "scala-loci-lang-transmitter-rescala" % "0.3.2",
  "com.lihaoyi" %%% "scalatags" % "0.6.7",
  "de.tuda.stg" %%%! "rescalatags" % "0.19.0")

val librariesClientServed = Seq(
  dependencyOverrides += "org.webjars.bower" % "jquery" % "1.12.0",
  libraryDependencies += "org.webjars.bower" % "bootstrap" % "3.3.6",
  libraryDependencies += "org.webjars.bower" % "webrtc-adapter" % "0.2.5")


// addCompilerPlugin("de.tuda.stg" % "dslparadise" % "0.2.0" cross CrossVersion.patch)

val macroparadise = addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)


def standardDirectoryLayout(directory: File): Seq[Def.Setting[_]] =
  standardDirectoryLayout(Def.setting { directory })

def standardDirectoryLayout(directory: Def.Initialize[File]): Seq[Def.Setting[_]] = Seq(
  unmanagedSourceDirectories in Compile += directory.value / "src" / "main" / "scala",
  unmanagedResourceDirectories in Compile += directory.value / "src" / "main" / "resources",
  unmanagedSourceDirectories in Test += directory.value / "src" / "test" / "scala",
  unmanagedResourceDirectories in Test += directory.value / "src" / "test" / "resources")

val sharedMultitierDirectories =
  standardDirectoryLayout(Def.setting { baseDirectory.value.getParentFile })

val settingsMultitier =
  sharedMultitierDirectories ++ Seq(macroparadise, librariesMultitier, librariesAkkaHttp)


lazy val chat = (project in file("chat") / ".all"
  settings (run in Compile :=
  ((run in Compile in chatJVM) dependsOn
    (fastOptJS in Compile in chatJS)).evaluated)
  aggregate (chatJVM, chatJS))

lazy val chatJVM = (project in file("chat") / ".jvm"
  settings (settingsMultitier: _*)
  settings (librariesClientServed: _*)
  settings (resources in Compile ++=
  ((crossTarget in Compile in chatJS).value ** "*.js").get))

lazy val chatJS = (project in file("chat") / ".js"
  settings (settingsMultitier: _*)
  settings (scalaJSUseMainModuleInitializer in Compile := true)
  settings (scalaJSMainModuleInitializer in Compile := Some(org.scalajs.core.tools.linker.ModuleInitializer.mainMethod(
  "chat.Client", "main")))
  enablePlugins ScalaJSPlugin)