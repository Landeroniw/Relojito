package com.bicu.myapplication

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Separa "HH:mm" en sus dos partes para dibujarlas en tarjetas distintas.
        val timeParts = currentTime.split(":")
        val hourText = timeParts.getOrElse(0) { "00" }
        val minuteText = timeParts.getOrElse(1) { "00" }

        Row(
            modifier = Modifier.offset(x = offsetX, y = offsetY),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DigitCard(text = hourText)
            ColonSeparator()
            DigitCard(text = minuteText)
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

// Tarjeta redondeada con los dos dígitos de la hora o de los minutos.
@Composable
private fun DigitCard(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(DigitCardColor)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Light
        )
    }
}

// Los dos puntos verticales que separan la tarjeta de hora de la de minutos.
@Composable
private fun ColonSeparator() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
