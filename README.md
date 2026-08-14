# BlackShark Dock

Convertir un Black Shark 2 viejo en un panel de escritorio tipo "smart display": reloj siempre encendido sin quemado de pantalla OLED, personalizable, con planes de dashboard, control de PC/música y asistente de voz.

App Android nativa (Kotlin + Jetpack Compose), paquete `com.bicu.myapplication`, `minSdk 26`. Diseño y roadmap completo en [mdFiles/DESIGN.md](mdFiles/DESIGN.md). Instrucciones para probar cambios en Android Studio: [mdFiles/TESTING.md](mdFiles/TESTING.md).

## Estado actual (Fase 1: base + personalización del reloj)

### Kiosco / comportamiento de pantalla
- La app se registra como launcher (`HOME`), además de su modo normal.
- Modo inmersivo: sin barra de estado ni de navegación.
- Pantalla siempre encendida (`FLAG_KEEP_SCREEN_ON`).
- Orientación forzable a Landscape o Landscape invertida (se elige y persiste desde Ajustes ⚙️, ya que no sabíamos de qué lado quedaría el cable conectado).
- Brillo reducido automáticamente en horario nocturno (20:00–08:00).
- Pixel shifting: el reloj se desplaza aleatoriamente cada minuto para prevenir burn-in.

### Diseño del reloj
- Layout tipo "flip clock": dos tarjetas (HH y MM) con esquinas redondeadas, separadas por un `:` real (no íconos), todo centrado.
- Las tarjetas tienen tamaño **fijo proporcional a la pantalla** (actualmente 40% del alto / 25% del ancho — como usar `vh`/`vw` en CSS), calculado una sola vez y compartido entre ambas para garantizar que midan exactamente igual.
- El tamaño de los números es una **fracción del tamaño de la tarjeta** (no un valor `sp` fijo), con un techo matemático que impide que el texto rompa el marco gris sin importar qué tan grande se elija.
- El `:` se dibuja siempre al 50% del tamaño de los dígitos.

### Personalización del reloj (mantener presionado el reloj)
Pantalla completa dividida en dos paneles: vista previa en vivo (izquierda) + controles (derecha).
- **Tamaño**: slider relativo al máximo seguro dentro de la tarjeta.
- **Estilo**: Light / Bold / Cursiva.
- **Fuente**: predefinidas del sistema (Predeterminada, Serif, Monospace, Cursiva) o cargar un archivo `.ttf`/`.otf` propio desde el teléfono (se copia al almacenamiento interno de la app para persistir entre reinicios).
- **Color**: selector HSV completo (caja de saturación/brillo arrastrable + slider de matiz en arcoíris + campo de texto hex editable).
- Botón "Guardar" al final (equivalente a la "X" de cerrar).
- Todo se persiste en `SharedPreferences` y se recarga al reabrir la app.

## Pendiente / próximos pasos
- **Fase 2 — Dashboard**: widgets de clima, notificaciones/calendario, con posiciones y tamaños configurables por el usuario (tipo tiles de Windows Phone). Aún no iniciado.
- **Fase 3 — Control de PC**: companion server en la PC + cliente en la app para controlar música/volumen/comandos por WiFi (LAN). No iniciado.
- **Fase 4 — Asistente de voz**: wake word on-device + LLM (Claude) para comandos conversacionales. No iniciado.
- Preguntas abiertas antes de Fase 3/4: si la PC estará siempre encendida, qué tan crítica es la latencia del asistente, cómo correr el companion server (servicio vs manual). Ver [mdFiles/DESIGN.md](mdFiles/DESIGN.md).

## Notas de desarrollo
- El build/testeo se hace desde Android Studio (botón ▶️ Run), no por línea de comandos — ver [mdFiles/TESTING.md](mdFiles/TESTING.md) para la configuración inicial del teléfono (depuración USB, instalar vía USB en MIUI/Joy UI).
- Convención de este proyecto: se dejan comentarios explicando las líneas de código nuevas/modificadas, para facilitar mantenimiento futuro.
