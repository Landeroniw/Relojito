# Cómo probar la app en tu Black Shark 2

## Primera vez (configurar el teléfono)
1. En el teléfono: **Ajustes → Acerca del teléfono** → toca 7 veces sobre "Versión de MIUI/Joy UI" hasta que diga "Ya eres desarrollador".
2. Ve a **Ajustes → Configuración adicional → Opciones de desarrollador** y activa:
   - **Depuración USB**
   - **Instalar vía USB** (en MIUI suele estar oculta aquí, es clave para que `adb install` no falle)
3. Conecta el teléfono a la PC por USB.
4. En el teléfono aparecerá un diálogo "¿Permitir depuración USB?" → acepta y marca "Permitir siempre desde esta computadora".

## Cada vez que quieras probar un cambio
1. Abre el proyecto en **Android Studio** (`WatchApp`).
2. Conecta el teléfono por USB (o mantenlo conectado).
3. Arriba, junto al botón ▶️ Run, verifica que en el selector de dispositivos aparezca tu **Black Shark 2** (no un emulador). Si no aparece, revisa que la depuración USB siga activa y que hayas aceptado el diálogo de confianza.
4. Click en el botón ▶️ **Run 'app'** (o `Shift+F10`).
5. Android Studio compila, instala y abre la app automáticamente en el teléfono.

Si el teléfono muestra un diálogo pidiendo confirmar la instalación, acéptalo — solo pasa la primera vez con cada nueva firma de la app.

## Si algo falla
- **"No se detectan dispositivos"** → revisa el cable (algunos son solo de carga, no de datos) y que "Depuración USB" siga activada.
- **"INSTALL_FAILED_USER_RESTRICTED"** → falta activar "Instalar vía USB" en Opciones de desarrollador, o el teléfono está esperando que aceptes un diálogo en pantalla.
- **Cambios no se reflejan** → usa el botón ▶️ Run de nuevo (no hace falta desinstalar manualmente, Android Studio reinstala).
