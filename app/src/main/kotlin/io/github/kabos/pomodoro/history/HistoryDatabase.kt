package io.github.kabos.pomodoro.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PomodoroRecord::class], version = 1)
abstract class HistoryDatabase : RoomDatabase() {
  abstract fun pomodoroDao(): PomodoroDao

  companion object {
    fun create(context: Context): HistoryDatabase =
      Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "history.db").build()
  }
}
