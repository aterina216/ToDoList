package com.example.todolist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.todolist.domain.Task

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object{
        @Volatile
        private var Instanse: AppDataBase? = null

        fun getDatabase(context: Context) : AppDataBase{
            return Instanse?:synchronized(this){
                Room.databaseBuilder(
                    context,
                    AppDataBase::class.java,
                    "task_app_database"
                ).build()
                    .also {Instanse = it}

            }
        }
    }
}