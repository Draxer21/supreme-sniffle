# Drax Grave

Mod para **Minecraft 26.2 + NeoForge**: cuando mueres, tus ítems no se desparraman ni desaparecen a los 5 minutos: quedan guardados en una **tumba** en el lugar donde moriste.

- Al morir, el chat te dice las **coordenadas** de la tumba y cuántas pilas de ítems guardó.
- **Clic derecho** sobre la tumba para recuperar todo: la armadura vuelve puesta (si ese espacio está libre) y el resto va al inventario; lo que no quepa cae al suelo.
- **Solo el dueño** puede abrirla (o alguien en creativo), **o quien tenga la brújula de tumba**. Los demás ven un aviso con el nombre del dueño.
- Al reaparecer recibes una **brújula de tumba** que apunta a ella. También es la **llave**: pásasela a un amigo y él podrá abrirla. Al abrir la tumba, la brújula desaparece.
- La tumba es **indestructible**: no se rompe con picos, explosiones, lava ni pistones.
- Si moriste en lava o agua, la tumba reemplaza el líquido. Si moriste en el vacío, aparece en el borde inferior del mundo.
- Si no hay espacio cerca (8 bloques hacia arriba, 3 alrededor), los ítems caen como en vanilla y te avisa.
- Guarda **toda tu experiencia** y te la devuelve al abrirla (si no hay espacio para la tumba, cae como orbes).
- Respeta `keepInventory` y la Maldición de desaparición.

## Compilar
Con JDK 25, en esta carpeta: `gradlew.bat build`. El `.jar` queda en `build\libs\drax_grave-1.1.0+mc26.2.jar`.
