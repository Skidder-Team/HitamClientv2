package net.ccbluex.liquidbounce.features.module.modules.movement.flys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.cos
import kotlin.math.sin

class MatrixDamage : FlyMode("MatrixDamage") {
    private val mode = ListValue("${valuePrefix}Mode", arrayOf("Stable", "Stable2", "Custom"), "Stable")
    private val warn = BoolValue("${valuePrefix}DamageWarn", true)
    private val speedBoost = FloatValue("${valuePrefix}BoostSpeed", 0.5f, 0f, 3f)
    private val timer = FloatValue("${valuePrefix}Timer", 1.0f, 0f, 2f)
    private val boostTicks = IntegerValue("${valuePrefix}BoostTicks", 27, 10, 40)
    private val randomize = BoolValue("${valuePrefix}Randomize", true)
    private val randomAmount = IntegerValue("${valuePrefix}RandomAmount", 1, 0, 30).displayable { randomize.get() }

    private var velocitypacket = false
    private var packetymotion = 0.0
    private var tick = 0
    private var randomNum = 0.2

    override fun onEnable() {
        if (warn.get()) 
            ClientUtils.displayChatMessage("§8[§c§lMatrix-Dmg-Fly§8] §aGetting damage from other entities (players, arrows, snowballs, eggs...) is required to bypass.")
        velocitypacket = false
        packetymotion = 0.0
        tick = 0
    }

    override fun onUpdate(event: UpdateEvent) {
        if (velocitypacket) {
            val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
            when (mode.get().lowercase()) {
                "stable", "stable2" -> {
                    mc.timer.timerSpeed = 1.0F
                    mc.thePlayer.motionX += (-sin(yaw) * 0.416)
                    mc.thePlayer.motionZ += (cos(yaw) * 0.416)
                    if (mode.equals("stable")) 
                        mc.thePlayer.motionY = packetymotion
                    if (tick++ >= if (mode.equals("stable")) 27 else 30) {
                        mc.timer.timerSpeed = 1.0f
                        velocitypacket = false
                        packetymotion = 0.0
                        tick = 0
                    }
                }
                "custom" -> {
                    randomNum = if (randomize.get()) Math.random() * randomAmount.get() * 0.01 else 0.0
                    mc.timer.timerSpeed = timer.get()
                    mc.thePlayer.motionX += (-sin(yaw) * (0.3 + (speedBoost.get().toDouble() / 10.0) + randomNum))
                    mc.thePlayer.motionZ += (cos(yaw) * (0.3 + (speedBoost.get().toDouble() / 10.0) + randomNum))
                    mc.thePlayer.motionY = packetymotion
                    if (tick++ >= boostTicks.get()) {
                        mc.timer.timerSpeed = 1.0f
                        velocitypacket = false
                        packetymotion = 0.0
                        tick = 0
                    }
                }
            }

        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity) {
            if (mc.thePlayer == null || (mc.theWorld?.getEntityByID(packet.entityID) ?: return) != mc.thePlayer) return
            if (packet.motionY / 8000.0 > 0.2) {
                velocitypacket = true
                packetymotion = packet.motionY / 8000.0
            }
        }
    }
}
