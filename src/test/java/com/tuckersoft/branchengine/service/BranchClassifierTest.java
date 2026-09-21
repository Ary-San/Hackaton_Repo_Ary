package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.BranchType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Las cinco reglas del motor, aisladas del resto del sistema. */
class BranchClassifierTest {

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Stefan acepta la oferta de Mohan y se queda en Tuckersoft. | OBEDIENCIA",
            "Stefan rechaza la oferta y se va dando un portazo.         | REBELDIA",
            "Stefan cree que lo vigilan desde el televisor del salon.   | SOSPECHA",
            "Stefan mira a la camara y habla con quien decide por el.   | RUPTURA_CUARTA_PARED",
            "%%%% 01001 ### @@@ 110                                     | ENTRADA_CORRUPTA",
    })
    @DisplayName("Las cinco reglas dan la rama que toca")
    void las_cinco_reglas(String texto, BranchType esperada) {
        assertEquals(esperada, BranchClassifier.clasificar(texto));
    }

    @Test
    @DisplayName("La regla 2 se evalua antes que la 4")
    void la_precedencia_manda() {
        assertEquals(BranchType.RUPTURA_CUARTA_PARED,
                BranchClassifier.clasificar("Stefan destruye la camara que lo estaba grabando."));
    }

    @Test
    @DisplayName("El texto se normaliza antes de comparar: mayusculas y tildes dan igual")
    void las_tildes_no_despistan() {
        assertEquals(BranchType.RUPTURA_CUARTA_PARED,
                BranchClassifier.clasificar("Stefan mira fijamente la CÁMARA del salón y sonríe."));
        assertEquals("camara", BranchClassifier.normalizar("CÁMARA"));
    }

    @Test
    @DisplayName("Los digitos y los simbolos no cuentan como letras")
    void solo_cuentan_las_letras_de_la_a_a_la_z() {
        assertEquals(BranchType.ENTRADA_CORRUPTA, BranchClassifier.clasificar("1234567890 !!! ---"));
        assertEquals(BranchType.OBEDIENCIA, BranchClassifier.clasificar("1234567890 a !!!"));
    }
}
