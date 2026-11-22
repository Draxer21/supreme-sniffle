package com.draxer.draxsort;

/** Criterio de orden. El índice viaja en el paquete de red, así que no cambiar el orden de las constantes. */
public enum SortMode {
    /** Bloques, armas, herramientas, armadura, comida, el resto; dentro de cada grupo por id. */
    CATEGORY,
    /** Alfabético por nombre visible. */
    NAME,
    /** Primero lo que más hay. */
    COUNT;

    public SortMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public static SortMode byIndex(int index) {
        SortMode[] values = values();
        return index >= 0 && index < values.length ? values[index] : CATEGORY;
    }
}
