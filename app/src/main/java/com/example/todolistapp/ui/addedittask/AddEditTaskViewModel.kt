package com.example.todolistapp.ui.addedittask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolistapp.data.Task
import com.example.todolistapp.data.TaskDao
import com.example.todolistapp.ui.ADD_TASK_RESULT_OK
import com.example.todolistapp.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val state: SavedStateHandle //dagger would inject it in here as we inject our view model
    //into our AddEditTaskFragment, the saved state handle will
    //automatically contain the values of the arguments that we sent to this fragment
) : ViewModel() {
    //Now we can retrieve our navigation argument in the fragment, and send it over to the viewModel,
    //but we can also retrieve navigation arguments directly inside the view model. Through the saveStateHandle
    //SavedStateHandle is we can read the savedStateInstance in the fragment pass it here, so that's why it's called
    //the savedStateHandle, it is used to survive process death

    val task =
        state.get<Task>("task")//"task" is the same name as declared in the arguments in the nav graph

    var taskName = state.get<String>("taskName") ?: task?.name ?: ""
        //get the value from task, but task can be null as well so we use safe call operator
        //as we don't send the task over. If every things is null we send an empty string
        //Now we also have to store the taskName in the savedInstanceState, for that we override the setter method.
        set(value) {
            field = value
            state["taskName"] = value //value stored in savedStateHandle
        }

    var taskImportance = state.get<Boolean>("taskImportance") ?: task?.isImportant ?: false
        //get the value from task, but task can be null as well so we use safe call operator
        //as we don't send the task over. If every things is null we send an empty string
        //Now we also have to store the taskName in the savedInstanceState, for that we override the setter method.
        set(value) {
            field = value
            state["taskImportance"] = value //value stored in savedStateHandle
        }

    private val addEditTaskEventChannel = Channel<AddEditTaskEvent>()
    //we turn it into a flow
    val addEditTaskEvent = addEditTaskEventChannel.receiveAsFlow()

    fun onSaveClick() {
        if (taskName.isBlank()) {
            showInvalidInputMessage("Name cannot be empty")
            return //without it the methods below would execute and an empty task will also be added
        }

        if (task != null) {
            //edit case
            //copied it as task is immutable
            val updatedTask = task.copy(name = taskName, isImportant = taskImportance)
            updatedTask(updatedTask)
        } else {
            //add case
            val newTask = Task(name = taskName, isImportant = taskImportance)
            createTask(newTask)
        }
    }

    //If you have used startActivityForResult() before, there we have two default flags.
    //RESULT_CANCEL = 0, and RESULT_OK = -1. So it's good convention not to use these values
    //so we avoid any clashes between flags. So, we will create additional constants for successful add
    //and successful edit situation. We will put the constants in the main activity. 1) because that's where
    //the default flags are RESULT_CANCEL and RESULT_OK. 2) Also we want to use flags between different screens.
    //But we can have them anywhere

    private fun updatedTask(task: Task) = viewModelScope.launch {
        taskDao.update(task)
        addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(EDIT_TASK_RESULT_OK))
    }

    private fun createTask(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
        addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(ADD_TASK_RESULT_OK))
    }

    private fun showInvalidInputMessage(text: String) = viewModelScope.launch {
        //coroutine so we can launch an event through our channel
        addEditTaskEventChannel.send(AddEditTaskEvent.ShowInvalidInputMessage(text))
    }

    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()
    }

}


