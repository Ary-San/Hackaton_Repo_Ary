package com.tuckersoft.branchengine.domain;

/**
 * Las cinco ramas del guion.
 *
 * Cada rama lleva colgados su departamento y su consecuencia: ni handlerUnit ni
 * outcomeCode llegan nunca en el request, se derivan de aqui.
 */
public enum BranchType {

    OBEDIENCIA("Mesa de Guion", "ADVANCE_MAIN_PATH"),
    REBELDIA("Control de Continuidad", "FORK_TIMELINE"),
    SOSPECHA("Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL"),
    RUPTURA_CUARTA_PARED("Departamento Netflix", "BREAK_FOURTH_WALL"),
    ENTRADA_CORRUPTA("Archivo de Errores", "DISCARD_INPUT");

    private final String handlerUnit;
    private final String outcomeCode;

    BranchType(String handlerUnit, String outcomeCode) {
        this.handlerUnit = handlerUnit;
        this.outcomeCode = outcomeCode;
    }

    public String getHandlerUnit() {
        return handlerUnit;
    }

    public String getOutcomeCode() {
        return outcomeCode;
    }
}
