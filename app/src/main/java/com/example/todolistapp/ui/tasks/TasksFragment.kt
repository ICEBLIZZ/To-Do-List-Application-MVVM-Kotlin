package com.example.todolistapp.ui.tasks

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp.R
import com.example.todolistapp.data.SortOrder
import com.example.todolistapp.data.Task
import com.example.todolistapp.databinding.FragmentTasksBinding
import com.example.todolistapp.util.onQueryTextChanged
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint //Activities and fragments can't be constructor injected so we use this annotation
class TasksFragment : Fragment(R.layout.fragment_tasks), TasksAdapter.OnItemClickListener {
    //We add this first to the nav graph and the nav graph displays a home icon on to of it
    //it shows that this will be the first fragment displayed.

    //Here we used property delegation
    //@AndroidEntryPoint due to this annotation it will also be injected by dagger hilt
    private val viewModel: TasksViewModel by viewModels()

    private lateinit var searchView: SearchView //To resolve issue(search query lost on screen rotation)

    @SuppressLint("ShowToast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //As our layout is already inflated
        val binding = FragmentTasksBinding.bind(view)

        val tasksAdapter = TasksAdapter(this)//this as the fragment implements the interface

        binding.apply {
            recyclerViewTasks.apply {
                //access recycler view methods
                adapter = tasksAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)//optimization method if we know recycler view doesn't change its
                //position on the screen
            }

            //adding the swiping functionality
            ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ){
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = tasksAdapter.currentList[viewHolder.adapterPosition]
                    //business logic would be handled in the view model
                    viewModel.onTaskSwiped(task)
                }

            }).attachToRecyclerView(recyclerViewTasks)

            //floating action button
            fabAddTask.setOnClickListener {
                //we can do the navigation transition here but that should be done by the view model
                //separation of concerns is important as view models can be unit tested, and we won't need
                //these long lasting ui tests for fragments and activities
                viewModel.onAddNewTaskClick()
            }
        }

        //Here we will handle the results from AddEditTaskFragment
        setFragmentResultListener("add_edit_request") { _, bundle ->
            //executed when we receive our result
            val result = bundle.getInt("add_edit_result")
            //Now we need to show the snack bar, but view model should handle this event
            viewModel.onAddEditResult(result)
        }

        //Live data uses the lifecycle owner to figure out if it's active or not. And for fragments
        //we mostly always pass the viewLifecycleOwner. Because when we move to another fragment
        //and the fragment get added to the backstack, the fragment instance stays alive it stays in memory.
        //but the view hierarchy gets destroyed. So if we don't have a view we don't have recycler view or
        //any other view on the screen.So, we don't need updates. In fact if we'd get updates if the view hierarchy
        //is not there, we'd actually get crashes because the references are not valid anymore

        //When lambda is the last argument we use the trailing lambda syntax in kotlin as shown below
        viewModel.tasks.observe(viewLifecycleOwner) {
            //Now this is the observe method, where we get it: List<Task>, it is just a name
            tasksAdapter.submitList(it)
        }

        setupMenu()

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            //launchWhenStarted it limits the scope even more, this coroutine will be cancelled when
            //onStop() was called, and restarted on onStart()
            viewModel.tasksEvent.collect {event ->
                when(event){
                    //we'd get an error if we miss any event, just like enums sealed classes have this property
                    is TasksViewModel.TasksEvent.ShowUndoDeleteTaskMessage ->{
                        //View model tells the fragment to show undo delete snackBar
                        Snackbar.make(requireView(), "Task Deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO") {
                                //We have to put back item into the database, but the fragment does not
                                //make this decision, the view model does.
                                viewModel.onUndoDeleteClick(event.task)//task sent to put back in the database
                            }.show()
                    }

                    is TasksViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                        //The method below is generate by the NavigationComponent SavArgs Plugin,
                        //Do compile the project before using this method, else it won't show up
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment("New Task", null)
                        //now we pass the action to execute this navigation event and navigate to the
                        //other screen
                        findNavController().navigate(action)
                        //we can also pass R.id.AddEditTaskFragment instead of action, but that way
                        //we won't have compile time safety
                    }

                    is TasksViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment("Edit Task", event.task)
                        findNavController().navigate(action)
                    }

                    is TasksViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {
                        //show snack bar
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }

                    TasksViewModel.TasksEvent.NavigateToDeleteAllCompletedScreen -> {
                        val action = TasksFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment()
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    override fun onItemClick(task: Task) {
        //We would pass these values received from the adapter to the view model as all
        //the business logic should go there
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChanged(task, isChecked)
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_fragment_tasks, menu)

                val searchItem = menu.findItem(R.id.action_search)
                //The reason why we made search view a property of this class is when the view model is destroyed
                //the searchView by default sends an empty string as input, which makes the searchQuery value in
                //our view model empty
                searchView = searchItem.actionView as SearchView

                //when we inflate the options menu in onCreateMenu(), we want to restore the previous query
                //if there is one existing
                //By this code now on screen rotation search query won't be lost
                val pendingQuery = viewModel.searchQuery.value
                if(pendingQuery != null && pendingQuery.isNotEmpty()) {
                    searchItem.expandActionView()//expands the magnifying glass into the whole expanded search view widget
                    searchView.setQuery(pendingQuery, false)
                }

                searchView.onQueryTextChanged {
                    //"it" is the text we type in the search bar
                    viewModel.searchQuery.value = it
                }

                //here we want to read the current hide completed state from the preferences flow
                viewLifecycleOwner.lifecycleScope.launch {
                    //launched a coroutine scope
                    menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                        viewModel.preferencesFlow.first().hideCompleted

                //normally we'd use viewModel.preferencesFlow.collect(), so whenever a new value from the
                //flow emits this flow block will be executed again. But we only want to read from it
                //once because after we started our app we don't need updates from this checkbox anymore
                //as it will be automatically updated when we click it. So, instead of collect we use first()
                //this will only read a single value from the flow and then cancel it, and the flow will
                //just be cancelled inside this coroutine here.
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                //Validate and handle the selected menu item
                return when (menuItem.itemId) {
                    R.id.action_sort_by_name -> {
                        //viewModel.sortOrder.value = SortOrder.BY_NAME
                        //value sent to view model, it would decide what would do as all the business logic
                        //goes there
                        viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                        true //returns true when we have return with when statement
                    }
                    R.id.action_sort_by_date_created -> {
                        //viewModel.sortOrder.value = SortOrder.BY_DATE
                        viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                        true
                    }
                    R.id.action_hide_completed_tasks -> {
                        menuItem.isChecked = !menuItem.isChecked
                        //viewModel.hideCompleted.value = menuItem.isChecked
                        viewModel.onHideCompletedClick(menuItem.isChecked)
                        true
                    }
                    R.id.action_delete_all_completed_tasks -> {
                        viewModel.onDeleteAllCompletedClick()
                        true
                    }
                    else -> false //this means that we didn't handled the click
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        //called when view of the fragment is destroyed
        super.onDestroyView()
        //It will remove the listener, so now we don't send the unnecessary empty string
        searchView.setOnQueryTextListener(null)
    }

}