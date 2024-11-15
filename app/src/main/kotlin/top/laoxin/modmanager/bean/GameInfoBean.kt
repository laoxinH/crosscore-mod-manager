package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class GameInfoBean(
    val gameName: String,
    val serviceName: String,
    val packageName: String,
    val gamePath: String,
    val modSavePath: String = "",
    val antiHarmonyFile: String = "",
    val antiHarmonyContent: String = "",
    val gameFilePath: List<String>,
    val version: String,
    val modType: List<String>,
    val isGameFileRepeat: Boolean = true,
    val enableBackup: Boolean = true,
    val tips: String = ""

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(gameName)
        parcel.writeString(serviceName)
        parcel.writeString(packageName)
        parcel.writeString(gamePath)
        parcel.writeString(modSavePath)
        parcel.writeString(antiHarmonyFile)
        parcel.writeString(antiHarmonyContent)
        parcel.writeStringList(gameFilePath)
        parcel.writeString(version)
        parcel.writeStringList(modType)
        parcel.writeByte(if (isGameFileRepeat) 1 else 0)
        parcel.writeByte(if (enableBackup) 1 else 0)
        parcel.writeString(tips)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GameInfoBean> {
        override fun createFromParcel(parcel: Parcel): GameInfoBean {
            return GameInfoBean(parcel)
        }

        override fun newArray(size: Int): Array<GameInfoBean?> {
            return arrayOfNulls(size)
        }
    }
}
