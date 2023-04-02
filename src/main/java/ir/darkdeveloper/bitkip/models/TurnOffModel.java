package ir.darkdeveloper.bitkip.models;

public enum TurnOffModel {
    SLEEP,
    TURN_OFF,
    HIBERNATE;

    @Override
    public String toString() {
        return this.name();
    }
}
