package com.github.aecsocket.sokol.core;

class Test {
    interface Instance<D extends Data<?>> {}

    interface Data<I extends Instance<?>> {
        I asInstance();
    }

    class Node<
        F extends Instance<? extends Data<?>>
    > {
        <D extends Data<? extends F>> void initFeatures(D data) {
            F inst = data.asInstance();
        }
    }
}
