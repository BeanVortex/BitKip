package io.beanvortex.bitkip.models;

public enum TurnOffMode {
    NOTHING,
    SLEEP,
    TURN_OFF;

    @Override
    public String toString() {
        return this.name();
    }
}
