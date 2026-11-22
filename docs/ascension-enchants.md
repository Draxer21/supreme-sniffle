# Ascension Enchantments

Mod para **Minecraft 26.2 + NeoForge**: encantamientos nuevos y sistema de parry. Funciona solo; si también tienes la **Mesa de Ascensión**, todos (menos Telequinesis) se pueden subir hasta XV.

## Encantamientos
| Encantamiento | Dónde | Niveles | Efecto |
|---|---|---|---|
| Robo de vida | Espadas, hachas, lanzas | I–III | Cura parte del daño cuerpo a cuerpo (10 %, 15 %, 18 %…) |
| Aceleración de golpe | Espadas, hachas, lanzas | I–III | +0,2 de velocidad de ataque por nivel |
| Azote de illagers | Armas | I–V | +2,5 de daño por nivel contra illagers, brujas, devastadores y vexes |
| Azote del End | Armas | I–V | +2,5 por nivel contra endermans, endermites, shulkers y el dragón |
| Azote del Nether | Armas | I–V | +2,5 por nivel contra blazes, ghasts, cubos de magma, piglins y hoglins |
| Desvío | Espadas y escudos | I–III | Amplía la ventana del parry perfecto y potencia el contraataque |
| Vena minera *(tesoro)* | Picos | I–III | Rompe hasta 8 minerales conectados extra por nivel. Agáchate para picar uno solo |
| Telequinesis *(tesoro)* | Herramientas y armas | I | Lo que sueltan bloques y mobs va directo al inventario |
| Paso etéreo *(tesoro)* | Botas | I–II | Mantén Shift en el aire: caes lento y sin daño por caída |
| Espinas de escarcha | Armadura | I–III | Quien te golpea cuerpo a cuerpo queda lento (más fuerte y más largo por nivel) |
| Reflejos | Pantalones | I–III | 5 % por nivel de esquivar por completo un golpe o proyectil |
| Segundo aliento *(tesoro)* | Pechera | I–III | Al bajar del 30 % de vida: te cura, da regeneración y absorción. Se recarga en 5 minutos |
| Flecha buscadora | Arcos y ballestas | I–III | Las flechas giran hacia el monstruo más cercano que tengas por delante |
| Lluvia de flechas *(tesoro)* | Arcos y ballestas | I–III | Cada disparo lanza 2 flechas extra por nivel en abanico (no se recogen) |
| Impacto explosivo *(tesoro)* | Arcos y ballestas | I–III | Las flechas explotan al impactar, sin romper bloques |
| Talador | Hachas | I–III | Al talar un tronco cae el árbol entero (32 troncos por nivel). Solo árboles con hojas, no casas. Agáchate para cortar uno |
| Fundición | Picos, hachas, palas | I | Lo que rompes cae fundido (minerales → lingotes, arena → vidrio…). Se combina con Fortuna; no con Toque de seda |
| Excavación *(tesoro)* | Picos y palas | I | Rompe 3×3 en el plano que miras. Agáchate para romper uno solo |

- Los "Azote" no se combinan con Filo, Castigo ni Perdición de los artrópodos.
- Los normales salen en la mesa de encantamientos; los de *tesoro* solo en cofres, aldeanos y pesca.
- **Agregar otro "Azote":** copia `data/ascension_enchants/enchantment/ender_slayer.json` con otro nombre y crea su lista de mobs en `data/ascension_enchants/tags/entity_type/`.

Prueba en creativo, con el ítem en la mano: `/enchant @s ascension_enchants:life_steal 3`
(o `telekinesis`, `vein_miner`, `ethereal_step`, `swift_strike`, `illager_slayer`, `ender_slayer`, `nether_slayer`, `parry`, `frost_thorns`, `reflexes`, `second_wind`, `homing`, `arrow_rain`, `explosive_arrows`, `timber`, `smelting`, `excavation`).

## Versiones Giga
Casi todos tienen versión **Giga** (nombre dorado): funciona como si tuviera **el doble de niveles** (Giga Robo de vida II = Robo de vida IV). Se consiguen convirtiendo el normal en su nivel máximo en la **Mesa de Ascensión** (botón ★ + Estrella del Nether), o en cofres y pescando. No van junto a su versión normal. No hace falta el mod Giga Enchantments.

## Parry
- **Todas las espadas bloquean**: mantén clic derecho para alzar la guardia (pose de la 1.8). Un bloqueo normal con espada absorbe el 50 % del daño frontal. Solo con la **mano secundaria vacía**, para que comer, poner antorchas o usar el escudo sigan igual.
- **Escudo**: bloquea como siempre, y también puede hacer parry (con su propio sonido de campanada). Desvío también se le puede poner.
- **Parry perfecto**: alza la guardia justo antes del golpe (ventana de 0,2 s). Si viene de frente:
  - no recibes daño;
  - el atacante queda lento, débil y sale empujado;
  - tu siguiente golpe en 3 s hace ×1,5 de daño con efecto de crítico;
  - flechas y bolas de fuego rebotan hacia quien las lanzó;
  - chispas, un "clang" metálico y un **destello en la pantalla** (más suave al desviar proyectiles).
- Alzar la guardia muy seguido (menos de 0,6 s entre una y otra) no cuenta como parry.
- **Desvío** amplía la ventana (hasta ~0,4 s) y el contraataque (hasta casi ×2).

## Compilar
Igual que la Mesa: con JDK 25, en esta carpeta `gradlew.bat build`. El `.jar` queda en `build\libs\ascension_enchants-1.3.0+mc26.2.jar`.
