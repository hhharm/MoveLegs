package app.movelegs

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Палитра: бумага, чернила, терракота, олива, горчица.
private val Paper = Color(0xFFF3EAD7)
private val PaperDark = Color(0xFFE6D9BD)
private val Ink = Color(0xFF2B2622)
private val Terracotta = Color(0xFFC65D3B)
private val Olive = Color(0xFF66742F)
private val Mustard = Color(0xFFD9A441)
private val Faded = Color(0xFF8A7F6C)

private val Serif = FontFamily.Serif
private val Typewriter = FontFamily.Monospace

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        if (store.currentId == null) store.pickNext()
        Reminders.schedule(this)

        setContent {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // tick читаем, чтобы экран перерисовался после действия из уведомления.
            key(tick) { Screen(store) }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

@Composable
private fun Screen(store: Store) {
    var rev by remember { mutableIntStateOf(0) }
    var fresh by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    val ctx = androidContext()
    key(rev) {
        val current = challengeById(store.currentId)
        Column(
            Modifier
                .fillMaxSize()
                .background(Paper)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Header(store)
            Ticket(current) {
                fresh = store.markDone()
                store.pickNext()
                Reminders.schedule(ctx)
                rev++
            }
            if (fresh.isNotEmpty()) NewSticker(fresh.first())
            Album(store)
            Settings(store) { rev++ }
            Text(
                TIPS.first(),
                style = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Faded, lineHeight = 20.sp),
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun androidContext() = androidx.compose.ui.platform.LocalContext.current

@Composable
private fun Header(store: Store) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Ноги", style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 44.sp, color = Ink, lineHeight = 42.sp))
            Text("в ход", style = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 44.sp, color = Terracotta, lineHeight = 42.sp))
        }
        Stamp("СЕГОДНЯ", "${store.todayCount} из $DAILY_GOAL", Terracotta, -6f)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "серия: ${store.streak} дн.  ·  всего: ${store.total}",
            style = TextStyle(fontFamily = Typewriter, fontSize = 13.sp, color = Ink),
        )
    }
}

/** Штамп: двойная рамка, моноширинный шрифт, чуть повёрнут. */
@Composable
private fun Stamp(top: String, bottom: String, color: Color, angle: Float) {
    Box(
        Modifier
            .rotate(angle)
            .border(BorderStroke(3.dp, color), RoundedCornerShape(6.dp))
            .padding(3.dp)
            .border(BorderStroke(1.dp, color), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, style = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color, letterSpacing = 2.sp))
            Text(bottom, style = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color))
        }
    }
}

/** Карточка-билетик с жёсткой тенью и полоской скотча. */
@Composable
private fun Ticket(c: Challenge, onDone: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .rotate(-1f)
                .offset(5.dp, 6.dp)
                .background(Ink, RoundedCornerShape(4.dp))
                .matchParentSize(),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .rotate(-1f)
                .background(Color(0xFFFFF8E8), RoundedCornerShape(4.dp))
                .border(2.dp, Ink, RoundedCornerShape(4.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "СЕЙЧАС" + if (c.standing) "  ·  встань" else "  ·  можно сидя",
                style = TextStyle(fontFamily = Typewriter, fontSize = 11.sp, letterSpacing = 2.sp, color = Faded),
            )
            Text(c.title, style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink, lineHeight = 32.sp))
            Text(c.how, style = TextStyle(fontFamily = Serif, fontSize = 16.sp, color = Ink, lineHeight = 23.sp))
            Text(c.amount, style = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Olive))
            Spacer(Modifier.height(4.dp))
            HardButton("Готово", onDone)
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .rotate(3f)
                .size(80.dp, 22.dp)
                .background(Mustard.copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun HardButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).background(Ink, RoundedCornerShape(4.dp)))
        Text(
            label,
            Modifier
                .fillMaxWidth()
                .background(Terracotta, RoundedCornerShape(4.dp))
                .border(2.dp, Ink, RoundedCornerShape(4.dp))
                .padding(vertical = 14.dp),
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Paper, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        )
    }
}

