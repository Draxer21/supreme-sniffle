# Changelog — Mesa de Ascensión

Formato de versión: `MAYOR.MENOR.PARCHE+mcVERSIÓN` (por ejemplo `1.1.0+mc26.2`).
- **MAYOR**: cambios que rompen mundos o configuraciones.
- **MENOR**: funciones nuevas.
- **PARCHE**: correcciones.

## 1.4.0+mc26.2
- Nuevo: slot de **libro**. Con un libro normal, botón **⇪** para **extraer** un encantamiento a libro; con un libro encantado, botón **✚** para **aplicarlo** al ítem (como el yunque, sin "demasiado caro").
- Nuevo: botón **−** para **bajar un nivel** y guardar la mitad de su costo como **puntos guardados en la mesa** (se gastan primero).
- Nuevo: **estanterías** alrededor abaratan 1 % cada una (hasta 15 %).
- Nuevo: al pasar el mouse por un botón, los **materiales que se van a gastar se marcan en rojo** con la cantidad.

## 1.3.0+mc26.2
- Nuevo: **todos los bloques valen puntos** (también los de otros mods), según su dureza y rareza. Se puede apagar con `autoBlockValues`.
- Nuevo: con la mesa abierta, el **tooltip** de cada ítem muestra su valor y el de la pila.
- **Economía reequilibrada (×10 y más cara):** los puntos se multiplicaron por 10 para que los bloques comunes puedan valer poco, y los costos subieron (`40 × N^1.7`; XV cuesta 3995 en vez de 229, unos 40 diamantes en vez de 23). Se añadieron valores fijos para menas, minerales en bruto, bloques de mineral y objetos raros.
- La configuración de costos y materiales pasó a la sección `[economy]` con los valores nuevos; los valores viejos de la raíz del archivo ya no se usan.
- En la lista de materiales, `=0` hace que un ítem no valga nada.

## 1.2.0+mc26.2
- Nuevo: **bonificación por borde**: oro (−15 % de costo), cobre (+25 % de puntos), amatista (20 % de no gastar materiales), hierro (15 % de +2 niveles), diamante (25 % de conservar la Estrella del Nether al convertir a Giga). Se muestra en la ventana con su descripción.
- Nuevo: **tema arcano** para la ventana (violeta oscuro con filetes dorados).

## 1.1.0+mc26.2
- Nuevo: **conversión a Giga**. Botón ★ junto a cada encantamiento vanilla que tenga versión Giga (requiere el mod Giga Enchantments). Si el encantamiento está en su nivel máximo normal y hay una Estrella del Nether en los materiales, se convierte en su versión Giga con el mismo nivel y se gasta la estrella.
- El `.jar` ahora incluye la versión de Minecraft en el nombre.

## 1.0.0+mc26.2
- Actualizado a Minecraft 26.2 / NeoForge 26.2.0.93.

## 1.0.0 (26.1.2)
- Primera versión: Mesa de Ascensión (subir encantamientos hasta XV con materiales), libro flotante animado, bordes de mineral, animaciones y partículas.
