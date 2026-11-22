# Drax Morph

Mod para **Minecraft 26.2 + NeoForge**: mata un mob para **absorber su forma** y transfórmate en él.

## Cómo se usa
1. **Mata un mob.** El chat te avisa: "¡Absorbiste la forma de Araña!". Sirve cualquier mob, también los de otros mods (menos el dragón, el Wither y el gigante).
2. **G**: abre el menú de formas (con buscador). Pasa el mouse por una forma para ver su vida y sus habilidades. **Humano** te devuelve a la normalidad.
3. **V**: usa la habilidad especial de la forma.

Las formas absorbidas se conservan al morir. Las teclas se cambian en Controles.

## Qué cambia al transformarte
- **Tamaño:** tu hitbox y la altura de tu cámara son las del mob (como gallina pasas por huecos de un bloque).
- **Vida:** tu vida máxima pasa a ser la del mob (gallina 4, golem de hierro 100).
- **Aspecto:** los demás (y tú en tercera persona) te ven como el mob, caminando y mirando como tú.
- **Los mobs de tu misma especie no te atacan** (un zombi no ataca a un jugador zombi).

## Rasgos
| Rasgo | Formas |
|---|---|
| Vuela | murciélago, loro, abeja, allay, vex, phantom, ghast, ghast feliz, blaze |
| Cae lento | gallina |
| Trepa paredes | araña, araña de cueva |
| Respira bajo el agua y nada rápido | peces, calamares, delfín, ajolote, guardianes, tortuga, ahogado, rana |
| Inmune al fuego | blaze, ghast, cubo de magma, strider, esqueleto del Wither… (y cualquier mob inmune al fuego) |
| Visión nocturna | murciélago, arañas, gato, ocelote, zorro, enderman, phantom, warden |
| Rápido / muy rápido | gato, ocelote, zorro, lobo, conejo / caballos, burros, mulas, camello |
| Salta alto | conejo, cabra, slime, cubo de magma, rana, caballos, zorro |
| Sin daño por caída | gato, ocelote, conejo, cabra, slimes, gallina y todos los que vuelan |
| Fuerte / resistente | golem de hierro, devastador, warden, hoglin, zoglin, oso polar, lobo / golem, devastador, warden, shulker |

## Habilidades (tecla V)
| Forma | Habilidad | Recarga |
|---|---|---|
| Enderman, endermite | Teletransportarse adonde miras (hasta 32 bloques) | 1,5 s |
| Blaze | Bolas de fuego pequeñas | 0,4 s |
| Ghast | Bola de fuego explosiva | 2 s |
| Creeper | Explotar (rompe bloques si mobGriefing está activo) y vuelves a ser humano | 3 s |
| Breeze | Carga de viento | 1 s |
| Warden | Estallido sónico (10 de daño en línea recta) | 5 s |
| Golem de nieve | Bolas de nieve | 0,2 s |
| Esqueleto, stray, bogged | Disparar flechas (no se recogen) | 1 s |
| Shulker | Hace levitar al objetivo que miras | 2 s |

Tiene que estar en el servidor y en el cliente.

## Compilar
Con JDK 25, en esta carpeta: `gradlew.bat build`. El `.jar` queda en `build\libs\drax_morph-1.0.1+mc26.2.jar`.
