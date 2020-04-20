enablePlugins(JavaAppPackaging)

name := "cloudbowl-jamesward"

scalaSource in Compile := baseDirectory.value / "app"

resourceDirectory in Compile := baseDirectory.value / "conf"

scalaSource in Test := baseDirectory.value / "test"

scalaVersion := "2.13.1"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "cask"        % "0.6.0",

  "org.specs2"  %% "specs2-core" % "4.9.3" % "test",
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-explaintypes",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xcheckinit",
  "-Xfatal-warnings",
  "-Xlint:adapted-args",
  "-Xlint:constant",
  "-Xlint:delayedinit-select",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:nullary-override",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
)

Global / cancelable := false
