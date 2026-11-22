# Drax Pack (Minecraft 26.2 + NeoForge)

Un solo `.jar` con los 10 mods de Draxer. Dentro del juego siguen apareciendo como 10 mods separados (cada uno con su id, versión y config), así que **los mundos que ya usaban los mods sueltos siguen funcionando**.

| Mod | Id | Versión | Qué hace |
|---|---|---|---|
| Ascension Table | `ascension` | 1.4.0 | Mesa que sube encantamientos hasta XV, libros, estanterías, bordes de mineral |
| Ascension Enchantments | `ascension_enchants` | 1.3.0 | Encantamientos nuevos, parry y sus versiones Giga |
| Giga Enchantments | `giga_enchants` | 1.1.0 | Versiones Giga (×2) de los encantamientos vanilla |
| Drax Sort | `drax_sort` | 1.3.0 | Ordenar, depositar (Z), rellenar (R), buscador, plantillas |
| Drax Grave | `drax_grave` | 1.1.0 | Tumba con ítems y experiencia, brújula-llave |
| Drax Builder | `drax_builder` | 1.2.0 | Varitas de construcción, relleno e intercambio, Ctrl+Z, luz (F7) |
| Drax Magnet | `drax_magnet` | 1.0.0 | Anillos imán (M) |
| Drax Backpacks | `drax_backpacks` | 1.0.0 | Mochilas con recolección automática |
| Drax Waypoints | `drax_waypoints` | 1.0.0 | Brújula en pantalla y puntos de ruta (B, N, H) |
| Drax Morph | `drax_morph` | 1.0.1 | Transformarse en los mobs que matas (G, V) |

La guía de cada mod está en [`docs/`](docs).

## Instalar
1. Quita de la carpeta `mods` los `.jar` sueltos de estos mods (si quedan, el juego dice "duplicate mod").
2. Pon `drax-pack-1.0.0+mc26.2.jar`.

## Compilar
- **En GitHub:** cada push compila solo (pestaña *Actions* → último run → artefacto `drax-pack`).
- **En tu PC:** con JDK 25, `gradlew.bat build`. El jar queda en `build/libs/`.
