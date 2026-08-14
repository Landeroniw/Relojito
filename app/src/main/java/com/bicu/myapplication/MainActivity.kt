package com.bicu.myapplication

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Nombre del archivo de SharedPreferences donde se guardan los ajustes,
// y la llave usada para la opción de orientación elegida.
private const val PREFS_NAME = "watch_app_settings"
private const val KEY_ORIENTATION = "orientation"

// Las dos orientaciones landscape posibles. Como no sabemos de qué lado
// quedará el cable conectado, el usuario puede elegir cuál usar desde ajustes.
enum class ScreenOrientationOption(val androidValue: Int, val label: String) {
    LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "Landscape"),
    LANDSCAPE_REVERSE(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, "Landscape invertida")
}

private fun loadOrientationOption(activity: Activity): ScreenOrientationOption {
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedName = prefs.getString(KEY_ORIENTATION, ScreenOrientationOption.LANDSCAPE.name)
    return ScreenOrientationOption.entries.find { it.name == savedName }
        ?: ScreenOrientationOption.LANDSCAPE
}

private fun saveOrientationOption(activity: Activity, option: ScreenOrientationOption) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_ORIENTATION, option.name)
    }
}

// --- Personalización del reloj (color, tamaño y estilo de fuente) ---

private const val KEY_CLOCK_COLOR = "clock_color"
private const val KEY_CLOCK_SIZE_FRACTION = "clock_size_fraction"
private const val KEY_CLOCK_STYLE = "clock_style"

private val DEFAULT_CLOCK_COLOR = Color.White
// Tamaño del número como fracción del alto de su tarjeta (0.6 = 60% del alto).
private const val DEFAULT_CLOCK_SIZE_FRACTION = 0.6f

// Límites de esa fracción: en 1.0 el número usa el tamaño máximo seguro
// calculado dentro de la tarjeta, en 0.3 queda pequeño con mucho aire alrededor.
// Como DigitCard ya calcula un techo que nunca rompe el marco, es seguro
// permitir hasta 1.0 sin riesgo de desbordar la tarjeta.
private const val CLOCK_SIZE_FRACTION_MIN = 0.3f
private const val CLOCK_SIZE_FRACTION_MAX = 1.0f

// Tamaño fijo de las tarjetas, proporcional a la pantalla (como usar
// unidades vh/vw en web): 80% del alto y 15% del ancho de la pantalla.
private const val CARD_HEIGHT_SCREEN_FRACTION = 0.8f
private const val CARD_WIDTH_SCREEN_FRACTION = 0.25f

// Las tres variantes de estilo que pidió el usuario. "Cursiva" usa peso
// normal + italic; Light y Bold cambian el grosor del trazo.
enum class ClockFontStyleOption(val label: String, val weight: FontWeight, val italic: Boolean) {
    LIGHT("Light", FontWeight.Light, italic = false),
    BOLD("Bold", FontWeight.Bold, italic = false),
    ITALIC("Cursiva", FontWeight.Normal, italic = true)
}

private fun loadClockColor(activity: Activity): Color {
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val argb = prefs.getInt(KEY_CLOCK_COLOR, DEFAULT_CLOCK_COLOR.toArgb())
    return Color(argb)
}

private fun saveClockColor(activity: Activity, color: Color) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_CLOCK_COLOR, color.toArgb())
    }
}

private fun loadClockSizeFraction(activity: Activity): Float {
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getFloat(KEY_CLOCK_SIZE_FRACTION, DEFAULT_CLOCK_SIZE_FRACTION)
}

private fun saveClockSizeFraction(activity: Activity, fraction: Float) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putFloat(KEY_CLOCK_SIZE_FRACTION, fraction)
    }
}

private fun loadClockStyle(activity: Activity): ClockFontStyleOption {
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedName = prefs.getString(KEY_CLOCK_STYLE, ClockFontStyleOption.LIGHT.name)
    return ClockFontStyleOption.entries.find { it.name == savedName }
        ?: ClockFontStyleOption.LIGHT
}

private fun saveClockStyle(activity: Activity, style: ClockFontStyleOption) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_CLOCK_STYLE, style.name)
    }
}

// Paleta de colores preseleccionados para el color picker (versión simple:
// swatches en vez de una rueda HSV completa).
private val ClockColorPalette = listOf(
    Color.White, Color.Red, Color(0xFFFF9800), Color.Yellow, Color.Green,
    Color.Cyan, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63), Color.Gray
)

