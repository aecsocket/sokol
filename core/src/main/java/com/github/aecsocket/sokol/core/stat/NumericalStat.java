package com.github.aecsocket.sokol.core.stat;

public interface NumericalStat {
    interface Value {
        double base();
    }

    interface SetValue extends Value {
        @Override default double base() { return 0; }
    }

    interface SumValue extends Value {
        @Override default double base() { return 0; }
    }
    interface AddValue extends SumValue {}
    interface SubtractValue extends SumValue {}

    interface FactorValue extends Value {
        @Override default double base() { return 1; }
    }
    interface MultiplyValue extends FactorValue {}
    interface DivideValue extends FactorValue {}
}
