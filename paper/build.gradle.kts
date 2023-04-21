plugins {
  id("carbon.shadow-platform")
  id("net.minecrell.plugin-yml.bukkit")
  id("xyz.jpenilla.run-paper")
  id("carbon.permissions")
}

dependencies {
  implementation(projects.carbonchatCommon)

  // Server
  compileOnly("net.pl3x.purpur:purpur-api:1.16.5-R0.1-SNAPSHOT")
  implementation(libs.paperLib)

  // Commands
  implementation(libs.cloudPaper)

  // Misc
  implementation(libs.bstatsBukkit)

  // Plugins
  compileOnly("me.clip:placeholderapi:2.10.9") // TODO: move this to libs.versions.yml
  compileOnly(libs.essentialsXDiscord)
  compileOnly(libs.discordsrv)
}

carbonShadowPlatform {
  relocateGuice.set(true)
}

tasks {
  shadowJar {
    relocateDependency("io.papermc.lib")
    relocateDependency("io.leangen.geantyref")
  }
  runServer {
    minecraftVersion("1.19.3")
  }
}

// Generates plugin.yml automatically
bukkit {
  name = rootProject.name
  version = project.version as String
  main = "net.draycia.carbon.paper.CarbonPaperBootstrap"
  apiVersion = "1.16"
  author = "Draycia"
  depend = listOf("LuckPerms")
  softDepend = listOf("PlaceholderAPI", "EssentialsDiscord", "DiscordSRV")
  website = GITHUB_REPO_URL
}

carbonPermission.permissions.get().forEach {
  bukkit.permissions.register(it.string) {
    description = it.description
    childrenMap = it.children
  }
}
