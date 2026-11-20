package com.m21deck.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.m21deck.R
import com.m21deck.data.M3u
import com.m21deck.data.Playlist
import com.m21deck.data.Track

@Composable
private fun ScreenShell(name:String,s:DeckState,actions:@Composable RowScope.()->Unit={},content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={s.back()}) { Icon(Icons.Default.ArrowBack,"Back") }
            Text(name,Modifier.weight(1f),color=Bone,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            actions()
        }
        HorizontalDivider(color=Color(0xFF28292C))
        content()
    }
}

@Composable
fun LibraryScreen(s:DeckState) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Track?>(null) }
    val headings=listOf("Songs","Albums","Artists","Folders","Favorites","Recent")
    val search=s.tracks.filter {
        query.isBlank() || listOf(it.title,it.artist,it.album,it.folder).any { value -> value.contains(query,true) }
    }
    val list = when(tab) {
        4 -> search.filter { it.id in s.favorites }
        5 -> search.sortedByDescending { it.dateAdded }
        else -> search.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
    ScreenShell("Library",s,actions={
        IconButton(onClick={s.page=Page.QUEUE}) { Icon(Icons.Default.QueueMusic,"Queue") }
        IconButton(onClick={s.refreshLibrary()}) { Icon(Icons.Default.Refresh,"Rescan") }
    }) {
        OutlinedTextField(value=query,onValueChange={query=it},singleLine=true,placeholder={Text("Search music, artist, folder")},
            leadingIcon={Icon(Icons.Default.Search,null)},modifier=Modifier.fillMaxWidth().padding(12.dp))
        ScrollableTabRow(selectedTabIndex=tab,edgePadding=4.dp,containerColor=DeepBlack) {
            headings.forEachIndexed { i,name -> Tab(selected=tab==i,onClick={tab=i},text={Text(name)}) }
        }
        if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.error?.let { Text(it,Modifier.padding(12.dp),color=Amber) }
        if(tab in 1..3) {
            val groups = when(tab) {
                1 -> list.groupBy { it.album }
                2 -> list.groupBy { it.artist }
                else -> list.groupBy { it.folder }
            }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            var expanded by remember(tab,query) { mutableStateOf<String?>(null) }
            LazyColumn(contentPadding=PaddingValues(bottom=80.dp)) {
                groups.forEach { (name, songs) ->
                    item(key="group:$name") {
                        ListItem(headlineContent={Text(name,maxLines=2)},supportingContent={Text("${songs.size} songs")},
                            leadingContent={Icon(if(tab==1) Icons.Default.Album else if(tab==2) Icons.Default.Person else Icons.Default.Folder,null)},
                            trailingContent={Icon(Icons.Default.ChevronRight,null)},modifier=Modifier.clickable { expanded = if(expanded==name) null else name })
                        HorizontalDivider(color=Color(0xFF222326))
                    }
                    if(expanded==name) items(songs,key={"song:${it.id}"}) { song ->
                        TrackRow(song,s,songs,{selected=it})
                    }
                }
            }
        } else {
            if(list.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                Text("No songs here yet. Scan your music folders.",color=Dim)
            }
            else LazyColumn(contentPadding=PaddingValues(bottom=80.dp)) {
                items(list,key={it.id}) { track -> TrackRow(track,s,list,{selected=it}) }
            }
        }
    }
    selected?.let { track -> TrackOptions(track,s,onClose={selected=null}) }
}

@Composable
private fun TrackRow(track:Track,s:DeckState,queue:List<Track>,onOptions:(Track)->Unit) {
    val index=queue.indexOfFirst { it.id==track.id }
    ListItem(
        headlineContent={ Text(track.title,maxLines=1,overflow=TextOverflow.Ellipsis) },
        supportingContent={ Text("${track.artist} · ${track.album}",maxLines=1,overflow=TextOverflow.Ellipsis,color=Dim) },
        leadingContent={
            AsyncImage(model=track.artwork,contentDescription="Album art",modifier=Modifier.size(45.dp).clip(RoundedCornerShape(7.dp)),
                contentScale=androidx.compose.ui.layout.ContentScale.Crop,
                placeholder=painterResource(R.drawable.placeholder_album),error=painterResource(R.drawable.placeholder_album))
        },
        trailingContent={ IconButton(onClick={onOptions(track)}) {Icon(Icons.Default.MoreVert,"More song options")}},
        modifier=Modifier.clickable { s.player.playTracks(queue,index.coerceAtLeast(0)) }
    )
    HorizontalDivider(color=Color(0xFF25262A))
}

