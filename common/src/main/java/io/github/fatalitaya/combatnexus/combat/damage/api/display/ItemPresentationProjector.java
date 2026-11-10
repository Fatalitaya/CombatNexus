package io.github.fatalitaya.combatnexus.combat.damage.api.display;
import io.github.fatalitaya.combatnexus.presentation.api.PresentationBudget;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot;

/** Streaming-by-entry construction; refuses before accumulating an unbounded presentation tree. */
public final class ItemPresentationProjector {
    private ItemPresentationProjector() {}
    public static JsonObject project(ItemCombatPresentationSnapshot snapshot) {
        var budget = new PresentationBudget(4096, 24000);
        var ops = new io.github.fatalitaya.combatnexus.presentation.api.BudgetJsonOps(budget);
        var documents = new JsonArray();
        for (var document : DamageDescriptions.project(snapshot.entries(), snapshot.affixes())) {
            budget.entry(1);
            documents.add(io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument.CODEC.encodeStart(ops, document).getOrThrow());
        }
        var result = new JsonObject(); result.add("documents", documents); return result;
    }
}
