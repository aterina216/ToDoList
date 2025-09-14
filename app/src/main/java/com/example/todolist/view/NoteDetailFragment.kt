package com.example.todolist.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.todolist.R
import com.example.todolist.databinding.FragmentNoteDetailBinding
import com.example.todolist.domain.Task
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Calendar

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NoteDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NoteDetailFragment : Fragment() {
    // TODO: Rename and change types of parameters


    private lateinit var binding: FragmentNoteDetailBinding
    private lateinit var viewModel: TaskViewModel

    var taskId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNoteDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        arguments?.let {
            taskId = it.getInt("taskId", -1)
        }

        setupToolBar()
        setupButtons()
        loadTaskIfExists()
    }

    private fun setupToolBar(){
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupButtons(){
        binding.saveButton.setOnClickListener {
            saveTask()
        }

        binding.setAlarmButton.setOnClickListener {
            showAlarmDialog()
        }
    }

    private fun showAlarmDialog(){
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Выберите время напоминания")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
            }
            setAlarm(calendar.timeInMillis)
        }
        timePicker.show(parentFragmentManager, "time_picker")
    }

    private fun setAlarm(timeInMillis: Long){
        val title = binding.titleEditText.text.toString()
        if(title.isBlank()){
            Toast.makeText(requireContext(), "Сначала введите заголовок", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(), "Напоминание установлено", Toast.LENGTH_SHORT).show()
    }

    private fun loadTaskIfExists(){
        if(taskId != -1){
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getTaskById(taskId).collect {
                    task ->
                    task?.let {
                        binding.titleEditText.setText(it.name)
                        binding.contentEditText.setText(it.description)
                        binding.toolbar.title = "Редактирование"
                    }
                }
            }
        }
    }

    private fun saveTask(){
        val title = binding.titleEditText.text.toString()
        val description = binding.contentEditText.text.toString()

        if (title.isBlank()){
            binding.titleEditText.error = "Введите заголовок"
            return
        }

        val task = if(taskId != -1){
            Task(id = taskId, name = title, description = description)
        }
        else{
            Task(id, name = title, description = description)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if(taskId!= -1){
                viewModel.updateTask(task)
            }
            else{
                viewModel.addTask(task)
            }
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NoteDetailFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(taskId: Int = -1): NoteDetailFragment =
            NoteDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("taskId", taskId)
                }
            }
    }
}