@Composable
private fun TrackOptions(track:Track,s:DeckState,onClose:()->Unit) {
    var showPlaylists by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onClose,title={Text(track.title,maxLines=2)},
        text={ Column {
            Text("${track.artist} · ${time(track.durationMs)}",color=Dim)
            Spacer(Modifier.height(6.dp))
            TextButton(onClick={s.toggleFavorite(track.id);onClose()}) {
                Icon(if(track.id in s.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null)
                Spacer(Modifier.width(9.dp)); Text(if(track.id in s.favorites) "Remove favorite" else "Add to favorites")
            }
            TextButton(onClick={showPlaylists=true}) { Icon(Icons.Default.PlaylistAdd,null);Spacer(Modifier.width(9.dp));Text("Add to playlist") }
            if(showPlaylists) {
                if(s.playlists.isEmpty()) Text("Create a playlist first",color=Dim)
                s.playlists.forEach { pl -> TextButton(onClick={s.addToPlaylist(pl.id,track.id);onClose()}) { Text(pl.name) } }
            }
        } }, confirmButton={TextButton(onClick=onClose){Text("Close")}})
}

@Composable
fun PlaylistScreen(s:DeckState) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val importPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { s.context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->reader.readText() } }
                .getOrNull()?.let { text -> s.importM3u("Imported ${System.currentTimeMillis()%100000}",text) }
        }
    }
    ScreenShell("Playlists",s,actions={
        IconButton(onClick={importPicker.launch(arrayOf("audio/x-mpegurl","application/vnd.apple.mpegurl","text/plain","*/*"))}) {
            Icon(Icons.Default.FileOpen,"Import M3U")
        }
        IconButton(onClick={showCreate=true}) { Icon(Icons.Default.Add,"Create playlist") }
    }) {
        Text("Offline collections · ${s.playlists.size} playlists",color=Dim,modifier=Modifier.padding(16.dp))
        LazyColumn {
            items(s.playlists,key={it.id}) { pl ->
                ListItem(headlineContent={Text(pl.name)},supportingContent={Text("${pl.trackIds.size} tracks")},
                    leadingContent={Icon(Icons.Default.QueueMusic,null,tint=Amber)},
                    trailingContent={Icon(Icons.Default.ChevronRight,null)},
                    modifier=Modifier.clickable { s.openPlaylist(pl.id) })
                HorizontalDivider(color=Color(0xFF25262A))
            }
        }
    }
    if(showCreate) AlertDialog(onDismissRequest={showCreate=false},title={Text("New playlist")},
        text={OutlinedTextField(name,{name=it},singleLine=true,label={Text("Playlist name")})},
        confirmButton={TextButton(onClick={s.createPlaylist(name);showCreate=false;name=""},enabled=name.isNotBlank()) { Text("Create") }},
        dismissButton={TextButton(onClick={showCreate=false}) {Text("Cancel")}})
}

