package com.m21deck.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.m21deck.R
import kotlin.math.atan2

@Composable
fun DeckApp(s: DeckState) {
    BackHandler(enabled = s.page != Page.HOME) { s.back() }
    Surface(Modifier.fillMaxSize(), color = DeepBlack) {
        when(s.page) {
            Page.HOME -> HomeScreen(s)
            Page.LIBRARY -> LibraryScreen(s)
            Page.PLAYLISTS -> PlaylistScreen(s)
            Page.PLAYLIST_DETAIL -> PlaylistDetailScreen(s)
            Page.SETTINGS -> SettingsScreen(s)
            Page.QUEUE -> QueueScreen(s)
            Page.EQ -> EqualizerScreen(s)
            Page.APPS -> AppsScreen(s)
            Page.QUICK -> QuickDeckScreen(s)
        }
    }
}

@Composable
private fun HomeScreen(s: DeckState) {
    val p = s.player
    val current = p.mediaId?.let(s::trackById)
    val view = LocalView.current
    Column(Modifier.fillMaxSize().background(DeepBlack).verticalScroll(rememberScrollState())
        .padding(top = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp), verticalAlignment=Alignment.CenterVertically) {
            Text("FiiO", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Black, color=Bone)
            Text("  |  M21", color=Dim, fontSize=14.sp)
            Spacer(Modifier.weight(1f))
            Text("HI-RES  •  CONTROL DECK", color=Amber, fontSize=9.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.width(44.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.Apps, "All apps", Modifier.size(20.dp).clickable { s.page = Page.APPS }, tint=Dim)
                Spacer(Modifier.height(45.dp))
                Text("VOL\nUP", modifier=Modifier.clickable { p.adjustVolume(1) }, fontSize=10.sp, color=Dim, lineHeight=12.sp)
                Spacer(Modifier.height(45.dp))
                Text("VOL\nDOWN", modifier=Modifier.clickable { p.adjustVolume(-1) }, fontSize=10.sp, color=Dim, lineHeight=12.sp)
            }
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(if(s.compact) 155.dp else 192.dp).clip(RoundedCornerShape(23.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B3038),Color(0xFF101114))))) {
                    AsyncImage(model = current?.artwork ?: R.drawable.placeholder_album,
                        contentDescription = "Now playing album artwork", modifier=Modifier.fillMaxSize(),
                        contentScale=androidx.compose.ui.layout.ContentScale.Crop,
                        placeholder=painterResource(R.drawable.placeholder_album),
                        error=painterResource(R.drawable.placeholder_album))
                }
                Spacer(Modifier.height(10.dp))
                Text(p.title, color=Bone, fontSize=20.sp, fontWeight=FontWeight.SemiBold,
                    maxLines=1, overflow=TextOverflow.Ellipsis)
                Text(p.artist, color=Dim, fontSize=13.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(current?.mimeType?.substringAfter('/')?.uppercase() ?: "LOCAL AUDIO", color=Amber, fontSize=10.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("•", color=Dim)
                    Spacer(Modifier.width(8.dp))
                    Text(current?.let { time(it.durationMs) } ?: "Offline music", color=Dim,fontSize=10.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(if(current?.id in s.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorite", Modifier.size(20.dp).clickable { current?.id?.let { s.toggleFavorite(it) } }, tint=Amber)
                }
            }
            Column(Modifier.width(31.dp).padding(start=5.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text("HOLD", color=Dim, fontSize=9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        val progress = if (p.duration > 0) (p.position.toFloat() / p.duration).coerceIn(0f,1f) else 0f
        Slider(progress, onValueChange = { if(p.duration > 0) p.seek((it*p.duration).toLong()) },
            Modifier.fillMaxWidth().padding(horizontal=28.dp).height(26.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=30.dp), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(time(p.position),fontSize=10.sp,color=Dim)
            Text(time(p.duration),fontSize=10.sp,color=Dim)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp), horizontalArrangement=Arrangement.SpaceEvenly,
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={ p.updateShuffle(!p.shuffle) }) { Icon(Icons.Default.Shuffle, "Shuffle", tint=if(p.shuffle) Amber else Dim) }
            IconButton(onClick={p.previous()}) { Icon(Icons.Default.SkipPrevious,"Previous") }
            Box(Modifier.size(49.dp).clip(CircleShape).background(Panel).clickable { p.toggle() },
                contentAlignment=Alignment.Center) { Icon(if(p.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play or pause", tint=Amber, modifier=Modifier.size(30.dp)) }
            IconButton(onClick={p.next()}) { Icon(Icons.Default.SkipNext,"Next") }
            IconButton(onClick={p.updateRepeat(if(p.repeat==Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF)}) {
                Icon(Icons.Default.Repeat,"Repeat", tint=if(p.repeat!=Player.REPEAT_MODE_OFF) Amber else Dim)
            }
        }
        Spacer(Modifier.height(14.dp))
        ControlPlate(s, onHaptic={ if(s.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) })
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp).height(81.dp), verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(41.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF24262A), Color(0xFF131416))))
                .border(1.dp, Color(0xFF48474A), RoundedCornerShape(41.dp)).padding(horizontal=8.dp),
                verticalAlignment=Alignment.CenterVertically) {
                NavTile(Icons.Default.LibraryMusic,"Library",Modifier.weight(1f)) { s.page=Page.LIBRARY }
                Box(Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))
                NavTile(Icons.Default.QueueMusic,"Playlists",Modifier.weight(1f)) { s.page=Page.PLAYLISTS }
                Box(Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))
                NavTile(Icons.Default.Settings,"Settings",Modifier.weight(1f)) { s.page=Page.SETTINGS }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("FiiO",color=Dim,fontWeight=FontWeight.Bold,fontSize=13.sp)
                Box(Modifier.size(59.dp).border(1.dp,Color.DarkGray,CircleShape).padding(6.dp)
                    .clip(CircleShape).background(Brush.radialGradient(listOf(Panel,DeepBlack)))
                    .clickable { s.page=Page.QUICK },contentAlignment=Alignment.Center) {
                    Box(Modifier.size(8.dp).background(Amber,CircleShape))
                }
            }
        }
        if(s.source==Source.STREAMING) StreamingRow(s.context)
    }
}

