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
    var showStreamingApps by remember { mutableStateOf(false) }

    // The M21 has a compact 4.7-inch screen. Calculate every major section from
    // the measured safe-area height instead of letting a tall Column scroll.
    BoxWithConstraints(Modifier.fillMaxSize().background(DeepBlack)) {
        val isTight = maxHeight < 650.dp
        val artHeight = (maxHeight * (if(s.compact) 0.18f else if(isTight) 0.215f else 0.245f))
            .coerceIn(92.dp, 172.dp)
        val dialSize = (maxWidth * 0.32f).coerceIn(98.dp, 136.dp)
        val smallKnobSize = if(isTight) 42.dp else 49.dp
        val dockHeight = if(isTight) 59.dp else 69.dp

        Column(
            Modifier.fillMaxSize().padding(top=4.dp,bottom=7.dp),
            verticalArrangement=Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal=17.dp).height(25.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                Text("FiiO", fontSize=18.sp, fontWeight=FontWeight.Black,color=Bone)
                Text("  |  M21", fontSize=12.sp,color=Dim)
                Spacer(Modifier.weight(1f))
                Text("HI-RES  •  CONTROL DECK", color=Amber,fontSize=8.sp)
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal=5.dp),
                verticalAlignment=Alignment.Top
            ) {
                Column(
                    Modifier.width(32.dp).height(artHeight),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Apps,"All apps",
                        Modifier.size(19.dp).clickable { s.page=Page.APPS },tint=Dim)
                    Text("VOL\nUP",modifier=Modifier.clickable { p.adjustVolume(1) },
                        color=Dim,fontSize=9.sp,lineHeight=10.sp)
                    Text("VOL\nDOWN",modifier=Modifier.clickable { p.adjustVolume(-1) },
                        color=Dim,fontSize=9.sp,lineHeight=10.sp)
                }
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier.fillMaxWidth().height(artHeight)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF25272B),Color(0xFF101114))))
                    ) {
                        AsyncImage(
                            model=current?.artwork ?: R.drawable.placeholder_album,
                            contentDescription="Now playing album artwork",
                            modifier=Modifier.fillMaxSize(),
                            contentScale=androidx.compose.ui.layout.ContentScale.Crop,
                            placeholder=painterResource(R.drawable.placeholder_album),
                            error=painterResource(R.drawable.placeholder_album)
                        )
                    }
                    Spacer(Modifier.height(if(isTight) 3.dp else 6.dp))
                    Text(p.title,color=Bone,fontSize=if(isTight) 17.sp else 19.sp,
                        fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,
                        lineHeight=21.sp)
                    Text(p.artist,color=Dim,fontSize=if(isTight) 11.sp else 12.sp,
                        maxLines=1,overflow=TextOverflow.Ellipsis,lineHeight=14.sp)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(current?.mimeType?.substringAfter('/')?.uppercase() ?: "LOCAL AUDIO",
                            color=Amber,fontSize=9.sp,maxLines=1)
                        Spacer(Modifier.width(6.dp))
                        Text("•",color=Dim,fontSize=9.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(current?.let { time(it.durationMs) } ?: "Offline music",
                            color=Dim,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if(current?.id in s.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",Modifier.size(18.dp).clickable { current?.id?.let { s.toggleFavorite(it) } },
                            tint=Amber
                        )
                    }
                }
                Box(Modifier.width(28.dp).height(artHeight),contentAlignment=Alignment.Center) {
                    Text("HOLD",color=Dim,fontSize=8.sp)
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal=22.dp)) {
                val progress=if(p.duration>0) (p.position.toFloat()/p.duration).coerceIn(0f,1f) else 0f
                Slider(
                    value=progress,
                    onValueChange={ if(p.duration>0) p.seek((it*p.duration).toLong()) },
                    modifier=Modifier.fillMaxWidth().height(22.dp)
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(time(p.position),fontSize=9.sp,color=Dim)
                    Text(time(p.duration),fontSize=9.sp,color=Dim)
                }
            }

            Row(
                Modifier.fillMaxWidth().height(if(isTight) 42.dp else 48.dp)
                    .padding(horizontal=15.dp),
                horizontalArrangement=Arrangement.SpaceEvenly,
                verticalAlignment=Alignment.CenterVertically
            ) {
                IconButton(onClick={p.updateShuffle(!p.shuffle)},modifier=Modifier.size(38.dp)) {
                    Icon(Icons.Default.Shuffle,"Shuffle",tint=if(p.shuffle) Amber else Dim)
                }
                IconButton(onClick={p.previous()},modifier=Modifier.size(38.dp)) {
                    Icon(Icons.Default.SkipPrevious,"Previous",tint=Bone)
                }
                Box(
                    Modifier.size(if(isTight) 41.dp else 46.dp).clip(CircleShape)
                        .background(Panel).clickable { p.toggle() },
                    contentAlignment=Alignment.Center
                ) {
                    Icon(if(p.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play or pause",tint=Amber,modifier=Modifier.size(27.dp))
                }
                IconButton(onClick={p.next()},modifier=Modifier.size(38.dp)) {
                    Icon(Icons.Default.SkipNext,"Next",tint=Bone)
                }
                IconButton(
                    onClick={p.updateRepeat(if(p.repeat==Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF)},
                    modifier=Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Repeat,"Repeat",tint=if(p.repeat!=Player.REPEAT_MODE_OFF) Amber else Dim)
                }
            }

            ControlPlate(
                s=s,
                diameter=dialSize,
                knobDiameter=smallKnobSize,
                tight=isTight,
                onSourceSelected={ source ->
                    s.source=source
                    if(source==Source.STREAMING) showStreamingApps=true
                },
                onHaptic={ if(s.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal=10.dp).height(dockHeight),
                verticalAlignment=Alignment.CenterVertically
            ) {
                Row(
                    Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(39.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF222428),Color(0xFF121315))))
                        .border(1.dp,Color(0xFF474448),RoundedCornerShape(39.dp))
                        .padding(horizontal=5.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    NavTile(Icons.Default.LibraryMusic,"Library",Modifier.weight(1f)) {s.page=Page.LIBRARY}
                    Box(Modifier.width(1.dp).height(22.dp).background(Color.DarkGray))
                    NavTile(Icons.Default.QueueMusic,"Playlists",Modifier.weight(1f)) {s.page=Page.PLAYLISTS}
                    Box(Modifier.width(1.dp).height(22.dp).background(Color.DarkGray))
                    NavTile(Icons.Default.Settings,"Settings",Modifier.weight(1f)) {s.page=Page.SETTINGS}
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("FiiO",color=Dim,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    Box(
                        Modifier.size(if(isTight) 43.dp else 51.dp)
                            .border(1.dp,Color.DarkGray,CircleShape).padding(5.dp)
                            .clip(CircleShape).background(Brush.radialGradient(listOf(Panel,DeepBlack)))
                            .clickable { s.page=Page.QUICK },
                        contentAlignment=Alignment.Center
                    ) {
                        Box(Modifier.size(7.dp).background(Amber,CircleShape))
                    }
                }
            }
        }
        if(showStreamingApps) StreamingServicesDialog(s.context) {showStreamingApps=false}
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
private fun ControlPlate(
    s:DeckState,
    diameter:androidx.compose.ui.unit.Dp,
    knobDiameter:androidx.compose.ui.unit.Dp,
    tight:Boolean,
    onSourceSelected:(Source)->Unit,
    onHaptic:()->Unit
) {
    val player=s.player
    Row(
        Modifier.fillMaxWidth().padding(horizontal=9.dp)
            .shadow(5.dp,RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF29292B),Color(0xFF19191B))))
            .border(1.dp,Color(0xFF464447),RoundedCornerShape(25.dp))
            .padding(start=12.dp,end=9.dp,top=if(tight) 11.dp else 14.dp,
                bottom=if(tight) 11.dp else 14.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SOURCE",color=Dim,fontSize=9.sp,letterSpacing=1.sp)
            Spacer(Modifier.height(if(tight) 5.dp else 9.dp))
            Row(
                Modifier.height(if(tight) 33.dp else 37.dp)
                    .clip(RoundedCornerShape(23.dp)).background(DeepBlack)
                    .border(1.dp,Color(0xFF484447),RoundedCornerShape(23.dp))
            ) {
                listOf(Source.LOCAL,Source.STREAMING).forEach { option ->
                    val selected=s.source==option
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if(selected) Color(0xFF313035) else Color.Transparent)
                            .clickable {onSourceSelected(option);onHaptic()},
                        contentAlignment=Alignment.Center
                    ) {
                        Text(if(option==Source.LOCAL) "LOCAL" else "STREAMING",
                            color=if(selected) Amber else Bone,fontSize=if(tight) 9.sp else 10.sp,
                            maxLines=1)
                    }
                }
            }
            Spacer(Modifier.height(if(tight) 10.dp else 14.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(if(tight) 8.dp else 12.dp)) {
                SmallKnob("EQ",knobDiameter) {s.page=Page.EQ;onHaptic()}
                SmallKnob("MODE",knobDiameter) {player.cycleMode();onHaptic()}
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    player.shuffle->"Shuffle"
                    player.repeat==Player.REPEAT_MODE_ALL->"Repeat all"
                    player.repeat==Player.REPEAT_MODE_ONE->"Repeat one"
                    else->"Normal"
                },
                color=Amber,fontSize=9.sp,
                maxLines=1
            )
        }
        Spacer(Modifier.width(7.dp))
        VolumeDial(s,onHaptic,diameter)
    }
}

