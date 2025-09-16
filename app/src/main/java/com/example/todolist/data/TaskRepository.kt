package com.example.todolist.data

import com.example.todolist.domain.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTask() : Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun  deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    fun getTaskById(id: Int): Flow<Task?>{
        return taskDao.getTaskById(id)
    }

    suspend fun updateCompletionStatus(id: Int, isComlete: Boolean){
        taskDao.updateCompletionStatus(id, isComlete)
    }
}