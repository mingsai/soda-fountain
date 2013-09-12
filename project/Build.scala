import sbt._

object Build extends sbt.Build {
  lazy val build = Project(
    "soda-fountain",
    file(".")
  ).settings(BuildSettings.buildSettings : _*).aggregate(allOtherProjects: _*)

  private def allOtherProjects =
    for {
      method <- getClass.getDeclaredMethods.toSeq
      if method.getParameterTypes.isEmpty && classOf[Project].isAssignableFrom(method.getReturnType) && method.getName != "build"
    } yield method.invoke(this).asInstanceOf[Project] : ProjectReference

  private def p(name: String, settings: { def settings: Seq[Setting[_]] }, dependencies: ClasspathDep[ProjectReference]*) =
    Project(name, file(name)).settings(settings.settings : _*).dependsOn(dependencies: _*)

  val sodaFountainLibMacros = p("soda-fountain-lib-macros", SodaFountainLibMacros)

  val sodaFountainLib = p("soda-fountain-lib", SodaFountainLib,
    sodaFountainLibMacros)

  val sodaFountainJetty = p("soda-fountain-jetty", SodaFountainJetty,
    sodaFountainLib)

  val sodaFountainWar = p("soda-fountain-war", SodaFountainWar,
    sodaFountainLib)
}
