/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.macro.MacroManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.AntiForge
import net.ccbluex.liquidbounce.features.special.CombatManager
import net.ccbluex.liquidbounce.features.special.DiscordRPC
import net.ccbluex.liquidbounce.features.special.ServerSpoof
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.config.ConfigManager
import net.ccbluex.liquidbounce.launch.EnumLaunchFilter
import net.ccbluex.liquidbounce.launch.LaunchFilterInfo
import net.ccbluex.liquidbounce.launch.LaunchOption
import net.ccbluex.liquidbounce.launch.data.GuiLaunchOptionSelectMenu
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.FontsGC
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.ui.sound.TipSoundManager
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import java.util.*
import javax.swing.JOptionPane
import kotlin.concurrent.thread

object LiquidBounce {

    // Client information
    const val CLIENT_NAME = "Hitam Client"
    const val CLIENT_NAME_NOSPACE = "HitamClient"
    const val COLORED_NAME = "§b§lHitamClient"
    const val CLIENT_CREATOR = "Necro, JektDV and Liep_"
    const val CLIENT_WEBSITE = "hitam.client"
    const val MINECRAFT_VERSION = "1.8.9" 


    @JvmField
    val gitInfo = Properties().also {
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")
        if(inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.branch"] = "unofficial" // fill with default values or we'll get null pointer exceptions
        }
    }

    // 自动读取客户端版本 (bahasa felix tjonghoa)
    @JvmField
    val CLIENT_VERSION = "v1.0-BETA"
    @JvmField
    val CLIENT_BRANCH = (gitInfo["git.branch"] ?: "unknown").let {
        if(it == "main") "dev" else it
    }
    @JvmField
    val CLIENT_COMMIT_ID = gitInfo["git.commit.id.abbrev"]

    var isStarting = true
    var isLoadingConfig = true
    var latest = ""
        private set

    // Managers
    lateinit var moduleManager: ModuleManager
    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager
    lateinit var tipSoundManager: TipSoundManager
    lateinit var combatManager: CombatManager
    lateinit var macroManager: MacroManager
    lateinit var configManager: ConfigManager

    // Some UI things
    lateinit var hud: HUD
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager

    // Menu Background
    var background: ResourceLocation? = null

    val launchFilters = mutableListOf<EnumLaunchFilter>()
    private val dynamicLaunchOptions: Array<LaunchOption>
        get() = ClassUtils.resolvePackage("${LaunchOption::class.java.`package`.name}.options", LaunchOption::class.java)
            .filter {
                val annotation = it.getDeclaredAnnotation(LaunchFilterInfo::class.java)
                if (annotation != null) {
                    return@filter annotation.filters.toMutableList() == launchFilters
                }
                false
            }
            .map { try { it.newInstance() } catch (e: IllegalAccessException) { ClassUtils.getObjectInstance(it) as LaunchOption } }.toTypedArray()

    /**
     * Execute if client will be started
     */
    fun initClient() {
        ClientUtils.logInfo("Loading $CLIENT_NAME $CLIENT_VERSION, by $CLIENT_CREATOR")
        val startTime = System.currentTimeMillis()

        // Create file manager
        fileManager = FileManager()
        configManager = ConfigManager()

        // Create event manager
        eventManager = EventManager()

        // Load language
        LanguageManager.switchLanguage(Minecraft.getMinecraft().gameSettings.language)

        // Register listeners
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(AntiForge)
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(ServerSpoof)

        // Create command manager
        commandManager = CommandManager()

        fileManager.loadConfigs(fileManager.accountsConfig, fileManager.friendsConfig, fileManager.specialConfig)

        // Load client fonts
        Fonts.loadFonts()
        eventManager.registerListener(FontsGC)

        macroManager = MacroManager()
        eventManager.registerListener(macroManager)

        // Setup module manager and register modules
        moduleManager = ModuleManager()
        moduleManager.registerModules()

        try {
            // ScriptManager, Remapper will be lazy loaded when scripts are enabled
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }

        // Register commands
        commandManager.registerCommands()

        tipSoundManager = TipSoundManager()

        // KeyBindManager
        keyBindManager = KeyBindManager()

        // bstats.org user count display
        ClientUtils.buildMetrics()

        combatManager = CombatManager()
        eventManager.registerListener(combatManager)

        GuiCapeManager.load()

        mainMenu = GuiLaunchOptionSelectMenu()

        // Set HUD
        hud = HUD.createDefault()

        fileManager.loadConfigs(fileManager.hudConfig, fileManager.xrayConfig)

        // start discord rpc
        thread {
            try {
                DiscordRPC.run()
            } catch (e: Throwable) {
                ClientUtils.logError("Failed to load DiscordRPC.", e)
            }
        }

        // run update checker
        /*
        if(CLIENT_VERSION != "unknown") {
            thread(block = this::checkUpdate)
        }
        */

        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION loaded in ${(System.currentTimeMillis() - startTime)}ms!")
    }

    private fun checkUpdate() {
        try {
            val get = HttpUtils.get("https://api.github.com/repos/UnlegitMC/FDPClient/commits/${gitInfo["git.branch"]}")

            val jsonObj = JsonParser()
                .parse(get).asJsonObject

            latest = jsonObj.get("sha").asString.substring(0, 7)

            if (latest != gitInfo["git.commit.id.abbrev"]) {
                ClientUtils.logInfo("New version available: $latest")

                val buttons = arrayOf(LanguageManager.get("ui.update.download"), LanguageManager.get("ui.update.dismiss"))
                val selection = JOptionPane.showOptionDialog(null, LanguageManager.getAndFormat("ui.update.released", latest), "Alert",
                    JOptionPane.WARNING_MESSAGE, 0, null, buttons, buttons[0])
                if (selection == 0) {
                    MiscUtils.showURL("https://$CLIENT_WEBSITE")
                }
            } else {
                ClientUtils.logInfo("No new version available")
            }
        } catch (t: Throwable) {
            ClientUtils.logError("Failed to check for updates.", t)
        }
    }

    /**
     * Execute if client ui type is selected
     */
    fun startClient() {
        dynamicLaunchOptions.forEach {
            it.start()
        }

        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()

        // Set is starting status
        isStarting = false
        isLoadingConfig = false

        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started!")
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        if (!isStarting && !isLoadingConfig) {
            ClientUtils.logInfo("Shutting down $CLIENT_NAME $CLIENT_VERSION!")

            // Call client shutdown
            eventManager.callEvent(ClientShutdownEvent())

            // Save all available configs
            GuiCapeManager.save()
            configManager.save(true, true)
            fileManager.saveAllConfigs()

            dynamicLaunchOptions.forEach {
                it.stop()
            }
        }
        try {
            DiscordRPC.stop()
        } catch (e: Throwable) {
            ClientUtils.logError("Failed to shutdown DiscordRPC.", e)
        }
    }
}
