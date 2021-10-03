package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.Feature;

public interface PaperFeature<F extends PaperFeature<F>> extends Feature.Scoped<F, PaperNode> {}