@Composable
private fun NewSticker(s: Sticker) {
    Row(
        Modifier.fillMaxWidth().background(Mustard.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StickerFace(s, true)
        Column {
            Text("НОВЫЙ СТИКЕР", style = TextStyle(fontFamily = Typewriter, fontSize = 11.sp, letterSpacing = 2.sp, color = Terracotta))
            Text(s.title, style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink))
        }
    }
}

@Composable
private fun StickerFace(s: Sticker, on: Boolean) {
    val tint = when (s.id) {
        "first", "total25" -> Olive
        "day_goal", "total100" -> Mustard
        else -> Terracotta
    }
    val shape = if (s.id == "day_goal" || s.id == "total100") RoundedCornerShape(14.dp) else CircleShape
    Box(
        Modifier
            .size(64.dp)
            .rotate(if (on) (s.id.hashCode() % 9 - 4).toFloat() else 0f)
            .then(
                if (on) Modifier.background(tint, shape).border(2.dp, Ink, shape)
                else Modifier.drawBehind {
                    drawRoundRect(
                        Faded, size = Size(size.width, size.height), cornerRadius = CornerRadius(size.width / 2),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            s.glyph,
            style = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (on) Paper else Faded),
        )
    }
}

@Composable
private fun Album(store: Store) {
    val have = store.unlocked
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("АЛЬБОМ  ${have.size}/${STICKERS.size}", style = TextStyle(fontFamily = Typewriter, fontSize = 12.sp, letterSpacing = 2.sp, color = Ink))
        Column(
            Modifier.fillMaxWidth().background(PaperDark, RoundedCornerShape(4.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            STICKERS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { s ->
                        Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            StickerFace(s, s.id in have)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (s.id in have) s.title else s.hint,
                                style = TextStyle(fontFamily = Serif, fontSize = 12.sp, color = if (s.id in have) Ink else Faded, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 15.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Settings(store: Store, changed: () -> Unit) {
    val ctx = androidContext()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("РАСПИСАНИЕ", style = TextStyle(fontFamily = Typewriter, fontSize = 12.sp, letterSpacing = 2.sp, color = Ink))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Напоминать каждые 30 мин", Modifier.weight(1f), style = TextStyle(fontFamily = Serif, fontSize = 16.sp, color = Ink))
            Chip(if (store.enabled) "ВКЛ" else "ВЫКЛ", store.enabled) {
                store.enabled = !store.enabled
                Reminders.schedule(ctx)
                changed()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("С", style = TextStyle(fontFamily = Serif, fontSize = 16.sp, color = Ink))
            Chip("−", false) { store.startHour = store.startHour - 1; Reminders.schedule(ctx); changed() }
            Text("%02d:00".format(store.startHour), style = TextStyle(fontFamily = Typewriter, fontSize = 16.sp, color = Ink))
            Chip("+", false) { if (store.startHour < store.endHour - 1) store.startHour = store.startHour + 1; Reminders.schedule(ctx); changed() }
            Spacer(Modifier.width(8.dp))
            Text("до", style = TextStyle(fontFamily = Serif, fontSize = 16.sp, color = Ink))
            Chip("−", false) { if (store.endHour > store.startHour + 1) store.endHour = store.endHour - 1; Reminders.schedule(ctx); changed() }
            Text("%02d:00".format(store.endHour), style = TextStyle(fontFamily = Typewriter, fontSize = 16.sp, color = Ink))
            Chip("+", false) { store.endHour = store.endHour + 1; Reminders.schedule(ctx); changed() }
        }
    }
}

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        Modifier
            .clickable(onClick = onClick)
            .background(if (on) Olive else Color.Transparent, RoundedCornerShape(3.dp))
            .border(2.dp, Ink, RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (on) Paper else Ink),
    )
}
