package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.util.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.Node;

public interface Rule {
    void compatibility(Node node) throws IncompatibilityException;
    void visit(Visitor visitor);

    interface Visitor {
        void accept(Rule rule);
    }
}
