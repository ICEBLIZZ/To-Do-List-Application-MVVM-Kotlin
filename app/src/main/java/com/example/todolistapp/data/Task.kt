package com.example.todolistapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat


//We made this a data class as we'd be needing the equals method to compare objects
//in our recycler view
@Entity(tableName = "task_table")
@Parcelize //add this plugin id 'kotlin-parcelize'
data class Task(
    val name: String,
    val isImportant: Boolean = false,
    val isCompleted: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0 //this will change as we create objects
    ) : Parcelable {
    //We should make these properties immutable by using val instead of var, and later if we
    //need to update a task, instead of changing the existing task object, we will completely
    //create a new one and use this one to update our database. This way we can get hard to find
    //bugs, because our app might not recognize the changes if we modify an existing item. This
    //is just how the comparison works

    val createdDateFormatted: String
    //whenever we call Task.dataCreated this line would be executed
    get() = DateFormat.getDateInstance().format(dateCreated)
    //Note: we don't need to pass the creation date it'd be automatically generated here

    //Imp: We will make the class "parcelable". Parcelable is a special interface which makes it
    //possible to send this object between different fragments. Because otherwise we'd have to send
    //each property separately
}