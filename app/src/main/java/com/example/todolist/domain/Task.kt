package com.example.todolist.domain

data class Task (val id: Int, val name: String, var description: String, var isCompleted: Boolean = false)