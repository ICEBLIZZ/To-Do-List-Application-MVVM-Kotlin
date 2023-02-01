package com.example.todolistapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

//This is just a setup to activate dagger hilt basically
//We have to define it in the manifest
@HiltAndroidApp
class ToDoApplication : Application() {
}