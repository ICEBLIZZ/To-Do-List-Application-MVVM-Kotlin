package com.example.todolistapp.ui.deleteAllCompleted

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteAllCompletedDialogFragment : DialogFragment() {
    //*We won't create a layout file for this, as dialogs can be used with default layouts
    //*We will add this dialog fragment to the navGraph instead of a fragment
    //*Benefit is we will get a re-usable dialog
    //*It will have it's own view model so it needs @AndroidEntryPoint annotation

    private val viewModel: DeleteAllCompletedViewModel by viewModels()

    //Here we can return a dialog from this method that will be used as the dialog from this dialog fragment
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Do you really want to delete all completed tasks")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes") { _, _, ->
                viewModel.onConfirmClick()
            }
            .create()

    //In the navGraph we will drag and drop the dialog fragment. Instead of dragging an arrow from
    //one fragment to the other, we wil give it a global action, right click on it -> Add Action -> global action.
    //We did this to make the action re-usable, now we can use this dialog fragment from anywhere
}