// --- Fuente del reloj (predefinida o cargada por el usuario) ---

private const val KEY_CLOCK_FONT = "clock_font_selection"
// Marca especial en KEY_CLOCK_FONT que indica "usar la fuente cargada por
// el usuario" en vez de una de las predefinidas.
private const val CUSTOM_FONT_MARKER = "CUSTOM"
// Siempre guardamos la fuente externa con el mismo nombre de archivo, así
// no necesitamos recordar la ruta original ni pedir permisos de nuevo.
private const val CUSTOM_FONT_FILENAME = "clock_custom_font.ttf"

// Fuentes del sistema disponibles sin necesidad de cargar ningún archivo.
enum class BuiltInFontOption(val label: String, val family: FontFamily) {
    DEFAULT("Predeterminada", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Cursiva", FontFamily.Cursive)
}

private fun loadClockFontSelection(activity: Activity): String {
    val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CLOCK_FONT, BuiltInFontOption.DEFAULT.name)
        ?: BuiltInFontOption.DEFAULT.name
}

private fun saveClockFontSelection(activity: Activity, selection: String) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_CLOCK_FONT, selection)
    }
}

// Si ya existe un archivo de fuente personalizada guardado de una sesión
// anterior, lo carga; si no, devuelve null (usaremos una predefinida).
private fun loadCustomFontFamilyIfAny(activity: Activity): FontFamily? {
    val file = File(activity.filesDir, CUSTOM_FONT_FILENAME)
    if (!file.exists()) return null
    return runCatching { FontFamily(Font(file)) }.getOrNull()
}

// Decide qué FontFamily usar según la selección guardada: la personalizada
// (si aplica y sigue disponible) o una de las predefinidas.
private fun resolveFontFamily(selection: String, customFontFamily: FontFamily?): FontFamily {
    if (selection == CUSTOM_FONT_MARKER && customFontFamily != null) {
        return customFontFamily
    }
    return BuiltInFontOption.entries.find { it.name == selection }?.family
        ?: FontFamily.Default
}

// Copia el archivo elegido por el usuario (desde su Uri de content://) al
// almacenamiento interno de la app, para poder volver a cargarlo después
// sin depender de permisos sobre esa Uri externa.
private fun copyPickedFontToInternalStorage(activity: Activity, uri: Uri): FontFamily? {
    val destination = File(activity.filesDir, CUSTOM_FONT_FILENAME)
    return runCatching {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        FontFamily(Font(destination))
    }.getOrNull()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplica la orientación guardada (o Landscape por defecto la primera vez),
        // forzando que la pantalla no rote con el sensor del teléfono.
        requestedOrientation = loadOrientationOption(this).androidValue

        // Evita que la pantalla se apague/bloquee sola mientras la app está
        // en primer plano. Es lo que convierte al teléfono en un "display".
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Le dice al sistema que dibujaremos nuestro propio contenido detrás
        // de las barras de sistema, necesario para poder ocultarlas.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Oculta la barra de estado (arriba) y la barra de navegación (abajo)
        // para que la pantalla se vea como un display, no como una app.
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Con "swipe", las barras solo reaparecen temporalmente si el usuario
        // desliza desde el borde, y se vuelven a ocultar solas después.
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            ClockScreen()
        }
    }

    // Cuando la app vuelve a primer plano (ej. tras cambiar de app y regresar),
    // el sistema puede mostrar de nuevo las barras. Las re-ocultamos aquí.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

// Rango máximo (en dp) que el reloj se puede desplazar desde el centro.
// Suficiente para mover los píxeles encendidos, pero imperceptible a simple vista.
private const val PIXEL_SHIFT_RANGE_DP = 10
private const val PIXEL_SHIFT_INTERVAL_MS = 60_000L

// Horario nocturno: de 20:00 a 07:59 se reduce el brillo para no deslumbrar
// de noche y ayudar a que el OLED se desgaste menos.
private const val NIGHT_START_HOUR = 20
private const val NIGHT_END_HOUR = 8
private const val NIGHT_BRIGHTNESS = 0.05f
// -1 le dice a Android que use el brillo normal/automático del sistema.
private const val DAY_BRIGHTNESS = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
private const val BRIGHTNESS_CHECK_INTERVAL_MS = 60_000L

private fun isNightTime(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
}

