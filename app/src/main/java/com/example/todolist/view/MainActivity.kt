package com.example.todolist.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.data.AppDataBase
import com.example.todolist.data.TaskRepository
import com.example.todolist.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Objects

// Замените на ваш package



private const val REQUEST_CODE_NOTIFICATIONS = 1001
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter
    private lateinit var binding: ActivityMainBinding // Добавляем binding как поле класса


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()

        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализируем binding
        setContentView(binding.root)

        adapter = TaskAdapter { task ->
            val fragment = NoteDetailFragment.newInstance(task.id)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val taskDao = AppDataBase.getDatabase(applicationContext).taskDao()
        val repository = TaskRepository(taskDao)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return TaskViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        )[TaskViewModel::class.java]

        // --- Добавляем логику свайпа для удаления ---
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, // Мы не хотим перетаскивать элементы (drag & drop), поэтому 0
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Разрешаем свайп влево и вправо
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Возвращаем false, так как не поддерживаем перетаскивание
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val taskToDelete = adapter.taskList[position]
                    viewModel.deleteTask(taskToDelete)
                    // После вызова deleteTask, Flow в ViewModel обновится,
                    // и наш collector в lifecycleScope.launch { ... }
                    // вызовет adapter.updateTasks, который обновит RecyclerView.
                    // Нет необходимости вручную вызывать adapter.notifyItemRemoved(position),
                    // так как updateTasks полностью обновляет список.
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        // --- Конец логики свайпа ---

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.allTasks.collect { tasks ->
                    adapter.updateTasks(tasks)
                }
            }
        }

        binding.addButton.setOnClickListener {
            val fragment = NoteDetailFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний о задачах"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
