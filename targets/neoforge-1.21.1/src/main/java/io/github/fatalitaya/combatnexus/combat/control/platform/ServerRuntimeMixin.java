package io.github.fatalitaya.combatnexus.combat.control.platform;

import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.fatalitaya.combatnexus.combat.platform.NativeServerRuntimeBinding;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.Objects;
import java.util.Optional;

@Mixin(MinecraftServer.class)
public abstract class ServerRuntimeMixin implements NativeServerRuntimeBinding {
    @Unique private CombatServerRuntime combatnexus$runtime;
    public Optional<CombatServerRuntime> combatnexus$serverRuntime() { return Optional.ofNullable(combatnexus$runtime); }
    public void combatnexus$bindServerRuntime(CombatServerRuntime runtime) {
        if (combatnexus$runtime != null) throw new IllegalStateException("Server session already bound");
        combatnexus$runtime = Objects.requireNonNull(runtime);
    }
}