@Composable
fun PlaylistDetailScreen(s:DeckState) {
    val pl=s.playlists.firstOrNull { it.id==s.selectedPlaylist }
    if(pl==null) { ScreenShell("Playlist",s) { Text("Playlist not found",Modifier.padding(16.dp)) };return }
    val byId=s.tracks.associateBy { it.id }
    val tracks=pl.trackIds.mapNotNull(byId::get)
    var rename by remember(pl.id) { mutableStateOf(false) }
    var delete by remember(pl.id) { mutableStateOf(false) }
    var newName by remember(pl.id) { mutableStateOf(pl.name) }
    val exportPicker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        uri?.let { runCatching {
            s.context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { out -> M3u.export(tracks,out) }
        } }
    }
    ScreenShell(pl.name,s,actions={
        IconButton(onClick={exportPicker.launch("${pl.name}.m3u")}) { Icon(Icons.Default.IosShare,"Export M3U") }
        IconButton(onClick={rename=true}) {Icon(Icons.Default.Edit,"Rename playlist")}
        IconButton(onClick={delete=true}) {Icon(Icons.Default.Delete,"Delete playlist")}
    }) {
        Button(onClick={s.player.playTracks(tracks)},Modifier.fillMaxWidth().padding(14.dp),enabled=tracks.isNotEmpty()) {
            Icon(Icons.Default.PlayArrow,null); Text("Play ${tracks.size} tracks")
        }
        Text("Add songs via Library → song menu → Add to playlist",color=Dim,modifier=Modifier.padding(horizontal=16.dp,vertical=3.dp),fontSize=12.sp)
        LazyColumn(contentPadding=PaddingValues(bottom=90.dp)) {
            items(tracks,key={it.id}) { track ->
                val ix=pl.trackIds.indexOf(track.id)
                ListItem(headlineContent={Text(track.title,maxLines=1)},supportingContent={Text(track.artist,color=Dim)},
                    modifier=Modifier.clickable {s.player.playTracks(tracks,tracks.indexOfFirst {it.id==track.id})},
                    trailingContent={Row(verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={s.reorderPlaylist(pl.id,ix,ix-1)},enabled=ix>0) {Icon(Icons.Default.KeyboardArrowUp,"Move up")}
                        IconButton(onClick={s.reorderPlaylist(pl.id,ix,ix+1)},enabled=ix<pl.trackIds.lastIndex) {Icon(Icons.Default.KeyboardArrowDown,"Move down")}
                        IconButton(onClick={s.removeFromPlaylist(pl.id,track.id)}) {Icon(Icons.Default.Close,"Remove from playlist")}
                    }})
                HorizontalDivider(color=Color(0xFF25262A))
            }
        }
    }
    if(rename) AlertDialog(onDismissRequest={rename=false},title={Text("Rename playlist")},
        text={OutlinedTextField(newName,{newName=it})},confirmButton={TextButton(onClick={s.renamePlaylist(pl.id,newName);rename=false}){Text("Save")}},
        dismissButton={TextButton(onClick={rename=false}){Text("Cancel")}})
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("Delete ${pl.name}?")},
        text={Text("The music files won't be deleted.")},confirmButton={TextButton(onClick={s.deletePlaylist(pl.id);delete=false}){Text("Delete")}},
        dismissButton={TextButton(onClick={delete=false}){Text("Cancel")}})
}

@Composable
fun QueueScreen(s:DeckState) {
    val ids=s.player.queueIds()
    val tracks=ids.mapNotNull(s::trackById)
    ScreenShell("Now Playing Queue",s) {
        Text("${ids.size} queued tracks · drag-free move controls",color=Dim,modifier=Modifier.padding(16.dp))
        LazyColumn {
            items(tracks,key={it.id}) { track ->
                val i=ids.indexOf(track.id)
                ListItem(headlineContent={Text(track.title)},supportingContent={Text(track.artist)},
                    leadingContent={Text("${i+1}",color=Amber)},
                    trailingContent={Row {
                        IconButton(onClick={s.player.moveQueue(i,i-1)},enabled=i>0){Icon(Icons.Default.ArrowUpward,"Move up")}
                        IconButton(onClick={s.player.moveQueue(i,i+1)},enabled=i<ids.lastIndex){Icon(Icons.Default.ArrowDownward,"Move down")}
                    }},modifier=Modifier.clickable { s.player.seek(0L); if(i!=s.player.index) s.player.playTracks(tracks,i) })
                HorizontalDivider(color=Color(0xFF25262A))
            }
        }
    }
}

