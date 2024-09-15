package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
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

    /*   override fun equals(other: Any?): Boolean {
           if (this === other) return true
           if (javaClass != other?.javaClass) return false
           val modBean = other as ModBean
           if (name != modBean.name) return false
           if (path != modBean.path) return false
           //if (id != modBean.id) return false
           if (isEncrypted != modBean.isEncrypted) return false
           //if (isEnable != modBean.isEnable) return false
           if (isZipFile != modBean.isZipFile) return false
           if (version != modBean.version) return false
           if (description != modBean.description) return false
           if (author != modBean.author) return false
           if (date != modBean.date) return false
           if (icon != modBean.icon) return false
           if (images != modBean.images) return false
           if (modFiles != modBean.modFiles) return false
           if (password != modBean.password) return false
           if (readmePath != modBean.readmePath) return false
           if (fileReadmePath != modBean.fileReadmePath) return false
           if (gamePackageName != modBean.gamePackageName) return false
           if (gameModPath != modBean.gameModPath) return false
           if (modType != modBean.modType) return false
           return true
       }

       override fun hashCode(): Int {
           var result = name?.hashCode() ?: 0
           result = 31 * result + (version?.hashCode() ?: 0)
           result = 31 * result + (description?.hashCode() ?: 0)
           result = 31 * result + (author?.hashCode() ?: 0)
           result = 31 * result + date.hashCode()
           result = 31 * result + (path?.hashCode() ?: 0)
           result = 31 * result + (icon?.hashCode() ?: 0)
           result = 31 * result + (images?.hashCode() ?: 0)
           result = 31 * result + (modFiles?.hashCode() ?: 0)
          // result = 31 * result + isEncrypted.hashCode()
           result = 31 * result + (password?.hashCode() ?: 0)
           result = 31 * result + (readmePath?.hashCode() ?: 0)
           result = 31 * result + (fileReadmePath?.hashCode() ?: 0)
           result = 31 * result + (gamePackageName?.hashCode() ?: 0)
           result = 31 * result + (gameModPath?.hashCode() ?: 0)
           result = 31 * result + (modType?.hashCode() ?: 0)
           result = 31 * result + isEnable.hashCode()
           result = 31 * result + isZipFile.hashCode()
           return result
       }*/

    fun equalsIgnoreId(other: ModBean): Boolean {
        if (this === other) return true
        if (name != other.name) return false
        if (version != other.version) return false
        if (description != other.description) return false
        if (author != other.author) return false
        if (date != other.date) return false
        if (path != other.path) return false
        if (icon != other.icon) return false
        if (images != other.images) return false
        if (modFiles != other.modFiles) return false
        if (isEncrypted != other.isEncrypted) return false
        //if (password != other.password) return false
        if (readmePath != other.readmePath) return false
        if (fileReadmePath != other.fileReadmePath) return false
        if (gamePackageName != other.gamePackageName) return false
        if (gameModPath != other.gameModPath) return false
        if (modType != other.modType) return false
        //if (isEnable != other.isEnable) return false
        if (isZipFile != other.isZipFile) return false

        return true
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
                    mod.date != date ||
                    mod.icon != icon ||
                    mod.images != images ||
                    mod.modFiles != modFiles ||
                    mod.readmePath != readmePath ||
                    mod.fileReadmePath != fileReadmePath ||
                    mod.gameModPath != gameModPath ||
                    mod.modType != modType ||
                    mod.isZipFile != isZipFile
                ) {
                    return this.copy(
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
                        isZipFile = mod.isZipFile
                    )
                }

                //if (mod.isEncrypted != isEncrypted) return true
                //if (mod.password != password) return true
                //if (mod.gamePackageName != gamePackageName) return true
                //if (mod.isEnable != isEnable) return true

            }
        }
        return null
    }

    fun isNew(mods: List<ModBean>): ModBean? {
        Log.d("ModBean", "所有mod$mods")
        if (mods.none { it.path == path && it.name == name }) {
            Log.d("ModBean", "新的: $name==$path")
            return this
        }
        return null
    }
}
