package com.ntmspace.probe;

/** Additional development probe content, declared with the probe's own tests and client hooks. */
public interface ProbeExtension {
    default void tests() {}

    default void client() {}
}
