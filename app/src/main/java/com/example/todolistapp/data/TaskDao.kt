package com.example.todolistapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    //with suspend modifier we convert this function into a suspending function
    //which belongs to the kotlin coroutine feature. Suspend is a way through which
    //we can let this function switch to a different thread. It's the easiest way
    //to use a background thread in Kotlin. Suspend method would suspend this function, on it's
    //thread, the rest of the UI would work normally, and when the suspended method is completed
    //the code below it would execute. We won't have to use a callback as we used in Java

    //Note: The suspend function can only be called from another suspend function or a Coroutine
    @Insert(onConflict = OnConflictStrategy.REPLACE)//in case of same id we'd just replace it
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    // '||' is the append operator in SQL. Data is sorted by importance and then by name
    // WHERE (isCompleted != :hideCompleted OR isCompleted = 0) -> this means if hideCompleted is true,
    // show all uncompleted tasks. If hide completed is false show all completed tasks.
    //When hideCompleted is false, we have to show all completed tasks along with all in completed tasks.
    //This is what "OR isCompleted = 0" this is for. '0' means false in SQLite. So it'll show the uncompleted
    //tasks in any case.
    @Query("SELECT * FROM task_table WHERE (isCompleted != :hideCompleted OR isCompleted = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY isImportant DESC, name")
    fun getTasksSortedByName(
        searchQuery: String,
        hideCompleted: Boolean
    ): Flow<List<Task>>//Flow from coroutine library

    @Query("SELECT * FROM task_table WHERE (isCompleted != :hideCompleted OR isCompleted = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY isImportant DESC, dateCreated")
    fun getTasksSortedByDateCreated(searchQuery: String, hideCompleted: Boolean): Flow<List<Task>>

    fun getTasks(query: String, sortOrder: SortOrder, hideCompleted: Boolean): Flow<List<Task>> =
        when (sortOrder) {
            SortOrder.BY_DATE -> getTasksSortedByDateCreated(query, hideCompleted)
            SortOrder.BY_NAME -> getTasksSortedByName(query, hideCompleted)
        }


    @Query("DELETE FROM task_table WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    //Flow will return a stream of tasks, it represents stream of data
    //Whenever the database table changes room will automatically put a list of tasks
    //into the flow, it's kind of like live Data
    //Flow can only be used or collected inside a coroutine. That's why we don't need
    //the suspend modifier here all the suspension happens inside the flow.
    //In short Flow is an asynchronous stream of data

    //Live data would work similar, but they have slight differences

    //Note: we can't pass a column name as a variable to the room query, because this would destroy the
    //compile time safety, because the column name could be invalid. For this reason we have
    //to create two separate functions, because we can't pass the sort order, and then use it
    //inside the query up here. Instead we have to create one function for sort by name and
    //one for sort by date created.

}