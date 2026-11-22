# Mesa de Ascensión (Ascension Table)

> Mod compañero opcional: **Ascension Enchantments** y **Giga Enchantments** — sus encantamientos también se pueden subir en esta mesa.

Mod para **Minecraft 26.2 + NeoForge** que añade la **Mesa de Ascensión**, un bloque que sube los encantamientos de armas, armaduras, herramientas y libros encantados **hasta el nivel XV**, gastando materiales con valor en puntos.

## Cómo se usa en el juego
1. Fabrica la mesa:
   ```
   Oro      Diamante  Oro
   Amatista Mesa enc. Amatista
   Obsidiana Obsidiana Obsidiana
   ```
   (también está en la pestaña creativa *Bloques funcionales*). Se pica con pico de hierro o mejor.
2. Pon el objeto encantado en el primer slot (⚔) y los materiales en los 3 slots de **Materiales**. El segundo slot (▤) es para un **libro**.
3. A la derecha aparece cada encantamiento con su **siguiente nivel y su costo**. Si tienes los puntos, el botón se activa. Al pulsarlo se gastan los materiales y sube un nivel. Al pasar el mouse por el botón, los materiales que se van a gastar se marcan en **rojo** con la cantidad.
4. Al cerrar la mesa, los ítems vuelven a tu inventario.

No se pueden subir las maldiciones ni los encantamientos de un solo nivel (Reparación, Toque de seda, Infinidad, etc.).

### Bajar, extraer y combinar
- **− (bajar un nivel):** baja el encantamiento un nivel (en I lo quita) y **guarda en la mesa la mitad** de lo que costaba ese nivel. Esos puntos guardados se suman a tus puntos y se gastan antes que los materiales. Quedan en esa mesa aunque la cierres.
- **⇪ (extraer a libro):** con un **libro normal** en el slot ▤, saca el encantamiento del ítem y lo pasa al libro con su nivel. Cuesta un 25 % del costo de ese nivel. Sirve para reciclar armas viejas.
- **✚ (aplicar libro):** con un **libro encantado** en el slot ▤, aplica sus encantamientos al ítem como el yunque (dos iguales suben uno, hasta el máximo normal), pero **nunca "demasiado caro"**. Respeta las incompatibilidades. Cuesta un 25 % del costo de cada nivel resultante.

### Estanterías
Como en la mesa vanilla: cada **estantería** alrededor (mismo patrón, un bloque de aire entre la mesa y la estantería) abarata todo un **1 %**, hasta **15 %**. Se ve arriba a la derecha (▤15). Se combina con el borde de oro.

### Conversión a Giga (con el mod Giga Enchantments)
Junto a cada encantamiento vanilla que tenga versión Giga aparece un botón **★**. Si el encantamiento está en su **nivel máximo normal** (Filo V, Protección IV, Eficiencia V…) y hay una **Estrella del Nether** en los materiales, al pulsarlo se convierte en su versión Giga **con el mismo nivel** y se gasta la estrella. Pasa el mouse sobre ★ para ver qué falta.

> La Estrella del Nether también vale 1200 puntos como material: si la dejas en la mesa al subir niveles normales, puede gastarse ahí.

### Aspecto de la mesa
- Sobre la mesa flota un libro, como en la mesa de encantamientos: cuando te acercas (a menos de 3 bloques) se abre, te mira y pasa páginas; cuando te alejas se cierra y gira despacio. Los glifos de encantamiento vuelan hacia él.
- **Bordes de mineral:** agáchate (Shift) y haz clic derecho sobre la mesa con **diamante, lingote de hierro, lingote de oro, lingote de cobre o fragmento de amatista** para cambiar las esquinas. Gasta 1 unidad (nada en creativo). Al romper la mesa, conserva el mineral elegido.
  Cada mineral da una **bonificación** (se ve arriba a la derecha de la ventana; pasa el mouse para el detalle):

  | Borde | Bonificación |
  |---|---|
  | Oro | Cada nivel cuesta **15 % menos** |
  | Cobre | Los materiales valen **25 % más** puntos |
  | Amatista | **20 %** de probabilidad de **no gastar** los materiales |
  | Hierro | **15 %** de probabilidad de subir **2 niveles** (sin pasar del máximo) |
  | Diamante | **25 %** de probabilidad de que la conversión a Giga **no gaste la Estrella del Nether** |

