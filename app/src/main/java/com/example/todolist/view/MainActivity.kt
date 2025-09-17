package com.example.todolist.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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




private const val REQUEST_CODE_NOTIFICATIONS = 1001
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter
    private lateinit var binding: ActivityMainBinding // Добавляем binding как поле класса

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)

        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализируем binding
        setContentView(binding.root)

        val taskDao = AppDataBase.getDatabase(applicationContext).taskDao()
        val repository = TaskRepository(taskDao)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TaskViewModel(repository) as T
                }
            }
        )[TaskViewModel::class.java]

        adapter = TaskAdapter (  onItemClick = { task ->
            val fragment = NoteDetailFragment.newInstance(task.id)
            supportFragmentManager.beginTransaction()
            openFragment(fragment)
        },
            viewModel
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }



        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val taskToDelete = adapter.taskList[position]

                    // Анимация удаления
                    val view = viewHolder.itemView
                    view.animate()
                        .translationX((if (direction == ItemTouchHelper.LEFT) -view.width else view.width).toFloat())
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            viewModel.deleteTask(taskToDelete)
                        }
                        .start()
                }
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background =
                    ColorDrawable(ContextCompat.getColor(this@MainActivity, R.color.light_grey))
                val backgroundCornerOffset = 20

                when {
                    dX > 0 -> { // Swiping to the right
                        background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom)
                    }
                    dX < 0 -> { // Swiping to the left
                        background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset, itemView.top, itemView.right, itemView.bottom)
                    }
                    else -> { // view is unSwiped
                        background.setBounds(0, 0, 0, 0)
                    }
                }
                background.draw(canvas)
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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
            openFragment(fragment)
        }
    }
}