@Composable
private fun NavTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String,
                    modifier: Modifier = Modifier, onClick:()->Unit) {
    Column(modifier.fillMaxHeight().clickable(onClick=onClick), horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center) {
        Icon(icon,title,tint=Bone,modifier=Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(title,color=Bone,fontSize=11.sp)
    }
}

@Composable
private fun ControlPlate(s:DeckState, onHaptic:()->Unit) {
    val player=s.player
    Row(Modifier.fillMaxWidth().padding(horizontal=17.dp)
        .shadow(8.dp,RoundedCornerShape(30.dp))
        .clip(RoundedCornerShape(30.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF292B2E), Color(0xFF1C1E21))))
        .border(1.dp,Color(0xFF48474A),RoundedCornerShape(30.dp))
        .padding(start=15.dp,end=12.dp,top=22.dp,bottom=19.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SOURCE",color=Dim,fontSize=10.sp,letterSpacing=2.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.clip(RoundedCornerShape(25.dp)).background(DeepBlack)
                .border(1.dp,Color(0xFF4A494A),RoundedCornerShape(25.dp))) {
                listOf(Source.LOCAL,Source.STREAMING).forEach { option ->
                    val selected=s.source==option
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(22.dp))
                        .background(if(selected) Color(0xFF292A2D) else Color.Transparent)
                        .clickable { s.source=option; onHaptic() },contentAlignment=Alignment.Center) {
                        Text(if(option==Source.LOCAL) "LOCAL" else "STREAMING",color=if(selected) Amber else Bone,
                            fontSize=10.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                SmallKnob("EQ") { s.page=Page.EQ; onHaptic() }
                SmallKnob("MODE") { player.cycleMode(); onHaptic() }
            }
            Text(when {
                player.shuffle -> "Shuffle"
                player.repeat==Player.REPEAT_MODE_ALL -> "Repeat all"
                player.repeat==Player.REPEAT_MODE_ONE -> "Repeat one"
                else -> "Normal"
            },fontSize=10.sp,color=Amber,modifier=Modifier.padding(top=7.dp))
        }
        Spacer(Modifier.width(12.dp))
        VolumeDial(s,onHaptic)
    }
}

