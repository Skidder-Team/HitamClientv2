package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import com.mojang.authlib.Agent.MINECRAFT
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication
import com.thealtening.AltService
import com.thealtening.api.TheAltening
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.net.Proxy.NO_PROXY

class GuiTheAltening(private val prevGui: GuiAltManager): GuiScreen() {

    companion object {
        var apiKey: String = ""
    }

    // Button
    private lateinit var loginButton: GuiButton
    private lateinit var generateButton: GuiButton

    // User Input Fields
    private lateinit var apiKeyField: GuiTextField
    private lateinit var tokenField: GuiTextField

    private var status = ""

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        // Login button
        loginButton = GuiButton(2, width / 2 - 100, 75, "Login")
        buttonList.add(loginButton)

        // Generate button
        generateButton = GuiButton(1, width / 2 - 100, 140, "Generate")
        buttonList.add(generateButton)


        // Back buttons
        buttonList.add(GuiButton(0, width / 2 - 100, height - 58, "Back"))

        // Token text field
        tokenField = GuiTextField(666, Fonts.font40, width / 2 - 100, 50, 200, 20)
        tokenField.isFocused = true
        tokenField.maxStringLength = Integer.MAX_VALUE

        // Api key password field
        apiKeyField = GuiPasswordField(1337, Fonts.font40, width / 2 - 100, 115, 200, 20)
        apiKeyField.maxStringLength = 18
        apiKeyField.text = apiKey

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        RenderUtils.drawRoundedCornerRect(30.0f, 30.0f, width - 30.0f, height - 30.0f, 4f, Color(0,0,0, 70).rgb)

        // Draw title and status
        Fonts.font35.drawCenteredString("TheAltening", width / 2.0f, 10.0f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2.0f, 18.0f, 0xffffff)

        // Draw fields
        apiKeyField.drawTextBox()
        tokenField.drawTextBox()

        // Draw text
        Fonts.font40.drawCenteredString("§7Token:", width / 2.0f - 84, 40.0f, 0xffffff)
        Fonts.font40.drawCenteredString("§7API-Key:", width / 2.0f - 78, 105.0f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                loginButton.enabled = false
                generateButton.enabled = false
                apiKey = apiKeyField.text

                val altening = TheAltening(apiKey)
                val asynchronous = TheAltening.Asynchronous(altening)
                status = "§cGenerating account..."

                asynchronous.accountData.thenAccept { account ->
                    status = "§aGenerated account: §b§l${account.username}"

                    try {
                        status = "§cSwitching Alt Service..."

                        GuiAltManager.altService.switchService(AltService.EnumAltService.THEALTENING)

                        status = "§cLogging in..."

                        // Set token as username
                        val yggdrasilUserAuthentication = YggdrasilUserAuthentication(YggdrasilAuthenticationService(NO_PROXY, ""), MINECRAFT)
                        yggdrasilUserAuthentication.setUsername(account.token)
                        yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME)

                        status = try {
                            yggdrasilUserAuthentication.logIn()

                            mc.session = Session(yggdrasilUserAuthentication.selectedProfile.name, yggdrasilUserAuthentication
                                .selectedProfile.id.toString(),
                                yggdrasilUserAuthentication.authenticatedToken, "mojang")

                            LiquidBounce.eventManager.callEvent(SessionEvent())

                            prevGui.status = "§aYour name is now §b§l${yggdrasilUserAuthentication.selectedProfile.name}§c."
                            mc.displayGuiScreen(prevGui)
                            ""
                        } catch (e: AuthenticationException) {
                            GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)

                            ClientUtils.logError("Failed to login.", e)
                            "§cFailed to login: ${e.message}"
                        }
                    } catch (throwable: Throwable) {
                        status = "§cFailed to login. Unknown error."
                        ClientUtils.logError("Failed to login.", throwable)
                    }

                    loginButton.enabled = true
                    generateButton.enabled = true
                }.handle {_, err ->
                    status = "§cFailed to generate account."
                    ClientUtils.logError("Failed to generate account.", err)
                }.whenComplete{_, _ ->
                    loginButton.enabled = true
                    generateButton.enabled = true
                }
            }
            2 -> {
                loginButton.enabled = false
                generateButton.enabled = false

                Thread(Runnable {
                    try {
                        status = "§cSwitching Alt Service..."

                        GuiAltManager.altService.switchService(AltService.EnumAltService.THEALTENING)

                        status = "§cLogging in..."

                        // Set token as username
                        val yggdrasilUserAuthentication =
                            YggdrasilUserAuthentication(YggdrasilAuthenticationService(NO_PROXY, ""), MINECRAFT)
                        yggdrasilUserAuthentication.setUsername(tokenField.text)
                        yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME)

                        status = try {
                            yggdrasilUserAuthentication.logIn()

                            mc.session = Session(
                                yggdrasilUserAuthentication.selectedProfile.name, yggdrasilUserAuthentication
                                    .selectedProfile.id.toString(),
                                yggdrasilUserAuthentication.authenticatedToken, "mojang"
                            )

                            LiquidBounce.eventManager.callEvent(SessionEvent())

                            prevGui.status =
                                "§aYour name is now §b§l${yggdrasilUserAuthentication.selectedProfile.name}§c."
                            mc.displayGuiScreen(prevGui)
                            "§aYour name is now §b§l${yggdrasilUserAuthentication.selectedProfile.name}§c."
                        } catch (e: AuthenticationException) {
                            GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)

                            ClientUtils.logError("Failed to login.", e)
                            "§cFailed to login: ${e.message}"
                        }
                    } catch (throwable: Throwable) {
                        ClientUtils.logError("Failed to login.", throwable)
                        status = "§cFailed to login. Unknown error."
                    }

                    loginButton.enabled = true
                    generateButton.enabled = true
                }).start()
            }
        }
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Check if user want to escape from screen
        if (Keyboard.KEY_ESCAPE == keyCode) {
            // Send back to prev screen
            mc.displayGuiScreen(prevGui)
            return
        }

        // Check if field is focused, then call key typed
        if (apiKeyField.isFocused) apiKeyField.textboxKeyTyped(typedChar, keyCode)
        if (tokenField.isFocused) tokenField.textboxKeyTyped(typedChar, keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // Call mouse clicked to field
        apiKeyField.mouseClicked(mouseX, mouseY, mouseButton)
        tokenField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen() {
        apiKeyField.updateCursorCounter()
        tokenField.updateCursorCounter()
        super.updateScreen()
    }

    /**
     * Handle gui closed
     */
    override fun onGuiClosed() {
        // Disable keyboard repeat events
        Keyboard.enableRepeatEvents(false)

        // Set API key
        apiKey = apiKeyField.text
        super.onGuiClosed()
    }
}