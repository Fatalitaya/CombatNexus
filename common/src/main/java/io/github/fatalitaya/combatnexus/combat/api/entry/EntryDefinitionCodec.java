package io.github.fatalitaya.combatnexus.combat.api.entry;

import com.mojang.serialization.*;
import com.mojang.datafixers.util.Pair;
import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import io.github.fatalitaya.combatnexus.combat.damage.core.definition.DamageDefinitionFields;

/** Shared bounded decoding for resource, component, network and saved instance definitions. */
public final class EntryDefinitionCodec {
    private EntryDefinitionCodec() {}
    public static <A> Codec<A> checked(Codec<A> delegate, String directory) {
        Codec<A> strict = new Codec<>() {
            public <T> DataResult<Pair<A,T>> decode(DynamicOps<T> ops,T input) {
                try {
                    var json=ops.convertTo(JsonOps.INSTANCE,input);
                    DamageDefinitionFields.validate(directory,json.getAsJsonObject());
                    return delegate.decode(ops,input);
                } catch (IllegalArgumentException | IllegalStateException | NullPointerException failure) {
                    return DataResult.error(()->"Invalid equipment definition: "+failure.getMessage());
                }
            }
            public <T> DataResult<T> encode(A input,DynamicOps<T> ops,T prefix) { return delegate.encode(input,ops,prefix); }
        };
        return CodecBudgets.guardRawStructure(CodecBudgets.guardText(strict,32768,131072),directory,48,32768);
    }
}