@Composable
private fun SmallKnob(title:String,onClick:()->Unit) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(57.dp).shadow(5.dp,CircleShape).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF44464A),Color(0xFF151618))))
            .border(2.dp,Color(0xFF090A0C),CircleShape).clickable(onClick=onClick),
            contentAlignment=Alignment.TopCenter) {
            Box(Modifier.padding(top=6.dp).width(3.dp).height(13.dp).background(Amber, RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Text(title,color=Bone,fontSize=11.sp,letterSpacing=1.sp)
    }
}

@Composable
private fun VolumeDial(s:DeckState,onHaptic:()->Unit) {
    val p=s.player
    val fraction = p.volume.toFloat()/p.maxVolume
    Box(Modifier.size(154.dp).clip(CircleShape)
        .pointerInput(p.maxVolume, s.dialSensitivity) {
            fun angle(pos: Offset): Float = Math.toDegrees(atan2(
                (pos.y - size.height/2f).toDouble(), (pos.x - size.width/2f).toDouble()
            )).toFloat()
            var previousAngle: Float? = null
            detectDragGestures(onDragStart={ previousAngle=angle(it) },onDragEnd={previousAngle=null}) { change,_ ->
                val newAngle=angle(change.position)
                val previous=previousAngle ?: newAngle
                var delta=newAngle-previous
                if(delta>180f) delta-=360f
                if(delta< -180f) delta+=360f
                val old=p.volume
                p.setVolumeFraction(p.volume.toFloat()/p.maxVolume + delta/360f * s.dialSensitivity)
                if(old!=p.volume) onHaptic()
                previousAngle=newAngle
                change.consume()
            }
        }, contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c=center
            val outer=size.minDimension/2f - 3.dp.toPx()
            drawCircle(brush=Brush.radialGradient(listOf(Color(0xFF444448),DeepBlack),center=c,radius=outer),radius=outer)
            drawCircle(color=Color(0xFF050506),radius=outer*.88f,style=Stroke(5.dp.toPx()))
            drawCircle(brush=Brush.radialGradient(listOf(Color(0xFF48494B),Color(0xFF242527),Color(0xFF111214)),center=Offset(c.x*.70f,c.y*.65f),radius=outer*1.15f),radius=outer*.75f)
            drawArc(color=Amber,startAngle=-90f,sweepAngle=360f*fraction,useCenter=false,
                topLeft=Offset(c.x-outer*.91f,c.y-outer*.91f),size=androidx.compose.ui.geometry.Size(outer*1.82f,outer*1.82f),
                style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text("VOLUME",fontSize=9.sp,color=Dim,letterSpacing=1.sp)
            Text("${(fraction*100).toInt()}%",fontSize=21.sp,color=Bone,fontWeight=FontWeight.Medium)
        }
    }
}

@Composable
private fun StreamingRow(context:Context) {
    val candidates = listOf("Spotify" to "com.spotify.music", "TIDAL" to "com.aspiro.tidal",
        "Apple Music" to "com.apple.android.music", "YouTube Music" to "com.google.android.apps.youtube.music")
    Column(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=12.dp)) {
        Text("STREAMING SERVICES",fontSize=11.sp,color=Amber,letterSpacing=1.sp)
        candidates.forEach { (label,pkg) ->
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if(intent!=null) TextButton(onClick={ context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                Icon(Icons.Default.PlayArrow,null,Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Open $label")
            }
        }
        Text("Streaming playback controls are controlled by the selected app. Global media-session integration is planned for the device-testing batch.",
            color=Dim,fontSize=11.sp)
    }
}

fun time(ms:Long):String {
    val seconds=(ms.coerceAtLeast(0)/1000)
    return "%02d:%02d".format(seconds/60,seconds%60)
}
