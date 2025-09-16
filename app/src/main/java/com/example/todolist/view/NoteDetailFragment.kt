package com.example.todolist.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.todolist.R
import com.example.todolist.data.AppDataBase
import com.example.todolist.data.TaskRepository
import com.example.todolist.databinding.FragmentNoteDetailBinding
import com.example.todolist.domain.Task
import com.google.android.material.datepicker.MaterialDatePicker
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
    private lateinit var binding: FragmentNoteDetailBinding
    private lateinit var viewModel: TaskViewModel
    private var taskId = -1
    private lateinit var alarmManager: AlarmManager
    private var selectedDate: Long = 0

    // Регистрация для запроса разрешений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Разрешение предоставлено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Разрешение необходимо для уведомлений", Toast.LENGTH_SHORT).show()
        }
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

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        viewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        arguments?.let {
            taskId = it.getInt("taskId", -1)
        }

        // Запрос разрешений при создании фрагмента
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupToolBar()
        setupButtons()
        loadTaskIfExists()
    }

    private fun setupToolBar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveTask()
        }

        binding.setAlarmButton.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = selection
            showTimePicker()
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Выберите время")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)

                // Если выбранное время уже прошло, устанавливаем на следующий день
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            setAlarm(calendar.timeInMillis)
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun setAlarm(timeInMillis: Long) {
        val title = binding.titleEditText.text.toString()
        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Сначала введите заголовок", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("task_title", title)
            putExtra("task_description", binding.contentEditText.text.toString())
            putExtra("task_id", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            taskId.takeIf { it != -1 } ?: System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )

            val date = Calendar.getInstance().apply {
                setTimeInMillis(timeInMillis)
            }

            val dateStr = "${date.get(Calendar.DAY_OF_MONTH)}.${date.get(Calendar.MONTH) + 1}.${date.get(Calendar.YEAR)}"
            val timeStr = "${date.get(Calendar.HOUR_OF_DAY)}:${date.get(Calendar.MINUTE).toString().padStart(2, '0')}"

            Toast.makeText(
                requireContext(),
                "Напоминание установлено на $dateStr в $timeStr",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Ошибка: нет разрешения на установку напоминаний",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadTaskIfExists() {
        if (taskId != -1) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getTaskById(taskId).collect { task ->
                    task?.let {
                        binding.titleEditText.setText(it.name)
                        binding.contentEditText.setText(it.description)
                        binding.toolbar.title = "Редактирование"
                    }
                }
            }
        }
    }

    private fun saveTask() {
        val title = binding.titleEditText.text.toString()
        val description = binding.contentEditText.text.toString()

        if (title.isBlank()) {
            binding.titleEditText.error = "Введите заголовок"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (taskId != -1) {
                // Редактирование существующей задачи
                val task = Task(id = taskId, name = title, description = description)
                viewModel.updateTask(task)
            } else {
                // Создание новой задачи - НЕ передаем id (автогенерация)
                val task = Task(name = title, description = description)
                viewModel.addTask(task)
            }
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        fun newInstance(taskId: Int = -1): NoteDetailFragment =
            NoteDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("taskId", taskId)
                }
            }
    }
}