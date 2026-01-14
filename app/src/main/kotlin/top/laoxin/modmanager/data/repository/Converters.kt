package top.laoxin.modmanager.data.repository

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import top.laoxin.modmanager.domain.bean.ModForm

class Converters {

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromModForm(value: ModForm): String {
        return value.name
    }

    @TypeConverter
    fun toModForm(value: String): ModForm {
        return try {
            ModForm.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ModForm.TRADITIONAL
        }
    }
}
