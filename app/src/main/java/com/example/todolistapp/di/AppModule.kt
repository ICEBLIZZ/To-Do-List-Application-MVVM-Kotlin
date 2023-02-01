package com.example.todolistapp.di

import android.app.Application
import androidx.room.Room
import com.example.todolistapp.data.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton
    //ApplicationComponent is renamed to SingletonComponent
    //This class is auto-generated by dagger. This object contains the
    //dependencies that we want to use throughout the application. It'd make
    //use of Application Context which we provide in provides method
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    //Module is simply a place where we give dagger the instructions on how to create the
    //dependencies we need
    //Here we will tell dagger how to create a TaskDataBase and a TaskDao, and later when we need
    //them somewhere dagger will provide them for us

    //here we provide code that constructs a database
    @Provides
    @Singleton //it'd create only one instance
    fun provideDatabase(//without dagger-hilt we would create another method for application
        app: Application, //hilt have a pre-defined feature of providing application context.
        callback: TaskDatabase.Callback//now dagger knows where can it get a call back.
    ) = Room.databaseBuilder(app, TaskDatabase::class.java, "task_database")//java as it uses java under the hood
            .fallbackToDestructiveMigration()
            //we normally add callback here to prefill our database, but this shouldn't be done in DI, so we do
            //it in the database class by creating a class there
            .addCallback(callback)
            .build()


    //When we need a task dao dagger will see this provides, and return the task dao. But to create a
    //taskDao dagger needs a taskDatabase object which it will get from the provides method above
    @Provides//it's also a singleton as dao is defined as a singleton by Room
    fun provideTaskDao(db: TaskDatabase) = db.taskDao()

    //we can later use the same scope to execute long running operations throughout our whole app. But by default
    //a coroutine gets cancelled when any of it's child fails. E.g we have two operations running at the same time
    //on our application scope and if one of them fails, then it'd cause the other one to get cancelled as well
    //because the whole scope cancels. To avoid this we have to add the Supervisor job. It tells the coroutine
    //when any of it's child fails keep the other children running
    @ApplicationScope //This will tell dagger that this is not just any coroutine scope but the application scope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())
}

//Here we created the ApplicationScope annotation, by which we can tell dagger about different scopes in case
//we use other scopes
//This means that this qualifier would be visible for reflection.
//Retention is basically for if you want to switch the dependency injection framework
//Here we just used it as Qualifier annotation won't work without it
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope