package io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.formula.CombatFormulaSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Server-to-client display parameters; never accepted from clients. */
public record FormulaSettingsPayload(long revision, float armorK, float resistanceK, float ratingPerProtectionScore,
                                     boolean managedArmor, boolean managedEnchantments)
        implements CustomPacketPayload {
    public FormulaSettingsPayload {
        if (revision < 1) throw new IllegalArgumentException("Invalid settings revision");
        new CombatFormulaSettings(armorK, resistanceK, ratingPerProtectionScore);
    }
    public CombatFormulaSettings formulas() { return new CombatFormulaSettings(armorK, resistanceK, ratingPerProtectionScore); }
    public static final Type<FormulaSettingsPayload> TYPE = new Type<>(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(NexusId.fromNamespaceAndPath("combatnexus", "formula_settings")));
    public static final StreamCodec<ByteBuf, FormulaSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, FormulaSettingsPayload::revision,
            ByteBufCodecs.FLOAT, FormulaSettingsPayload::armorK,
            ByteBufCodecs.FLOAT, FormulaSettingsPayload::resistanceK,
            ByteBufCodecs.FLOAT, FormulaSettingsPayload::ratingPerProtectionScore,
            ByteBufCodecs.BOOL, FormulaSettingsPayload::managedArmor,
            ByteBufCodecs.BOOL, FormulaSettingsPayload::managedEnchantments, FormulaSettingsPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