@Composable
fun ClockScreen() {
    // Necesitamos la Activity (no solo el Context) porque el brillo y la
    // orientación se controlan a través de la Activity, no del Context genérico.
    val activity = LocalContext.current as Activity

    // Controla si el panel de ajustes está visible en pantalla.
    var showSettings by remember { mutableStateOf(false) }
    // Orientación actualmente seleccionada, cargada desde SharedPreferences.
    var orientationOption by remember { mutableStateOf(loadOrientationOption(activity)) }

    // Controla si el panel de personalización del reloj está visible
    // (se abre al mantener presionado el reloj).
    var showClockCustomizer by remember { mutableStateOf(false) }
    // Apariencia actual del reloj: color, tamaño y estilo de fuente.
    var clockColor by remember { mutableStateOf(loadClockColor(activity)) }
    var clockSizeFraction by remember { mutableStateOf(loadClockSizeFraction(activity)) }
    var clockStyle by remember { mutableStateOf(loadClockStyle(activity)) }
    // Selección de fuente: nombre de una predefinida, o CUSTOM_FONT_MARKER
    // si el usuario cargó su propio archivo. customFontFamily solo tiene
    // valor cuando ya existe una fuente personalizada guardada en disco.
    var clockFontSelection by remember { mutableStateOf(loadClockFontSelection(activity)) }
    var customFontFamily by remember { mutableStateOf(loadCustomFontFamilyIfAny(activity)) }
    val clockFontFamily = resolveFontFamily(clockFontSelection, customFontFamily)

    var currentTime by remember { mutableStateOf(formatTime()) }

    // Posición actual del desplazamiento anti burn-in, empieza centrada (0, 0).
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime()
            delay(1000)
        }
    }

    // Cada minuto, mueve el reloj a una nueva posición aleatoria dentro del
    // rango permitido, para que ningún píxel quede encendido de forma fija.
    LaunchedEffect(Unit) {
        while (true) {
            delay(PIXEL_SHIFT_INTERVAL_MS)
            offsetX = Random.nextInt(-PIXEL_SHIFT_RANGE_DP, PIXEL_SHIFT_RANGE_DP + 1).dp
            offsetY = Random.nextInt(-PIXEL_SHIFT_RANGE_DP, PIXEL_SHIFT_RANGE_DP + 1).dp
        }
    }

    // Revisa cada minuto si estamos en horario nocturno y ajusta el brillo
    // de la pantalla de la Activity en consecuencia.
    LaunchedEffect(Unit) {
        while (true) {
            val attributes = activity.window.attributes
            attributes.screenBrightness = if (isNightTime()) NIGHT_BRIGHTNESS else DAY_BRIGHTNESS
            activity.window.attributes = attributes
            delay(BRIGHTNESS_CHECK_INTERVAL_MS)
        }
    }
    //Definición del fondo como negro y el estilo de la fuente del reloj
    // BoxWithConstraints (en vez de Box) nos da maxWidth/maxHeight = el
    // tamaño real de la pantalla, para calcular el tamaño de tarjeta UNA
    // sola vez y pasarlo fijo a ambas tarjetas (evita que cada una calcule
    // su propio porcentaje por separado dentro del Row, que es donde
    // estaba la inconsistencia de tamaños entre la izquierda y la derecha).
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Separa "HH:mm" en sus dos partes para dibujarlas en tarjetas distintas.
        val timeParts = currentTime.split(":")
        val hourText = timeParts.getOrElse(0) { "00" }
        val minuteText = timeParts.getOrElse(1) { "00" }

        // Tamaño de tarjeta calculado una sola vez a partir de la pantalla real.
        val cardWidth = maxWidth * CARD_WIDTH_SCREEN_FRACTION
        val cardHeight = maxHeight * CARD_HEIGHT_SCREEN_FRACTION

        Row(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY)
                // detectTapGestures con onLongPress: mantener presionado el
                // reloj abre el panel de personalización.
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showClockCustomizer = true })
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DigitCard(
                text = hourText,
                color = clockColor,
                sizeFraction = clockSizeFraction,
                style = clockStyle,
                fontFamily = clockFontFamily,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )
            ColonSeparator(color = clockColor)
            DigitCard(
                text = minuteText,
                color = clockColor,
                sizeFraction = clockSizeFraction,
                style = clockStyle,
                fontFamily = clockFontFamily,
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )
        }

        // Ícono de engrane en la esquina superior derecha para abrir ajustes.
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = Color.White
            )
        }

        // Panel de ajustes: solo se dibuja cuando showSettings es true.
        if (showSettings) {
            SettingsPanel(
                selectedOrientation = orientationOption,
                onOrientationSelected = { selected ->
                    orientationOption = selected
                    saveOrientationOption(activity, selected)
                    activity.requestedOrientation = selected.androidValue
                },
                onClose = { showSettings = false }
            )
        }

        // Pantalla de personalización del reloj: solo visible tras el long-press.
        if (showClockCustomizer) {
            ClockCustomizerScreen(
                selectedColor = clockColor,
                sizeFraction = clockSizeFraction,
                selectedStyle = clockStyle,
                fontSelection = clockFontSelection,
                activeFontFamily = clockFontFamily,
                onColorSelected = { selected ->
                    clockColor = selected
                    saveClockColor(activity, selected)
                },
                onSizeChanged = { newFraction ->
                    clockSizeFraction = newFraction
                    saveClockSizeFraction(activity, newFraction)
                },
                onStyleSelected = { selected ->
                    clockStyle = selected
                    saveClockStyle(activity, selected)
                },
                onBuiltInFontSelected = { option ->
                    clockFontSelection = option.name
                    saveClockFontSelection(activity, option.name)
                },
                onCustomFontPicked = { uri ->
                    val loaded = copyPickedFontToInternalStorage(activity, uri)
                    if (loaded != null) {
                        customFontFamily = loaded
                        clockFontSelection = CUSTOM_FONT_MARKER
                        saveClockFontSelection(activity, CUSTOM_FONT_MARKER)
                    }
                },
                onClose = { showClockCustomizer = false }
            )
        }
    }
}

