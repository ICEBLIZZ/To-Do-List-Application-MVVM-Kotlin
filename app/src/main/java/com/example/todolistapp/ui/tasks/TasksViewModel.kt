package com.example.todolistapp.ui.tasks

import androidx.lifecycle.*
import com.example.todolistapp.data.PreferencesManager
import com.example.todolistapp.data.SortOrder
import com.example.todolistapp.data.Task
import com.example.todolistapp.data.TaskDao
import com.example.todolistapp.ui.ADD_TASK_RESULT_OK
import com.example.todolistapp.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel //@ViewModelInject is deprecated so we use @Inject with @HiltViewModel annotation
class TasksViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle //to save the search query in the saved instance state
) : ViewModel() {//We have to inject the Task View Model class in the Tasks Fragment

    //it's like mutable live data which holds a single value not a stream of value but
    //we can use it as a flow
    //val searchQuery = MutableStateFlow("")

    //Note: we can't save a flow in the savedStateHandle, because flow is a language feature and savedInstanceState
    //is something that belongs to the frame work. But we can store live data into a flow
    //or vice versa.
    //The cool thing about using savedStateHandle with live data is that we don't have to handle the setter
    //method here, instead whenever we make changes to the search query it will automatically persist it inside
    //the savedStateHandle
    val searchQuery = state.getLiveData("searchQuery", "")


    //val sortOrder = MutableStateFlow(SortOrder.BY_DATE)
    //val hideCompleted = MutableStateFlow(false)
    //instead of upper two flows we can make a single flow with our defined PreferenceManager
    val preferencesFlow = preferencesManager.preferencesFlow

    //we can put TasksEvent type of data into this channel
    private val tasksEventChannel = Channel<TasksEvent>() //from kotlinx.coroutine

    //converted channel into flow so we can use it in our fragment to extract these single
    //values out of it
    val tasksEvent = tasksEventChannel.receiveAsFlow()

    //To translate searchQuery into a SQLite query we have to use the flow operator
    //private val tasksFlow = searchQuery.flatMapLatest {
    //Due to the flow operator whenever the value of searchQuery changes execute
    //this block of code.
    //Here we switched from one flow to another while observing it, same happens with live data
    //In live data its called switchMap
    //taskDao.getTasks(it)
    //}

    //this way we can combine multiple flows into a single flow
    private val tasksFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ) { query, filterPreferences ->
        //now whenever any of these flows emits a new value, we will get all three latest values
        //We can return only one value so somehow we have to combine all three of these values
        //for that we use triple class which is just a wrapper around three arbitrary values.
        //Triple(query, sortOrder, hideCompleted)
        Pair(query, filterPreferences)//same as triple but with two values
    }.flatMapLatest { (query, filterPreferences) ->
        //we can use Kotlin's destructuring declaration. Which basically means that we can split the
        //triple into three separate arguments
        //so instead of
        //taskDao.getTasks(it.first, it.second, it.third) //this way we can do something like
        taskDao.getTasks(query, filterPreferences.sortOrder, filterPreferences.hideCompleted)
    }

    val tasks = tasksFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        //it can only work in a coroutine or a suspend function
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    //functions having values from the TasksAdapter class
    fun onTaskSelected(task: Task) = viewModelScope.launch{
        //Now we will navigate to the AddEditTaskFragment. And we forward the task that was clicked
        tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        //update is a suspend function so we need a coroutine
        //we create a copy of the task as in Task table all the values are immutable(they are 'val')
        //isCompleted is updated rest values would be as are in the task variable
        taskDao.update(task.copy(isCompleted = isChecked)) //.copy() is due to data class
    }

    //to delete a task
    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        //here we will see how can a view model show a snack bar. As a snack bar
        //is associated with activity or fragments. We have to show a snack bar with keeping
        //separation of concerns in mind, which is that a view model should not contain
        //a reference to a fragment
        //In the past there has been attempts to turn normal live data into events or wrapper classes
        //to use view functions. But that wasn't good practice as live data isn't used to represent single
        //events, its used to represent a state.
        //If we use live data here, after screen rotation we would again see the snack bar

        //Here we will use another Kotlin feature called a "Channel", with a channel we can send data
        //between two coroutines
        //the view model will send events to the fragment, which would be the consumed by the fragment.
        //all of this happens without blocking the UI thread, as both fragment and the view model
        //use suspended functions.
        tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
        //Now on the fragment side we can wait on this channel until something arrives and show a snackBar.
        //But it's better not to expose it as a channel outside but as a flow, that why taskEventChannel is
        //private. Because when we expose the channel the fragment could put something into this channel.
        //We don't want this, we only want to be able to put something out, so it's better to not expose
        //this event object at all.
    }

    fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClick() = viewModelScope.launch {
        //we want to send an event through our channel which is only possible through a coroutine
        //This is how the view model layer tells the UI layer that what it has to do by events
        tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)
    }

    fun onAddEditResult(result: Int) {
        when(result) {
            ADD_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task Added")
            EDIT_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task Updated")
        }
    }

    private fun showTaskSavedConfirmationMessage(text: String) = viewModelScope.launch {
        //send it to the fragment to show snack bar
        tasksEventChannel.send(TasksEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun onDeleteAllCompletedClick() = viewModelScope.launch {
        tasksEventChannel.send(TasksEvent.NavigateToDeleteAllCompletedScreen)
    }

    //this sealed class will represent the different kinds of events that we want to be able to send it
    //to the fragment, this way we can distinguish between them without having to create a separate channel
    //for each of them.
    sealed class TasksEvent {
        //all events related to Tasks i.e TasksFragment, TasksViewModel etc
        //A sealed class is like an enum it can represent a closed combination of different values, but
        //as opposed to an enum these values can hold data, because those are instances of actual classes.

        //Instead of putting it into a sealed class instead of putting it somewhere in a top-level
        //class is because this way we can later get a warning, in our when statement where we can
        //check between these different kinds of events is not exhaustive, this is the main benefit
        //that we get from these sealed classes. Because the compiler know that there are not other
        //type of task events besides the ones that we have defined in here.
        // * We will send this event over a channel

        //Note: it doesn't have a snackBar in it's name, as view model doesn't know
        //what to display in a view, it just provide data to the fragment
        data class ShowUndoDeleteTaskMessage(val task: Task) : TasksEvent()

        //created it as an object as we do not want to pass any data, hence making the code efficient
        object NavigateToAddTaskScreen : TasksEvent()

        //data class as we want to send data
        data class NavigateToEditTaskScreen(val task: Task) : TasksEvent()

        //class to trigger events to show snack bar on add or edit
        data class ShowTaskSavedConfirmationMessage(val msg: String) : TasksEvent()

        object NavigateToDeleteAllCompletedScreen : TasksEvent()
    }

    //Note: A view model shouldn't have a reference to our activity or fragment, so we avoid
    //passing activity context here, as when the activity/fragments destroy that can result in
    //a memory leak. This is why we use reactive data sources like Flow here inside this view model
    //because the fragment can just observe this stream of data. And update itself when something
    //new comes through this stream. This way the view model doesn't need a reference to the fragment
    //that way it can just put this data there to observe

    //Difference between live data and flow is that live data always just has a single latest value, and
    //not the whole stream of values. The app would also work if we'd use flow all the way, but we use flow
    //being below the view model as flow is more flexible, it has different operators that we can use
    //to transform the data, we can switch thread in a flow. And we don't lose any values as it's a whole
    //stream and not just the latest value.
    //On the other hand live data has the benefit that it makes handling the life cycle of the fragment easier
    //because it is life cycle aware. As it waits if the app goes into the background, this prevents our app
    //to get memory leaks and crashes

    //So we often use flow below our view model and then turn our data into live data
}
