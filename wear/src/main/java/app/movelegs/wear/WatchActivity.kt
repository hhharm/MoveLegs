package app.movelegs.wear

import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable

private val Paper = Color(0xFFF3EAD7)
private val Ink = Color(0xFF2B2622)
private val Terracotta = Color(0xFFC65D3B)
private val Olive = Color(0xFF66742F)
private val Faded = Color(0xFF8A7F6C)

// Должно совпадать с app/WearSync.kt.
private const val STATE = "/state"
private const val DONE = "/done"
private const val SNOOZE = "/snooze"

data class WatchState(
    val title: String, val how: String, val amount: String,
    val today: Int, val goal: Int, val streak: Int,
)

class WatchActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var state by mutableStateOf<WatchState?>(null)
    private var sent by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WatchScreen(state, sent, ::send) }
    }

    override fun onResume() {
        super.onResume()
        val client = Wearable.getDataClient(this)
        client.addListener(this)
        // Data Layer хранит последнее значение, так что экран не пустой после запуска.
        client.getDataItems(Uri.Builder().scheme("wear").path(STATE).build())
            .addOnSuccessListener { items ->
                items.firstOrNull()?.let { apply(it) }
                items.release()
            }
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(events: com.google.android.gms.wearable.DataEventBuffer) {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == STATE }
            .forEach { apply(it.dataItem.freeze()) }
    }

    private fun apply(item: DataItem) {
        val m = DataMapItem.fromDataItem(item).dataMap
        state = WatchState(
            m.getString("title", ""), m.getString("how", ""), m.getString("amount", ""),
            m.getInt("today"), m.getInt("goal"), m.getInt("streak"),
        )
        sent = null
    }

    private fun send(path: String, label: String) {
        sent = label
        getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { Wearable.getMessageClient(this).sendMessage(it.id, path, ByteArray(0)) }
        }
    }
}

@Composable
private fun WatchScreen(s: WatchState?, sent: String?, send: (String, String) -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper)) {
        ScalingLazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (s == null) {
                item { Txt("Открой «Ноги в ход» на телефоне", 15.sp, FontFamily.Serif, Ink, align = TextAlign.Center) }
                return@ScalingLazyColumn
            }
            item {
                Box(
                    Modifier.rotate(-4f)
                        .border(2.dp, Terracotta, RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Txt("${s.today} из ${s.goal}  ·  серия ${s.streak}", 12.sp, FontFamily.Monospace, Terracotta, FontWeight.Bold)
                }
            }
            item { Txt(s.title, 20.sp, FontFamily.Serif, Ink, FontWeight.Bold, align = TextAlign.Center) }
            item { Txt(s.amount, 13.sp, FontFamily.Monospace, Olive, FontWeight.Bold, align = TextAlign.Center) }
            item { Txt(s.how, 12.sp, FontFamily.Serif, Ink, align = TextAlign.Center) }
            item {
                if (sent != null) {
                    Txt(sent.uppercase(), 13.sp, FontFamily.Monospace, Olive, FontWeight.Bold)
                } else {
                    Btn("Готово", Terracotta, Paper) { send(DONE, "засчитано") }
                }
            }
            if (sent == null) item { Btn("Позже", Color.Transparent, Ink) { send(SNOOZE, "через 10 мин") } }
        }
    }
}

@Composable
private fun Btn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.padding(end = 3.dp, bottom = 3.dp).clickable(onClick = onClick)) {
        Box(Modifier.matchParentSize().offset(3.dp, 3.dp).background(Ink, RoundedCornerShape(4.dp)))
        Box(
            Modifier.width(120.dp)
                .background(bg, RoundedCornerShape(4.dp))
                .border(2.dp, Ink, RoundedCornerShape(4.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Txt(label, 15.sp, FontFamily.Serif, fg, FontWeight.Black) }
    }
}

@Composable
private fun Txt(
    t: String, size: androidx.compose.ui.unit.TextUnit, font: FontFamily, color: Color,
    weight: FontWeight = FontWeight.Normal, align: TextAlign = TextAlign.Start,
) = BasicText(t, style = TextStyle(fontFamily = font, fontSize = size, color = color, fontWeight = weight, textAlign = align, fontStyle = FontStyle.Normal))
