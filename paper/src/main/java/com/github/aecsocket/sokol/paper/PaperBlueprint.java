package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.impl.BasicBlueprint;

public final class PaperBlueprint extends BasicBlueprint<PaperBlueprintNode> {
    PaperBlueprint(String id, PaperBlueprintNode blueprint) {
        super(id, blueprint);
    }

    public static final class Serializer extends BasicBlueprint.Serializer<
        PaperBlueprint, PaperBlueprintNode
    > {
        @Override protected Class<PaperBlueprintNode> nodeType() { return PaperBlueprintNode.class; }

        @Override
        protected PaperBlueprint create(
                String id, PaperBlueprintNode blueprint
        ) {
            return new PaperBlueprint(id, blueprint);
        }
    }
}
