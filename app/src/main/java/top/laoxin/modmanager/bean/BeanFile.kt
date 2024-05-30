package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable


class BeanFile : Parcelable {
    constructor(
        name: String?,
        path: String?,
        isDir: Boolean,
        isGrantedPath: Boolean,
        pathPackageName: String?
    ) {
        this.name = name
        this.path = path
        this.isDir = isDir
        this.isGrantedPath = isGrantedPath
        this.pathPackageName = pathPackageName
    }

    /**
     * 文件名
     */
    var name: String?

    /**
     * 文件路径
     */
    var path: String?

    /**
     * 是否文件夹
     */
    var isDir: Boolean

    /**
     * 是否被Document授权的路径
     */
    var isGrantedPath: Boolean

    /**
     * 如果文件夹名称是应用包名，则将包名保存到该字段
     */
    var pathPackageName: String?

    protected constructor(parcel: Parcel) {
        name = parcel.readString()
        path = parcel.readString()
        isDir = parcel.readByte().toInt() != 0
        isGrantedPath = parcel.readByte().toInt() != 0
        pathPackageName = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(path)
        dest.writeByte((if (isDir) 1 else 0).toByte())
        dest.writeByte((if (isGrantedPath) 1 else 0).toByte())
        dest.writeString(pathPackageName)
    }

    override fun toString(): String {
        return "BeanFile{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", isDir=" + isDir +
                ", isGrantedPath=" + isGrantedPath +
                ", pathPackageName='" + pathPackageName + '\'' +
                '}'
    }



    companion object CREATOR : Parcelable.Creator<BeanFile> {
        override fun createFromParcel(parcel: Parcel): BeanFile {
            return BeanFile(parcel)
        }

        override fun newArray(size: Int): Array<BeanFile?> {
            return arrayOfNulls(size)
        }
    }


}
