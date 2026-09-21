package com.tuckersoft.branchengine.domain;

/** Cuanto golpea una decision a la partida. Los deltas salen de la tabla del enunciado. */
public enum ImpactLevel {

    LEVE(-5, 5),
    MODERADO(-15, 10),
    GRAVE(-30, 20),
    CRITICO(-40, 45);

    private final int lucidityDelta;
    private final int controlDelta;

    ImpactLevel(int lucidityDelta, int controlDelta) {
        this.lucidityDelta = lucidityDelta;
        this.controlDelta = controlDelta;
    }

    public int getLucidityDelta() {
        return lucidityDelta;
    }

    public int getControlDelta() {
        return controlDelta;
    }
}
