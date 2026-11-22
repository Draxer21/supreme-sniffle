# Drax Sort

Mod para **Minecraft 26.2 + NeoForge**: ordena cofres, barriles, cajas de shulker, dispensadores, cofres de ender y tu inventario con un clic. Funciona en un solo jugador y en servidores (el orden lo hace el servidor; el mod debe estar instalado en ambos lados).

## Cómo se usa
Al costado derecho de cualquier pantalla con inventario aparecen botones pequeños (en tu inventario, tecla E, van dentro del panel a la derecha del libro de recetas):

| Botón | Qué hace |
|---|---|
| **⇅** (arriba) | Ordena el contenedor abierto (cofre, barril…) |
| **⇅** (abajo) | Ordena tu inventario. La **barra rápida no se toca**, ni la armadura ni la mano secundaria |
| **◆ / A / #** | Criterio: **categoría** (bloques, armas, herramientas, armadura, comida, resto), **nombre** (A-Z) o **cantidad** (lo que más hay primero) |
| **≡ / ‖** | **Filas** (izquierda → derecha) o **columnas** (arriba → abajo) |
| **⇧** | Rellena la barra rápida desde cofres cercanos (tecla **R**) |
| **▣** | Plantilla del inventario: guarda dónde va cada ítem (Shift + clic la borra) |

### Depositar en cofres cercanos
Pulsa **Z** (configurable en Controles) o el botón **⇩**: cada ítem de tu inventario se guarda en los cofres, barriles y cajas de shulker **a 8 bloques** que **ya tengan ese mismo ítem**. Funciona con o sin un inventario abierto. No toca la barra rápida, la armadura ni los slots bloqueados, ni los cofres de estructuras sin abrir o con llave. Los contenedores que recibieron algo sueltan unas chispas verdes.

### Rellenar desde cofres cercanos
Pulsa **R** (configurable) o el botón **⇧**: cada pila de tu **barra rápida y mano secundaria** se completa sacando ítems iguales de los cofres, barriles y cajas de shulker a 8 bloques. Ideal para recargar bloques y antorchas mientras construyes.

### Reabastecer la mano
Cuando gastas el **último bloque** de la mano, o se te **rompe la herramienta**, se trae sola otra pila igual de tu inventario al mismo slot.

### Rueda del mouse y arrastrar
- **Rueda hacia abajo** sobre una pila: la manda al otro lado (como Shift + clic).
- **Rueda hacia arriba** sobre una pila: trae una pila igual desde el otro lado.
- **Shift + arrastrar** con clic izquierdo: mueve todas las pilas por las que pasas.
Se puede desactivar la rueda con `scrollMove` en la config.

### Plantilla del inventario
Ordena tu inventario como te guste y pulsa **▣**: queda guardado en qué slot va cada ítem. Desde entonces, al ordenar el inventario cada ítem **vuelve a su slot** y el resto se ordena en los que quedan libres. Shift + clic en ▣ la borra.

### Buscador
Encima de cualquier inventario hay una barra de búsqueda: escribe y los slots que coinciden (por nombre o id) quedan con **marco dorado**; el resto se oscurece. El texto se mantiene al abrir otro cofre. Clic fuera de la barra para que E vuelva a cerrar el inventario.

**Enter** en la barra busca también en los **cofres, barriles y cajas de shulker a 16 bloques**: los que tienen el ítem brillan con partículas doradas durante 10 segundos, y arriba de la barra rápida sale cuántos encontró.

### Slots bloqueados
**Alt + clic** sobre un slot de tu inventario (incluida la barra rápida) lo bloquea: queda con un **candado** y tinte rojo. Un slot bloqueado no se ordena, no se deposita, no se mueve con Shift + clic y no se tira con Q. Alt + clic otra vez para desbloquearlo. Se guardan en `config/drax_sort-client.toml`.

**Atajo:** clic central (rueda del mouse) sobre un slot ordena esa sección. En creativo no, porque ahí el clic central copia ítems; usa los botones.

Al ordenar también **junta las pilas incompletas** del mismo ítem (dos pilas de 20 piedras quedan en una de 40).

El criterio y filas/columnas se recuerdan entre partidas (`config/drax_sort-client.toml`, donde también se puede desactivar el clic central).

## Qué no ordena
- La barra rápida, la armadura y la mano secundaria.
- La mesa de crafteo, hornos, yunques y contenedores de menos de 9 slots (para no desordenar recetas o máquinas).
- El inventario del modo creativo.

## Compilar
Con JDK 25, en esta carpeta: `gradlew.bat build`. El `.jar` queda en `build\libs\drax_sort-1.3.0+mc26.2.jar`.
