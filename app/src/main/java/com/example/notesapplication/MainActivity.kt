package com.example.notesapplication

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.notesapplication.ui.theme.NotesApplicationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesApplicationTheme {
                Nav()
            }
        }
    }
}

@Composable
fun Nav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "TheScreen") {
        composable(
            route = "TheScreen"
        ) {
            TheScreen(navController)
        }
        composable(
            route = "DetailScreen/{theTopic}/{theContent}",
            arguments = listOf(
                navArgument(name = "theTopic") {
                    type = NavType.StringType
                },
                navArgument(name = "theContent") {
                    type = NavType.StringType
                }
            )) { backstackEntry ->
            val topic = backstackEntry.arguments?.getString("theTopic")
            val content = backstackEntry.arguments?.getString("theContent")
            ContactDetail(
                navController,topic, content
            )
        }
    }
}
@Entity(tableName = "NotesTable")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val content: String
)

@Dao
interface NoteDao{
    @Query("SELECT * FROM NotesTable ORDER BY topic ASC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

@Database(entities = [Note::class], version = 1)
abstract class NoteDatabase: RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

class NoteRepository(private val noteDao:NoteDao){
    fun getAllNotes():Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(note:Note) {
        noteDao.deleteNote(note)
    }

}

class TheViewModel(private val Repo: NoteRepository): ViewModel() {
    val allNotes: Flow<List<Note>> = Repo.getAllNotes()

    fun insert(note: Note) {
        if (note.topic.isNotBlank() && note.content.isNotBlank()) {
            viewModelScope.launch {
                Repo.insertNote(note)
            }
        }
    }

    fun delete(note:Note) = viewModelScope.launch {
        Repo.deleteNote(note)
    }
}

@Composable
fun TheScreen(navController: NavController, theViewModel:TheViewModel = koinViewModel()){
    val notes by theViewModel.allNotes.collectAsState(initial = emptyList())

    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        OutlinedTextField(
            value = topic,
            onValueChange = {topic = it},
            label = {Text("Enter the title: ")},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = {content = it},
            label = {Text("Enter the content: ")},
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,   // Default elevation
                pressedElevation = 12.dp,  // Elevation when pressed
                hoveredElevation = 4.dp,   // Elevation when hovered
                focusedElevation = 6.dp    // Elevation when focused
            ),
            onClick = {
                theViewModel.insert(Note(
                    topic = topic,
                    content = content)
                )
                topic = ""
                content = ""
            },
            modifier = Modifier.padding(top = 16.dp)) {
            Text("Add Note")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(notes) {
                    note ->
                NoteItem(navController,note = note, onDeleteClick = {theViewModel.delete(note)})
            }

        }

    }

}

@Composable
fun NoteItem(navController: NavController, note:Note, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                navController.navigate("DetailScreen/${note.topic}/${note.content}")
            },
        horizontalArrangement= Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = note.topic)
        //Text(text = note.content)
        IconButton(onClick = {onDeleteClick()}) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Note")
        }
    }

}

@Composable
fun ContactDetail(navController: NavController, topic: String?, content: String?){
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Topic: $topic",fontSize = 20.sp)
        Spacer(modifier = Modifier.height(25.dp))
        Text(text = "Content: $content", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(25.dp))
    }
    Button(
        modifier = Modifier.absoluteOffset(x = 10.dp, y = 800.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,   // Default elevation
            pressedElevation = 12.dp,  // Elevation when pressed
            hoveredElevation = 4.dp,   // Elevation when hovered
            focusedElevation = 6.dp    // Elevation when focused
        ),
        onClick = {
            navController.navigate("TheScreen")
        }
    ) {
        Text(text = "Back to Notes")
    }

}
class TheApp: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TheApp)
            modules(appModule)
        }
    }

}