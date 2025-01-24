package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import top.laoxin.modmanager.tools.LogTools

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
    // 虚拟路径
    val virtualPaths: String?,
    val icon: String?,
    val images: List<String>?,
    val modFiles: List<String>?,
    val isEncrypted: Boolean,
    val password: String?,
    var readmePath: String?,
    var fileReadmePath: String?,
    var gamePackageName: String?,
    val gameModPath: String?,
    var modType: String?,
    var isEnable: Boolean,
    var isZipFile: Boolean = true,
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
        parcel.readString(),
        parcel.createStringArrayList(),
        parcel.createStringArrayList(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
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
        parcel.writeString(virtualPaths)
        parcel.writeString(icon)
        parcel.writeStringList(images)
        parcel.writeStringList(modFiles)
        parcel.writeByte(if (isEncrypted) 1 else 0)
        parcel.writeString(password)
        parcel.writeString(readmePath)
        parcel.writeString(fileReadmePath)
        parcel.writeString(gamePackageName)
        parcel.writeString(gameModPath)
        parcel.writeString(modType)
        parcel.writeByte(if (isEnable) 1 else 0)
        parcel.writeByte(if (isZipFile) 1 else 0)
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

    fun isDelete(scanMods: List<ModBean>): ModBean? {
        if (scanMods.none { it.path == path && it.name == name }) {
            return this
        }
        return null
    }

    fun isUpdate(scanMods: List<ModBean>): ModBean? {
        if (isEncrypted) return null
        for (mod in scanMods) {
            if (mod.path == path && mod.name == name) {
                if (mod.version != version ||
                    mod.description != description ||
                    mod.author != author ||
                    // 日期不需要吧？
                    // mod.date != date ||
                    mod.icon != icon ||
                    mod.images != images ||
                    mod.modFiles != modFiles ||
                    mod.readmePath != readmePath ||
                    mod.fileReadmePath != fileReadmePath ||
                    mod.gameModPath != gameModPath ||
                    mod.modType != modType ||
                    mod.isZipFile != isZipFile
                ) {
                    var new = this.copy(
                        version = mod.version,
                        description = mod.description,
                        author = mod.author,
                        date = mod.date,
                        icon = mod.icon,
                        images = mod.images,
                        modFiles = mod.modFiles,
                        isEncrypted = mod.isEncrypted,
                        readmePath = mod.readmePath,
                        fileReadmePath = mod.fileReadmePath,
                        gameModPath = mod.gameModPath,
                        modType = mod.modType,
                        isZipFile = mod.isZipFile,
                        virtualPaths = mod.virtualPaths
                    )
                    LogTools.logRecord("更新前$this")
                    LogTools.logRecord("更新后$new")
                    return new
                }
            }
        }
        return null
    }

    fun isNew(mods: List<ModBean>): ModBean? {
        //Log.d("ModBean", "所有mod$mods")
        if (mods.none { it.path == path && it.name == name }) {
            //Log.d("ModBean", "新的: $name==$path")
            return this
        }
        return null
    }
}
