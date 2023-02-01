package com.example.todolistapp.ui.deleteAllCompleted

import androidx.lifecycle.ViewModel
import com.example.todolistapp.data.TaskDao
import com.example.todolistapp.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAllCompletedViewModel @Inject constructor(
    private val taskDao: TaskDao,
    @ApplicationScope private val applicationScope: CoroutineScope //coroutine scope we created
): ViewModel() {
    //When we click a button in a dialog it is dismissed automatically, and immediately. Not after
    //we call something, it happens automatically. But operations such as deleting items from a database
    //can take a few milliseconds. So what would happen if we launch this operation in the view model scope,
    //the fragment dialog is dismissed, the view model gets removed from memory together with it because the
    //fragment is over, and the view model scope is actually cancelled. So if we launch this scope instead it
    //could happen that our delete operation is cancelled somewhere in the middle, this is why we need a larger
    //scope, and this is why we use our application scope for again, because this one is not cancelled when
    //the view model is removed from memory.

    fun onConfirmClick() = applicationScope.launch {
        taskDao.deleteCompletedTasks()
    }

}