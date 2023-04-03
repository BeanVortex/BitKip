package ir.darkdeveloper.bitkip.models;

public enum TurnOffMode {
    SLEEP,
    TURN_OFF,
    HIBERNATE;

    @Override
    public String toString() {
        return this.name();
    }
}
