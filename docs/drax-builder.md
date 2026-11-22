# Drax Builder

Mod para **Minecraft 26.2 + NeoForge**: varitas de construcción (extender y rellenar), varitas de intercambio, deshacer con Ctrl+Z e indicador de luz.

## Varitas de construcción
Clic derecho sobre la cara de un bloque: la varita **extiende esa cara**, poniendo una capa de bloques iguales delante de todos los bloques idénticos conectados. Usa los bloques de tu inventario (en creativo no gasta).

| Varita | Bloques por uso | Durabilidad | Receta |
|---|---|---|---|
| Hierro | 9 | 250 | palo + lingote de oro + bloque de hierro (en diagonal) |
| Diamante | 25 | 1561 | palo + lingote de oro + bloque de diamante (en diagonal) |
| Netherita | 64 | 4062, no se quema | varita de diamante + lingote de netherita |

**Vista previa:** con la varita en la mano, al apuntar a un bloque unas partículas celestes marcan dónde se colocarán los bloques (agáchate para ver el modo fila).

- **Agachado:** coloca solo **una fila** en la dirección hacia la que miras.
- Gasta 1 de durabilidad por uso (no por bloque).
- No funciona con bloques que tienen inventario (cofres, hornos…), ni coloca donde hay mobs o jugadores.
- Respeta la protección del spawn y los permisos de construcción.

### Modo Relleno
**Agachado + clic derecho al aire** cambia entre modo **Extender** y modo **Relleno**.
En modo Relleno: clic en un bloque marca la **primera esquina** (y elige ese bloque como material); clic en otro lugar marca la segunda y **rellena la caja** entre ambas (solo los espacios vacíos). La esquina marcada brilla en dorado y la vista previa muestra la caja. Agachado + clic en un bloque borra la esquina.
Tamaño máximo: 36 bloques (hierro), 100 (diamante), 256 (netherita).

## Varitas de intercambio
Cambian bloques ya puestos por otros, por ejemplo toda una pared de piedra a ladrillo.
- **Agachado + clic derecho** sobre un bloque: lo elige como bloque nuevo.
- **Clic derecho** sobre otro bloque: cambia ese bloque y todos los iguales conectados en el mismo plano. Gasta los bloques nuevos de tu inventario y **te devuelve los viejos**. Conserva la orientación (troncos, escaleras, losas).
- Vista previa en naranjo. Mismos límites que las de construcción (9 / 25 / 64).
- Receta: la varita de construcción del mismo material + una perla de ender.

## Deshacer (Ctrl+Z)
**Ctrl+Z** deshace la última acción de cualquier varita (hasta 10 hacia atrás): quita lo puesto y te lo devuelve, o vuelve a poner lo que se intercambió. Si alguien cambió un bloque después, ese no se toca. El historial se borra al reiniciar el servidor. La tecla se cambia en Controles.

## Indicador de luz
Pulsa **F7** (configurable en Controles) para marcar con partículas el suelo donde pueden aparecer mobs, en 12 bloques a la redonda:
- **Rojo:** sin luz de bloques ni de cielo → aparecen siempre (cuevas, interiores).
- **Amarillo:** sin luz de bloques pero al aire libre → aparecen de noche.
Con el indicador activo, debajo de la mira aparece el **nivel de luz** (de bloques y de cielo) del espacio al que apuntas.
Pon antorchas hasta que no quede nada marcado. Es solo del cliente: no hace falta que el servidor lo tenga.

## Compilar
Con JDK 25, en esta carpeta: `gradlew.bat build`. El `.jar` queda en `build\libs\drax_builder-1.2.0+mc26.2.jar`.