// Panel de ajustes superpuesto sobre toda la pantalla, con fondo semi-transparente
// para que se note que es una capa flotante encima del reloj.
@Composable
private fun SettingsPanel(
    selectedOrientation: ScreenOrientationOption,
    onOrientationSelected: (ScreenOrientationOption) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ajustes", color = Color.White, fontSize = 24.sp)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Text(
                text = "Orientación de pantalla",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            // Una fila por cada opción de orientación disponible, con un
            // RadioButton indicando cuál está activa actualmente.
            ScreenOrientationOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOrientationSelected(option) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option == selectedOrientation,
                        onClick = { onOrientationSelected(option) }
                    )
                    Text(text = option.label, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

// Ya no incluye segundos: el diseño tipo "flip clock" solo muestra HH:mm.
private fun formatTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

// Color de fondo de cada tarjeta de dígitos, gris oscuro sobre el negro puro
// del fondo general (mantiene buen contraste sin ser un gris demasiado claro).
private val DigitCardColor = Color(0xFF1C1C1E)

// Margen interno que dejamos libre dentro de la tarjeta (a cada lado / arriba
// y abajo) para que el número nunca quede pegado al borde redondeado.
private const val CARD_INNER_PADDING_FRACTION = 0.12f

// Ancho aproximado de un dígito respecto a su propio tamaño de fuente, para
// fuentes sans-serif estándar. Es una estimación (no medimos el texto real),
// pero es suficiente para calcular un techo seguro y evitar que se desborde.
private const val DIGIT_WIDTH_TO_FONT_SIZE_RATIO = 0.62f
private const val DIGITS_PER_CARD = 2

// Tarjeta con tamaño FIJO proporcional a la pantalla (80% alto / 15% ancho,
// como usar vh/vw en CSS) — no crece ni encoge con el contenido.
//
// El tamaño del número tiene "doble reactividad":
// 1) La tarjeta ya es proporcional a la pantalla (reactividad #1).
// 2) Dentro de la tarjeta, calculamos el tamaño de fuente MÁXIMO que cabe
//    sin romper ni el alto ni el ancho disponibles, y "sizeFraction" (el
//    slider) solo escala qué tan cerca de ese máximo seguro queremos estar
//    (reactividad #2). Así el número nunca puede salirse del marco gris,
//    sin importar qué tan arriba se mueva el slider.
@Composable
private fun DigitCard(
    text: String,
    color: Color,
    sizeFraction: Float,
    style: ClockFontStyleOption,
    fontFamily: FontFamily,
    // Tamaño de la tarjeta ya calculado UNA vez en ClockScreen (a partir del
    // tamaño real de pantalla) y pasado igual a ambas tarjetas, para
    // garantizar que midan exactamente lo mismo.
    cardWidth: Dp,
    cardHeight: Dp
) {
    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(DigitCardColor),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        // Espacio realmente disponible para el texto, descontando el margen interno.
        val usableHeight = cardHeight * (1f - CARD_INNER_PADDING_FRACTION * 2)
        val usableWidth = cardWidth * (1f - CARD_INNER_PADDING_FRACTION * 2)

        // Tamaño máximo que el texto podría tener según cada dimensión por separado.
        val maxFontSizeFromHeight = usableHeight
        val maxFontSizeFromWidth = usableWidth / (DIGITS_PER_CARD * DIGIT_WIDTH_TO_FONT_SIZE_RATIO)

        // El techo real es el más restrictivo de los dos: así el número
        // jamás rompe la barrera de la tarjeta en ninguna dirección.
        val safeMaxFontSize = if (maxFontSizeFromHeight < maxFontSizeFromWidth) {
            maxFontSizeFromHeight
        } else {
            maxFontSizeFromWidth
        }

        val fontSize = with(density) { (safeMaxFontSize * sizeFraction).toSp() }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = style.weight,
            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = fontFamily
        )
    }
}

