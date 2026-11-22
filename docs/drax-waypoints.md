# Drax Waypoints

Mod para **Minecraft 26.2 + NeoForge**, **solo del cliente**: funciona en cualquier servidor aunque el servidor no lo tenga.

## Brújula en pantalla
Arriba de la pantalla hay una **brújula** con N, NE, E… (el norte en rojo) y tus **puntos de ruta** como cuadraditos de color. Cuando miras hacia uno, aparece su **nombre y distancia**. Debajo: tus **coordenadas**, hacia dónde miras y el **bioma**.
**H** la muestra u oculta.

## Puntos de ruta
- **B**: guarda un punto donde estás ("Punto 1", "Punto 2"…).
- **N**: abre la lista. Ahí puedes **renombrar** (escribe sobre el nombre), cambiar el **color** (●), **ocultar** (◉/○), **borrar** (✖) o crear uno nuevo donde estás.
- Sobre cada punto a menos de 160 bloques sale una **columna de luz** de su color, visible de lejos.
- Al **morir** se guarda solo un punto **"Muerte"** (siempre el de la última muerte).
- Cada mundo y cada servidor tiene su propia lista, guardada en `config/drax_waypoints/`.
- Los puntos de otra dimensión no salen en la brújula (en la lista dice en qué dimensión están).

Las teclas se cambian en Controles.

## Compilar
Con JDK 25, en esta carpeta: `gradlew.bat build`. El `.jar` queda en `build\libs\drax_waypoints-1.0.0+mc26.2.jar`.