@Composable
private fun SmallKnob(
    title:String,diameter:androidx.compose.ui.unit.Dp,onClick:()->Unit
) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(
            Modifier.size(diameter).shadow(4.dp,CircleShape).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF434346),Color(0xFF151516))))
                .border(2.dp,Color(0xFF09090A),CircleShape).clickable(onClick=onClick),
            contentAlignment=Alignment.TopCenter
        ) {
            Box(Modifier.padding(top=5.dp).width(3.dp).height(10.dp)
                .background(Amber,RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(4.dp))
        Text(title,color=Bone,fontSize=10.sp,letterSpacing=0.5.sp)
    }
}

@Composable
private fun VolumeDial(
    s:DeckState,
    onHaptic:()->Unit,
    diameter:androidx.compose.ui.unit.Dp
) {
    val p=s.player
    val fraction=p.volume.toFloat()/p.maxVolume.coerceAtLeast(1)
    Box(
        Modifier.size(diameter).clip(CircleShape)
            .pointerInput(p.maxVolume,s.dialSensitivity) {
                fun angle(pos:Offset):Float=Math.toDegrees(
                    atan2((pos.y-size.height/2f).toDouble(),(pos.x-size.width/2f).toDouble())
                ).toFloat()
                var previousAngle:Float?=null
                detectDragGestures(onDragStart={previousAngle=angle(it)},
                    onDragEnd={previousAngle=null}) {change,_->
                    val newAngle=angle(change.position)
                    val previous=previousAngle?:newAngle
                    var delta=newAngle-previous
                    if(delta>180f)delta-=360f
                    if(delta< -180f)delta+=360f
                    val old=p.volume
                    p.setVolumeFraction(p.volume.toFloat()/p.maxVolume.coerceAtLeast(1)+
                        delta/360f*s.dialSensitivity)
                    if(old!=p.volume)onHaptic()
                    previousAngle=newAngle
                    change.consume()
                }
            },
        contentAlignment=Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c=center
            val outer=size.minDimension/2f-3.dp.toPx()
            drawCircle(brush=Brush.radialGradient(listOf(Color(0xFF424245),DeepBlack),center=c,radius=outer),radius=outer)
            drawCircle(color=Color(0xFF070708),radius=outer*.88f,style=Stroke(5.dp.toPx()))
            drawCircle(
                brush=Brush.radialGradient(
                    listOf(Color(0xFF434346),Color(0xFF242425),Color(0xFF111112)),
                    center=Offset(c.x*.70f,c.y*.65f),radius=outer*1.15f
                ),
                radius=outer*.75f
            )
            drawArc(color=Amber,startAngle=-90f,sweepAngle=360f*fraction,useCenter=false,
                topLeft=Offset(c.x-outer*.91f,c.y-outer*.91f),
                size=androidx.compose.ui.geometry.Size(outer*1.82f,outer*1.82f),
                style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text("VOLUME",fontSize=8.sp,color=Dim,letterSpacing=0.8.sp)
            Text("${(fraction*100).toInt()}%",fontSize=if(diameter<116.dp) 17.sp else 20.sp,
                color=Bone,fontWeight=FontWeight.Medium)
        }
    }
}

@Composable
private fun StreamingServicesDialog(context:Context,onDismiss:()->Unit) {
    val candidates=listOf(
        "Spotify" to "com.spotify.music",
        "TIDAL" to "com.aspiro.tidal",
        "Apple Music" to "com.apple.android.music",
        "YouTube Music" to "com.google.android.apps.youtube.music"
    )
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Streaming services")},
        text={
            Column {
                Text("Choose an installed music app",color=Dim,fontSize=12.sp)
                Spacer(Modifier.height(9.dp))
                var found=false
                candidates.forEach {(label,pkg)->
                    val intent=context.packageManager.getLaunchIntentForPackage(pkg)
                    if(intent!=null) {
                        found=true
                        TextButton(onClick={
                            onDismiss()
                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) {
                            Icon(Icons.Default.PlayArrow,null,Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label)
                        }
                    }
                }
                if(!found) Text("No supported streaming apps installed. You can open other apps from the app drawer.",color=Dim)
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Close")}}
    )
}

fun time(ms:Long):String {
    val seconds=(ms.coerceAtLeast(0)/1000)
    return "%02d:%02d".format(seconds/60,seconds%60)
}
