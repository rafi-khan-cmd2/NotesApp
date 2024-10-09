package com.example.notesapplication

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module{

    single{
        Room.databaseBuilder(
            androidContext(),
            NoteDatabase::class.java,"noteDatabase"
        ).build()
    }

    single{get<NoteDatabase>().noteDao()}

    single{NoteRepository(get())}

    viewModel{TheViewModel(get())}

}