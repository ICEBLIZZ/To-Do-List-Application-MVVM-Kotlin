package com.example.todolistapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolistapp.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {//abstract as room would generate all the necessary data

    abstract fun taskDao(): TaskDao

    //here we will use dependency injections, as DI is used when a class depends on another class,
    //so, normally we manually pass the instance to that class, but by DI, the view model
    //won't be responsible to search the dao.
    //Dagger will hold our defined dependencies then dagger will inject it in the correct
    //places in correct time. Dagger 2 is complicated to use, so we will use Hilt as
    //it takes care of complicated stuff of dagger 2


    //Again we have to tell dagger how it can pass an instance of our callback and later pass it
    //to our database
    class Callback @Inject constructor(
        private val dataBase: Provider<TaskDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope //now dagger knows how to get it,
        //          we added the annotation as in case if we have other scopes the dagger won't know
        //          the Application scope annotation in the AppModule class
    ) : RoomDatabase.Callback() {
        //This @Inject annotation have two uses. 1) it tells dagger how it can create an instance of this class
        //                                       2) it tells dagger to pass the necessary dependencies if we pass
        //                                          something in the constructor()
        //Inject is used when we have class which doesn't belong to a 3rd party library, or it doesn't need
        //any additional configuration, like call methods on it, so, we can tell dagger to use
        //this instance whenever it needs it. Also if we don't have
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            //this would be called only the first time we create this database
            //db operations
            //we need our database instance to do database operation, here we have a situation of circular
            //dependencies, our TaskDatabase in AppModule.kt needs the callback dependency, and our callback
            //dependency needs a TaskDatabase

            //Note: onCreate() gets executed after the provideDatabase method's .build method is executed
            // from AppModule. So we use Provide<TaskDatabase> which allow us to use lazy dependencies,
            //that after that method is finished then onCreate will be called. As provideDatabase
            //will result in calling this onCreate method. Now dagger won't try to instantiate the database when
            //this callback is created. In short first providesDatabase() will get the callback and after that
            //this onCreate would be provided with a database instance by dagger
            val dao = dataBase.get().taskDao()

            //Note: Database operations can only be called from a suspend function or a coroutine.
            //A coroutine is like a light weight thread. A coroutine is a piece of code that actually knows
            //how to suspend execution, and let the program continue with something else.
            //A coroutine also needs a scope, e.g, if we want a coroutine when we are in a certain screen in
            //our app and when we leave that screen we want it to be cancelled automatically, which is not
            //the case here. Here we just want to execute it in the background

            //We can use the GlobalScope class of coroutine. It will keep it running as long as the app is
            //running, until the coroutine is finished. But it is considered as a bad practice as it gives
            //us less control over its configuration, and its harder to test. What recommended is to create
            //our own coroutine scope that lives in our application.

            //Again we want dagger to inject that coroutine scope and it provides us whenever we need it,
            //and we don't have to worry about it

            applicationScope.launch {
                dao.insert(Task("Wash the dishes"))
                dao.insert(Task("Do the laundry"))
                dao.insert(Task("Buy groceries"))
                dao.insert(Task("Prepare food", isImportant = true))
                dao.insert(Task("Call mom"))
                dao.insert(Task("Visit grandma", isCompleted = true))
                dao.insert(Task("Repair my Lamborghini", isCompleted = true))
                dao.insert(Task("Call Elon Musk"))
            }
        }
    }

}