package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.StatDescriptor.desc;

public final class VectorStat {
    private VectorStat() {}

    public static SVector2 vector2Stat(@Nullable Vector2 def) { return new SVector2(def); }
    public static SVector2 vector2Stat() { return new SVector2(null); }

    public static SVector3 vector3Stat(@Nullable Vector3 def) { return new SVector3(def); }
    public static SVector3 vector3Stat() { return new SVector3(null); }

    public static final class SVector2 extends BasicStat<StatDescriptor<Vector2>> {
        public static final Map<String, Combiner<Vector2>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Vector2>>())
                .put("=", (a, b) -> b)
                .put("+", Vector2::add)
                .put("-", Vector2::subtract)
                .put("*", Vector2::multiply)
                .put("/", Vector2::divide)
                .build();
        public static final String DEFAULT_OPERATOR = "=";
        private SVector2(@Nullable Vector2 def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SVector3 extends BasicStat<StatDescriptor<Vector3>> {
        public static final Map<String, Combiner<Vector3>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Vector3>>())
                .put("=", (a, b) -> b)
                .put("+", Vector3::add)
                .put("-", Vector3::subtract)
                .put("*", Vector3::multiply)
                .put("/", Vector3::divide)
                .build();
        public static final String DEFAULT_OPERATOR = "=";
        private SVector3(@Nullable Vector3 def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }
}
