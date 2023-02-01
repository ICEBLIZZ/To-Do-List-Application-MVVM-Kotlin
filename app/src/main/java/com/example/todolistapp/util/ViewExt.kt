package com.example.todolistapp.util

import androidx.appcompat.widget.SearchView

//We don't need a class here we just need extension function

//Here we can write view extension functions, we use it in case when we only want one
//override method and not a bunch which gets implemented automatically

//We created an extension function in SearchView class named "onQueryTextChanged", by which we'd
//get an extra function in SearchView

//The inline keyword makes the code more efficient, it's not important in terms of functionality.
//Without this inline keyword what happens is, when we pass such a function parameter, the Kotlin code
//actually generates a separate object for it and this creates some runtime overhead.
//With this keyword it takes this code that we later defined in the lambda argument, and copies it
//directly in the onQueryTextChange() function, its just more efficient.

//cross inline is important without it listener() would give an error

inline fun SearchView.onQueryTextChanged(crossinline listener : (String) -> Unit) {
    this.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
        override fun onQueryTextSubmit(query: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            listener(newText.orEmpty()) //orEmpty() will return an empty string in null case
            return true //it tells the system that we handled the event and we don't want it to be handled
                        //by the system
        }
    })
}