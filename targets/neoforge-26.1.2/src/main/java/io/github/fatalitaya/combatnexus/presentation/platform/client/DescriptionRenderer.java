package io.github.fatalitaya.combatnexus.presentation.platform.client;

import io.github.fatalitaya.combatnexus.presentation.api.DescriptionDocument;
import io.github.fatalitaya.combatnexus.presentation.api.DescriptionLayout;
import io.github.fatalitaya.combatnexus.presentation.platform.client.DisplayTextResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Applies native text and colors to the shared description layout. */
public final class DescriptionRenderer {
    private DescriptionRenderer() {}
    public static List<Component> render(DescriptionDocument document, boolean expanded) {
        var result = new ArrayList<Component>();
        for (var line : DescriptionLayout.render(document, expanded)) {
            var color = switch (line.tone()) {
                case TITLE -> ChatFormatting.AQUA;
                case RULE -> ChatFormatting.DARK_AQUA;
                case TEXT -> ChatFormatting.GRAY;
                case DETAIL -> ChatFormatting.DARK_GRAY;
            };
            result.add(Component.literal("  ".repeat(line.depth())).append(DisplayTextResolver.resolve(line.text())).withStyle(color));
        }
        return result;
    }
}
