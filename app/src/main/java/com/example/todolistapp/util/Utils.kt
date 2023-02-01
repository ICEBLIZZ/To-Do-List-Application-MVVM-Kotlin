package com.example.todolistapp.util

//It will just contain general utility functions, which will only be one in our app

//We made this generic method to convert our when statement into an exhaustive type,
//for compile time safety, which means making a when statement an expression
//e.g, something = when(){}

//This is an extension property that we can use an extension function that we can use on any type
//all this function does is return the same object. It basically doesn't do anything but it can
//turn a value into an expression. But now when statement used will sealed classes is already exhaustive
//so, we won't be needing the method below

val <T> T.exhaustive: T
    get() = this