private fun launchAudioSettings(s:DeckState) {
    val i=Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME,s.context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if(runCatching { s.context.startActivity(i) }.isFailure) {
        runCatching { s.context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
fun EqualizerScreen(s:DeckState) {
    ScreenShell("Equalizer",s) {
        Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.GraphicEq,null,Modifier.size(42.dp),tint=Amber)
            Text("M21 audio equalizer",style=MaterialTheme.typography.titleLarge)
            Text("Open the audio effects panel exposed by your device. If the FiiO system-wide PEQ isn't exposed as an Android audio effect, use Settings → Audio → Equalizer on the M21.",color=Dim)
            Button(onClick={launchAudioSettings(s)}) { Text("Open audio equalizer / sound settings") }
            Text("We deliberately avoid silently applying a second EQ on top of FiiO's global PEQ. Direct PEQ band editing requires device testing and is not included in this build.",color=Dim,fontSize=12.sp)
        }
    }
}

@Composable
fun SettingsScreen(s:DeckState) {
    val context=s.context
    ScreenShell("Launcher Settings",s) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Appearance & feel",color=Amber,fontWeight=FontWeight.Bold)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text("Haptic dial feedback");Text("Vibrate when interacting with controls",color=Dim,fontSize=12.sp)}
                Switch(checked=s.haptics,onCheckedChange=s::updateHaptics)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text("Compact home layout");Text("Smaller album art for the M21 screen",color=Dim,fontSize=12.sp)}
                Switch(checked=s.compact,onCheckedChange=s::updateCompact)
            }
            Text("Dial sensitivity",fontSize=14.sp)
            Slider(value=s.dialSensitivity,onValueChange=s::updateSensitivity,valueRange=0.5f..2f)
            HorizontalDivider()
            Text("Local music",color=Amber,fontWeight=FontWeight.Bold)
            Button(onClick={s.refreshLibrary()}) {Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Refresh internal + microSD library")}
            OutlinedButton(onClick={s.page=Page.EQ}) {Icon(Icons.Default.GraphicEq,null);Spacer(Modifier.width(8.dp));Text("Audio / EQ settings")}
            HorizontalDivider()
            Text("Android integration",color=Amber,fontWeight=FontWeight.Bold)
            Button(onClick={runCatching{context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}) {
                Text("Choose default home launcher")
            }
            Text("Switch back to the original FiiO home screen any time in Android's Default Home app settings.",color=Dim,fontSize=12.sp)
            HorizontalDivider()
            Text("About",color=Amber,fontWeight=FontWeight.Bold)
            Text("M21 Control Deck · Combined Batch 1 + 2\nAndroid Mode · local-first · no servers\nPrototype v0.1.0",color=Dim,fontSize=12.sp)
            Text("Bit-perfect / native DSD output, direct FiiO PEQ editing, and custom HOLD button mapping are not verified and not claimed.",color=Dim,fontSize=12.sp)
        }
    }
}

@Composable
fun AppsScreen(s:DeckState) {
    val context=s.context
    val launchIntent=remember { Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER) }
    val installed=remember { context.packageManager.queryIntentActivities(launchIntent,0)
        .filter { it.activityInfo.packageName!=context.packageName }
        .sortedBy { it.loadLabel(context.packageManager).toString().lowercase() } }
    var search by remember {mutableStateOf("")}
    ScreenShell("Apps",s) {
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search installed apps")},singleLine=true,
            modifier=Modifier.fillMaxWidth().padding(12.dp),leadingIcon={Icon(Icons.Default.Search,null)})
        LazyColumn {
            items(installed.filter {it.loadLabel(context.packageManager).toString().contains(search,true)}) { app ->
                val label=app.loadLabel(context.packageManager).toString()
                ListItem(headlineContent={Text(label)},leadingContent={Icon(Icons.Default.Apps,null,tint=Amber)},
                    modifier=Modifier.clickable {
                        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                            .setClassName(app.activityInfo.packageName,app.activityInfo.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching {context.startActivity(intent)}
                    })
                HorizontalDivider(color=Color(0xFF25262A))
            }
        }
    }
}

@Composable
fun QuickDeckScreen(s:DeckState) {
    val context=s.context
    ScreenShell("FiiO Quick Deck",s) {
        Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("HARDWARE-STYLE SHORTCUTS",fontSize=11.sp,color=Amber)
            Button(onClick={s.page=Page.APPS},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Apps,null); Spacer(Modifier.width(10.dp));Text("All installed apps")
            }
            OutlinedButton(onClick={s.page=Page.LIBRARY},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.LibraryMusic,null); Spacer(Modifier.width(10.dp));Text("Music library")
            }
            OutlinedButton(onClick={s.page=Page.QUEUE},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QueueMusic,null); Spacer(Modifier.width(10.dp));Text("Playback queue")
            }
            OutlinedButton(onClick={s.page=Page.EQ},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.GraphicEq,null); Spacer(Modifier.width(10.dp));Text("Audio equalizer")
            }
            OutlinedButton(onClick={runCatching {context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}},
                modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Bluetooth,null); Spacer(Modifier.width(10.dp));Text("Bluetooth settings")
            }
            OutlinedButton(onClick={runCatching {context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}},
                modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Brightness6,null); Spacer(Modifier.width(10.dp));Text("Display settings")
            }
            Text("This menu opens when you tap the orange FiiO button. No server, login, or subscription.",
                color=Dim,fontSize=12.sp)
        }
    }
}
