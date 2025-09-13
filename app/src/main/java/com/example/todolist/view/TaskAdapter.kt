package com.example.todolist.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding
import com.example.todolist.domain.Task

class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {

    val taskList = mutableListOf<Task>(Task(1, "la", "la"))

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
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

}

class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(task: Task){
        binding.taskTitleTextview.text = task.name
    }
}