## Costos y materiales (por defecto)
Costo para subir **al** nivel N = `ceil(40 × N^1.7)`:

| Nivel | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Puntos | 130 | 259 | 423 | 618 | 842 | 1094 | 1372 | 1676 | 2005 | 2358 | 2734 | 3132 | 3553 | 3995 |

### Cualquier bloque sirve
**Todos los bloques** (también los de otros mods) valen puntos según su **dureza**: tierra, arena y hojas 1; piedra, adoquín y troncos 2; pizarra profunda 3; los más duros hasta 10. Si el bloque es poco común vale ×2, raro ×4 y épico ×8. No cuentan las cajas de shulker ni otros bloques con contenido guardado, ni los irrompibles.

Con la mesa abierta, **el tooltip de cada ítem muestra cuánto vale** (y cuánto vale la pila entera).

### Valores fijos (tienen prioridad)
| Material | Puntos | Material | Puntos |
|---|---|---|---|
| Carbón / Redstone | 3 / 5 | Bloque de hierro | 180 |
| Lingote de cobre | 8 | Fragmento de eco | 150 |
| Lapislázuli, Cuarzo | 10 | Chatarra de netherita | 200 |
| Obsidiana | 10 | Ancient debris | 220 |
| Perla de ender, Vara de blaze | 15 | Mesa de encantamientos | 250 |
| Lingote de hierro | 20 | Bloque de oro | 270 |
| Lingote de oro, Botella de experiencia | 30 | Tótem de la inmortalidad | 300 |
| Esmeralda | 60 | Corazón del mar | 400 |
| Bloque de lapislázuli | 90 | Bloque de esmeralda | 540 |
| Diamante | 100 | Lingote de netherita | 600 |
| Mena de diamante | 120 | Bloque de diamante | 900 |
| | | Estrella del Nether | 1200 |
| | | Faro | 1500 |
| | | Huevo de dragón | 3000 |
| | | Bloque de netherita | 5400 |

También tienen valor fijo las menas (y su versión de pizarra), los minerales en bruto y sus bloques, el cuarzo, la amatista y otros ítems raros; la lista completa está en la config.

Como referencia, subir de V a XV cuesta unos 22 700 puntos: unos 227 diamantes, o unas 177 pilas de piedra.

El gasto usa primero los materiales más valiosos que caben completos en el costo, y completa con el más barato que alcance. **Si sobra algo del último material, no se devuelve.**

Todo se cambia en `config/ascension-common.toml` (se crea al abrir el juego por primera vez): `maxLevel` y, en la sección `[economy]`, `costBase`, `costExponent`, `autoBlockValues` (apagar el valor automático de los bloques), `maxAutoBlockValue` y la lista `materials` con formato `"namespace:item=puntos"`. Sirve también para ítems de otros mods; con `=0` un bloque deja de valer.

## Compilar
Requisitos: **JDK 25** (por ejemplo Microsoft OpenJDK 25) y conexión a internet la primera vez.

```bash
# Windows
gradlew.bat build
# Linux / macOS
./gradlew build
```
El `.jar` queda en `build/libs/ascension-1.4.0+mc26.2.jar`. Cópialo a la carpeta `mods` de una instalación de NeoForge 26.2.

Para probar sin instalar nada: `gradlew runClient` abre Minecraft con el mod cargado.

También puedes abrir la carpeta en IntelliJ IDEA (2025.3 o superior) como proyecto Gradle.

## Si falla la compilación
Copia el bloque de errores de Gradle (las líneas `error:` con su archivo y número de línea) y pásamelo para corregirlo. Si el juego se cierra, el detalle está en `run/logs/latest.log` o en `crash-reports/`.

## Notas
- Pasado el máximo vanilla, el número romano se muestra gracias a las traducciones `enchantment.level.11` a `20` que incluye el mod.
- Algunos efectos tienen tope propio en el juego (por ejemplo, la reducción de daño de Protección llega al 80 %), así que más nivel no siempre rinde más.
- Un libro mejorado se puede aplicar con el yunque, pero en supervivencia puede salir "¡Demasiado caro!" a niveles altos.
