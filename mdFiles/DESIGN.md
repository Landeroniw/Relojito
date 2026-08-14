# BlackShark Dock — Documento de Diseño

## Objetivo
Convertir un Black Shark 2 viejo en un panel de escritorio tipo "smart display": reloj permanente sin quemado de pantalla OLED, dashboard de información, control remoto de PC/música, y asistente de voz con wake word + LLM.

## Alcance y no-alcance
- **Sí:** app Android nativa, modo kiosco, standalone con conectividad WiFi bajo demanda.
- **No (por ahora):** flasheo de ROM, root, hardware adicional (docks, sensores externos).

## Arquitectura general

```
┌─────────────────────────────┐         WiFi (LAN)         ┌──────────────────────┐
│   Black Shark 2 (Android)   │◄───────────────────────────►│   PC (Windows)       │
│                              │   WebSocket / HTTP REST     │                      │
│  ┌────────────────────────┐ │                              │  Companion Server    │
│  │ Kiosk Launcher          │ │                              │  (control música,    │
│  │  - Auto-start on boot   │ │                              │   comandos, estado)  │
│  │  - Pantalla siempre     │ │                              └──────────────────────┘
│  │    activa (bloqueada)   │ │
│  └────────────────────────┘ │
│  ┌────────────────────────┐ │
│  │ Clock/Dashboard Screen  │ │
│  │  - Anti burn-in engine  │ │
│  │  - Widgets (clima, cal.)│ │
│  └────────────────────────┘ │
│  ┌────────────────────────┐ │
│  │ Voice Assistant Module  │ │
│  │  - Wake word (on-device)│ │
│  │  - LLM client (Claude)  │ │
│  └────────────────────────┘ │
└──────────────────────────────┘
```

## Fase 1 — Kiosk base + Reloj anti burn-in

### Kiosk Launcher
- `Activity` marcada como `HOME` (launcher) para que sea la pantalla por defecto al desbloquear/encender.
- `Lock Task Mode` (modo kiosco de Android, API 21+) para bloquear salida a otras apps sin PIN.
- `FLAG_KEEP_SCREEN_ON` + gestión de brillo según hora del día (atenuar de noche).
- Sin barra de estado/navegación (`WindowInsetsController` en modo inmersivo).

### Anti burn-in
Riesgos concretos en pantalla OLED con reloj estático: píxeles de los dígitos y widgets se degradan por exposición prolongada a la misma posición/color.

Mitigaciones (todas activas simultáneamente):
1. **Pixel shifting**: cada 60s, desplazar el layout completo ±(2-8px) en X/Y de forma pseudoaleatoria, dentro de un margen de seguridad.
2. **Paleta mayormente negra**: fondo `#000000` puro (apaga píxeles OLED reales), texto en gris/blanco solo donde es necesario.
3. **Brillo adaptativo bajo**: sensor de luz ambiental o reducción nocturna programada (ej. 20:00–08:00 más tenue).
4. **Rotación de posición de widgets secundarios** (clima, fecha) cada N minutos, no solo el reloj.
5. **Auto-dim/blank tras inactividad prolongada** si se detecta que nadie interactúa (opcional, requiere sensor de proximidad o cámara — evaluar si vale la pena).

### Stack técnico Fase 1
- Kotlin, Jetpack Compose (UI declarativa, facilita animaciones de pixel-shift).
- Sin dependencias de red todavía.
- `minSdk`: revisar versión de Android del Black Shark 2 (probablemente Android 9/10 — MIUI-based Joy UI). Confirmar antes de fijar `minSdk`.

## Fase 2 — Dashboard (widgets)
- Clima: API pública (Open-Meteo, sin key) para no depender de credenciales.
- Notificaciones/calendario: `NotificationListenerService` (requiere permiso especial) o integración directa con Google Calendar API.
- Layout modular: cada widget es un Composable independiente, fácil de activar/desactivar.

## Fase 3 — Control de PC
- **Companion Server** en la PC (Python o Node, ligero) exponiendo WebSocket en la LAN.
- Comandos iniciales: play/pause, siguiente/anterior, volumen, "abrir app X".
- Descubrimiento: IP fija configurada manualmente en la app (evitar complejidad de mDNS en fase inicial).
- Seguridad: token compartido simple (no exponer el server fuera de la LAN).

## Fase 4 — Asistente de voz
- Wake word on-device: motor tipo Porcupine (Picovoice) — corre local, bajo consumo, sin enviar audio continuo a la nube.
- Tras detectar wake word: graba comando corto → transcribe (Speech-to-Text on-device o API) → envía texto a LLM (Claude vía API) → ejecuta acción o responde por voz (TTS).
- Costos: llamadas a la API de Claude tienen costo por token; evaluar rate-limiting o comandos cacheados para acciones frecuentes.

## Preguntas abiertas (a resolver antes de Fase 3 y 4)
1. ¿La PC estará siempre encendida para que el companion server esté disponible?
2. ¿Qué tan crítica es la latencia del asistente de voz (afecta elección de motor STT)?
3. ¿Prefieres alojar el companion server como servicio de Windows o solo ejecutarlo manualmente?

## Próximos pasos inmediatos
1. Confirmar versión de Android del Black Shark 2 y `minSdk` viable.
2. Generar proyecto Gradle base (Fase 1) con Kiosk Launcher + pantalla de reloj.
3. Probar en el dispositivo real vía USB debugging.
