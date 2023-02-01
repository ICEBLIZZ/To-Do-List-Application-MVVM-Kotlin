package com.example.todolistapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "PreferencesManager"

enum class SortOrder { BY_NAME, BY_DATE }

//We can't return two values from preferencesFlow so we use this wrapper class to do the trick.
//This function would act as a return type of preferenceFlow
data class FilterPreferences(val  sortOrder: SortOrder, val hideCompleted: Boolean)

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    //We separate data persistence code from the view model.
    //PreferencesManager belong to the whole application.

    //"dataStore" name is defined by us
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")
    private val dataStore: DataStore<Preferences> = context.dataStore

    //retrieving data
    val preferencesFlow = dataStore.data
        //Here we don't need a suspending function as its a flow and flow runs in a coroutine
        .catch { exception ->
            if(exception is IOException) {
                Log.e(TAG, "Error reading preferences: ", exception)
                //IOException means something went wrong while reading the data.
                emit(emptyPreferences()) //what this does is, cause the map function to use the
                                         //default values, in case of an exception
            } else {
                //we don't want to swallow the exception, we want to throw it in else case
                //to observe the fault
                throw exception
            }
        }
            //We used the map function to convert the normal preferences into FilterPreferences which
            //are much easier to use in viewModel, because we don't have to write all of this code
            //in the view model it's already written here
        .map {preferences ->
            //here we can take each value that comes through this flow, and transform it in some way
            //to turn it into another value, and preferencesFlow will then return this other value
            //instead.

            //instead of using the 'it' variable we can rename it to preferences or whatever as shown above

            //we use valueOf to convert the sort order back to enum from String
            val sortOrder =  SortOrder.valueOf( //SortOrder.BY_DATE.name this is the default value in case of null
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name //name would convert it to String
            )

            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED] ?: false

            //By this the return type of this function changed from flow of preferences to flow of
            //filtered preferences
            FilterPreferences(sortOrder, hideCompleted)
        }

    //We update the existing data here whenever we change the hide completed or sort order in the data store
    //saving or updating data in the datastore
    //Here we need a suspend function as it's an IO operation, it can take a moment.
    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name //converted enum to string
        }
    }

    suspend fun updateHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] = hideCompleted
        }
    }

    //this class is only to make code readable we can put it's content directly in here
    private object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
    }
}