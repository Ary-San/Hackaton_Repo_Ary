package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.BranchType;

import java.text.Normalizer;

/**
 * El motor de clasificacion. Reglas propias, sin IA ni servicios externos: la misma
 * entrada da siempre la misma rama.
 *
 * El orden de las reglas es parte del contrato. La primera que se cumple gana y las
 * demas no se evaluan, asi que "Stefan destruye la camara" es RUPTURA_CUARTA_PARED
 * (regla 2) y no REBELDIA (regla 4).
 */
public final class BranchClassifier {

    private static final String[] RUPTURA = {"netflix", "camara", "espectador", "videojuego"};
    private static final String[] SOSPECHA = {"vigilan", "simbolo", "conspiracion"};
    private static final String[] REBELDIA = {"rechaza", "destruye", "desobedece", "renuncia"};

    /** Minusculas y sin tildes, para que "CÁMARA", "cámara" y "camara" comparen igual. */
    public static String normalizar(String rawInput) {
        if (rawInput == null) return "";
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    public static BranchType clasificar(String rawInput) {
        String texto = normalizar(rawInput);

        // Regla 1: ni una sola letra de la 'a' a la 'z'.
        if (!tieneAlgunaLetra(texto)) return BranchType.ENTRADA_CORRUPTA;

        // Regla 2
        if (contieneAlguna(texto, RUPTURA)) return BranchType.RUPTURA_CUARTA_PARED;

        // Regla 3
        if (contieneAlguna(texto, SOSPECHA)) return BranchType.SOSPECHA;

        // Regla 4
        if (contieneAlguna(texto, REBELDIA)) return BranchType.REBELDIA;

        // Regla 5: todo lo demas.
        return BranchType.OBEDIENCIA;
    }

    private static boolean tieneAlgunaLetra(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= 'a' && c <= 'z') return true;
        }
        return false;
    }

    private static boolean contieneAlguna(String texto, String[] claves) {
        for (String clave : claves) {
            if (texto.contains(clave)) return true;
        }
        return false;
    }

    private BranchClassifier() {
    }
}
