package com.example.todolistapp.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.todolistapp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Navigation component is part of the android jetpack library, and it makes
        //it easier to navigate between different fragments of our app. It handles
        //the transitions, back stack etc, automatically

        //First piece of the navigation component is that we need to use the navGraph
        //component. It is an xml file that contains different destinations of our app,
        //arguments that we want to send over, animations and so on.

        //Jetpack Datastore: It is used to save the states of our search filters or data persistence.
        //Why do we need to use another library, why not do all that by Room. Because SQLite which Room uses
        //is meant for large amount of structured data that we want to query. So for small sets of data
        //such as saving UI states, we use data store. In the past we would have used shared preferences for this.
        //SharedPreferences is about to be deprecated because it have several problems. The biggest problem
        //is that it runs on the UI thread. And we should do operations of reading and writing from the UI
        //thread as it can cause UI to freeze. Whereas Jetpack Datastore runs on the background thread by using
        //flow

        //Navigation Component: The way we distinguish between an add or edit situation is either we send an entire
        //object over when we want to edit one or we don't send an object we create a new one.
        //We select the fragment in our nav_graph and click on add argument, give it a name "task" in this case,
        //and select a type which is in this case "Custom Parcelable", which are our own classes that implement
        //the parcelable interface. And to make this value optional we will check the nullable, and
        //we will set the default value to null. This means when we later click the add task button, we don't
        //have to send a task over at all, in this case it'll be null, we then check for this null value in the
        //AddEditTaskFragment, and this will see that we have a new task situation.
        //In the edit task situation we have to populate the fragment, and in the add task situation these fields
        //are empty
        //There are two places in which we can get the handle of the argument we send over to. One is the fragment
        //and other is the view model, but we'll use the view model as all the business logic lies there. We create
        //a separate view model class for that
        //Note: Do compile your project because navigation component have to generate some code for the navigation events

        // For some backend reasons we can't use findNavController() method in the onCreate() method,
        //it'd result in a crash so, we can use this function differently, as done below

        //This is the setup that we have to do if we want to access the nav controller in activities
        //we are getting a reference to the nav host fragment
        //it is the fragment container view that we put into our main activity layout
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.findNavController()

        //Now Nav controller will handle the functionality of the back button
        setupActionBarWithNavController(navController)

        //now we would get the fragment labels such as fragment_add_edit_task, etc on each fragment,
        //that's because we connected the action bar with the navigation component it automatically
        //populates the tool bar text with the name of these fragments
        //To change this go to the nav graph, and select the fragments and change the label up on the right

        //for our add edit fragment we will populate the tool bar text with the value we sent over.
        //for that add an argument title in the AddEditTaskFragment in the nav-graph, this time
        //don't make it null as we want to send the title over. Then in the nav_graph.xml in the
        //xml code of addEditTaskFragment add( android:label="{title}" ). Now head over to the TasksFragment
        //to handle the operation

    }

    override fun onSupportNavigateUp(): Boolean {
        //this will take care when we press up, we go back in the back stack
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

const val ADD_TASK_RESULT_OK = Activity.RESULT_FIRST_USER
//Activity.RESULT_FIRST_USER has a value of 1. This value means that it is the first value that we are free
//to use ourselves to avoid clashing with the system values
const val EDIT_TASK_RESULT_OK = Activity.RESULT_FIRST_USER + 1