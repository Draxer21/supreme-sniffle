package com.draxer.draxwaypoints;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Drax Waypoints: puntos de ruta, brújula en pantalla y coordenadas. Solo del cliente:
 * funciona en cualquier servidor aunque el servidor no lo tenga.
 */
@Mod(value = DraxWaypoints.MODID, dist = Dist.CLIENT)
public class DraxWaypoints {
    public static final String MODID = "drax_waypoints";

    public DraxWaypoints() {
    }
}
