package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * mod实体类
 */
@Entity(tableName = "mods")
data class ModBean(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val version: String?,
    val description: String?,
    val author: String?,
    val date: Long,
    val path: String?,
    val icon: String?,
    val images: List<String>?,
    val modFiles: List<String>?,
    val isEncrypted : Boolean,
    val password : String?,
    var readmePath: String?,
    var fileReadmePath: String?,
    var isEnable: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList(),
        parcel.createStringArrayList(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(version)
        parcel.writeString(description)
        parcel.writeString(author)
        parcel.writeLong(date)
        parcel.writeString(path)
        parcel.writeString(icon)
        parcel.writeStringList(images)
        parcel.writeStringList(modFiles)
        parcel.writeByte(if (isEncrypted) 1 else 0)
        parcel.writeString(password)
        parcel.writeString(readmePath)
        parcel.writeString(fileReadmePath)
        parcel.writeByte(if (isEnable) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ModBean> {
        override fun createFromParcel(parcel: Parcel): ModBean {
            return ModBean(parcel)
        }

        override fun newArray(size: Int): Array<ModBean?> {
            return arrayOfNulls(size)
        }
    }
}
