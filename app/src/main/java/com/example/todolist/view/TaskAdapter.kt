package com.example.todolist.view

import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.databinding.ItemTaskBinding
import com.example.todolist.domain.Task

class TaskAdapter(
    private val onItemClick: (Task) -> Unit,
    private val viewModel: TaskViewModel
) : RecyclerView.Adapter<TaskViewHolder>() {

    val taskList = mutableListOf<Task>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding, onItemClick, viewModel)
    }

    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {
        holder.bind(taskList[position])
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    fun updateTasks(newTasks: List<Task>){
        taskList.clear()
        taskList.addAll(newTasks)
        notifyDataSetChanged()
    }

    override fun onViewAttachedToWindow(holder: TaskViewHolder) {
        super.onViewAttachedToWindow(holder)
        animateItemAppearance(holder.itemView)
    }

    private fun animateItemAppearance(view: View){
        view.alpha = 0f
        view.translationY = 50f

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private class TaskDiffCallback(
        private val oldList: List<Task>,
        private val newList: List<Task>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

class TaskViewHolder(val binding: ItemTaskBinding, private val onItemClick: (Task) -> Unit,
                     private val viewModel: TaskViewModel) : RecyclerView.ViewHolder(binding.root) {

    fun bind(task: Task){
        binding.taskTitleTextview.text = task.name

        binding.root.setOnClickListener {
            onItemClick(task)
        }

        binding.taskCheckbox.isChecked = task.isCompleted

        binding.taskCheckbox.setOnCheckedChangeListener {
            _, isChecked -> viewModel.updateCompletionStatus(task.id, isChecked)
        }
    }

}
