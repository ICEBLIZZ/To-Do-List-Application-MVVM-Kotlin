package com.example.todolistapp.ui.addedittask

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.todolistapp.R
import com.example.todolistapp.databinding.FragmentAddEditTaskBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {
    private val viewModel: AddEditTaskViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddEditTaskBinding.bind(view)
        binding.apply {
            editTextTaskName.setText(viewModel.taskName) //we use setText() in case of an edit text
            checkBoxImportant.isChecked = viewModel.taskImportance
            //Without this line when we open a task, we'd see an animation of check-box checking
            checkBoxImportant.jumpDrawablesToCurrentState()
            //date only visible if we send a task over which is the edit case
            textViewDateCreated.isVisible = viewModel.task != null
            textViewDateCreated.text = "Created: ${viewModel.task?.createdDateFormatted}"

            //if text or importance changes these values would be sent to the view model
            editTextTaskName.addTextChangedListener {
                viewModel.taskName = it.toString()
            }

            //_ is used to ignore the field
            checkBoxImportant.setOnCheckedChangeListener { _, isChecked ->
                viewModel.taskImportance = isChecked
            }

            fabSaveTask.setOnClickListener {
                viewModel.onSaveClick()
            }

            //launchWhenStarted means we stop listening for the events when the app is in the background
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                //we access the event flow here
                viewModel.addEditTaskEvent.collect { event ->
                    when (event) {
                        is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                            //When we click save button to navigate back, we clear focus which will hide the
                            //keyboard on going back.
                            binding.editTextTaskName.clearFocus()
                            //Now we want to send the result back to the previous fragment to show the corresponding
                            //snack bar there. And for this we will use the fragment result API
                            setFragmentResult(
                                "add_edit_request", //to identify the request in the previous fragment
                                //we can only pass primitives or parcelables
                                bundleOf("add_edit_result" to event.result)//"to" is used to map key to values
                            )
                            findNavController().popBackStack() //to immediately remove this fragment from the back stack and go to prev one
                        }

                        is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> {
                            Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }

        }
    }
}