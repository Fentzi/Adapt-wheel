package com.adapts.mixin;

import com.adapts.AdaptsRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Entity status 35 = "used Totem of Undying"
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void adapts$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Entity entity = packet.getEntity(mc.world);
        if (entity != null) AdaptsRenderer.trigger(entity, mc.world.getTime());
    }
}