// Los dos puntos verticales que separan la tarjeta de hora de la de minutos.
// Usa el mismo color que el reloj para que se vea como un solo conjunto.
@Composable
private fun ColonSeparator(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// Pantalla completa de personalización del reloj: panel izquierdo con una
// tarjeta de muestra en vivo, panel derecho con todos los controles
// (tamaño, estilo, fuente y color).
@Composable
private fun ClockCustomizerScreen(
    selectedColor: Color,
    sizeFraction: Float,
    selectedStyle: ClockFontStyleOption,
    fontSelection: String,
    activeFontFamily: FontFamily,
    onColorSelected: (Color) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onStyleSelected: (ClockFontStyleOption) -> Unit,
    onBuiltInFontSelected: (BuiltInFontOption) -> Unit,
    onCustomFontPicked: (Uri) -> Unit,
    onClose: () -> Unit
) {
    // Selector de archivos del sistema: al elegir uno, entrega su Uri.
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onCustomFontPicked(uri) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // --- Panel izquierdo: vista previa en vivo ---
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Tamaño de la tarjeta de muestra, proporcional a ESTE panel
                // (no a la pantalla completa), para que se vea bien en el espacio disponible.
                val previewCardWidth = maxWidth * 0.5f
                val previewCardHeight = maxHeight * 0.75f
                DigitCard(
                    text = "12",
                    color = selectedColor,
                    sizeFraction = sizeFraction,
                    style = selectedStyle,
                    fontFamily = activeFontFamily,
                    cardWidth = previewCardWidth,
                    cardHeight = previewCardHeight
                )
            }

            // --- Panel derecho: controles, con scroll por si no caben todos ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Personalizar reloj", color = Color.White, fontSize = 22.sp)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // --- Tamaño (relativo al alto de la tarjeta, no sp absoluto) ---
                Text(
                    text = "Tamaño",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
                Slider(
                    value = sizeFraction,
                    onValueChange = onSizeChanged,
                    valueRange = CLOCK_SIZE_FRACTION_MIN..CLOCK_SIZE_FRACTION_MAX
                )

                // --- Estilo (Light / Bold / Cursiva) ---
                Text(
                    text = "Estilo",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                ClockFontStyleOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStyleSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedStyle,
                            onClick = { onStyleSelected(option) }
                        )
                        Text(text = option.label, color = Color.White, fontSize = 16.sp)
                    }
                }

                // --- Fuente: predefinidas + carga de archivo externo ---
                Text(
                    text = "Fuente",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                BuiltInFontOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBuiltInFontSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = fontSelection == option.name,
                            onClick = { onBuiltInFontSelected(option) }
                        )
                        Text(text = option.label, color = Color.White, fontSize = 16.sp)
                    }
                }
                // La fuente cargada por el usuario aparece como una opción más,
                // seleccionada automáticamente en cuanto termina de cargarse.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = fontSelection == CUSTOM_FONT_MARKER,
                        onClick = { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) }
                    )
                    Text(
                        text = "Cargar fuente personalizada (.ttf/.otf)...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                // --- Color picker (swatches) ---
                Text(
                    text = "Color",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                // Agrupa la paleta en filas de 5 swatches para que quepan en el panel.
                ClockColorPalette.chunked(5).forEach { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        rowColors.forEach { swatchColor ->
                            ColorSwatch(
                                color = swatchColor,
                                selected = swatchColor == selectedColor,
                                onClick = { onColorSelected(swatchColor) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Un círculo de color seleccionable dentro del color picker. El borde blanco
// indica cuál es el color activo actualmente.
@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(2.dp, Color.White, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}
