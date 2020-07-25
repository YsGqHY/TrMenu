package me.arasple.mc.trmenu.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import io.izzel.taboolib.Version
import io.izzel.taboolib.loader.internal.IO
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.lite.Materials
import me.arasple.mc.trmenu.modules.packets.PacketsHandler
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*
import java.util.function.Consumer

/**
 * @author Arasple
 * @date 2020/7/6 20:44
 */
object Skulls {

    val cache = mutableMapOf<String, ItemStack>()
    val cachePlayerTexture = mutableMapOf<String, String?>()

    fun getPlayerHead(name: String): ItemStack = cache.computeIfAbsent(name) {
        val texture = getPlayerTexture(name) ?: kotlin.run {
            val head = ItemBuilder(Materials.PLAYER_HEAD.parseItem()).build()
            getPlayerTexture(name) {
                setTextureSkull(it, head)
            }
            return@computeIfAbsent head
        }
        return@computeIfAbsent getTextureSkull(texture)
    }

    private fun getPlayerTexture(id: String) = getPlayerTexture(id, null)

    private fun getPlayerTexture(id: String, consumer: Consumer<String>?): String? {
        if (cachePlayerTexture.containsKey(id)) {
            return cachePlayerTexture[id]
        } else {
            val player = Bukkit.getPlayerExact(id)
            if (player != null && Version.isAfter(Version.v1_13)) {
                cachePlayerTexture[id] = PacketsHandler.getPlayerTexture(player)
                return cachePlayerTexture[id]
            } else {
                cachePlayerTexture[id] = null
            }
            Tasks.run(true) {
                try {
                    val userProfile = JsonParser().parse(IO.readFromURL("https://api.mojang.com/users/profiles/minecraft/$id")) as JsonObject
                    val uuid = userProfile["id"].asString
                    val textures = (JsonParser().parse(IO.readFromURL("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")) as JsonObject).getAsJsonArray("properties")
                    for (element in textures) if ("textures" == element.asJsonObject["name"].asString) cachePlayerTexture[id] = element.asJsonObject["value"].asString
                    if (consumer != null) cachePlayerTexture[id]?.let { consumer.accept(it) }
                } catch (e: Throwable) {
                    Msger.printErrors("PLAYER-HEAD", e)
                }
            }
        }
        return cachePlayerTexture[id]
    }

    fun getTextureSkull(texture: String) = setTextureSkull(texture, Materials.PLAYER_HEAD.parseItem()!!)

    fun setTextureSkull(texture: String, item: ItemStack): ItemStack = cache.computeIfAbsent(texture) {
        val meta = item.itemMeta as SkullMeta
        val profile = GameProfile(UUID.randomUUID(), null)
        val field = meta.javaClass.getDeclaredField("profile")
        profile.properties.put("textures", Property("textures", texture, "TrMenu_TexturedSkull"))
        field.isAccessible = true
        field[meta] = profile
        item.itemMeta = meta
        return@computeIfAbsent item
    }

}