// IFileExplorerService.aidl
package top.laoxin.modmanager.service.shizuku;

import top.laoxin.modmanager.service.model.RemoteResult;
import top.laoxin.modmanager.service.model.RemoteStringResult;
import top.laoxin.modmanager.service.model.RemoteBoolResult;
import top.laoxin.modmanager.service.model.RemoteLongResult;
import top.laoxin.modmanager.service.model.RemoteStringListResult;
import top.laoxin.modmanager.service.model.RemoteFileListResult;
import android.os.ParcelFileDescriptor;

/**
 * 文件浏览器服务接口
 * 通过 Shizuku 提供高权限文件操作
 * 所有方法返回封装的 Result 类型，包含操作结果和错误信息
 */
interface IFileExplorerService {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取目录下的文件名列表
     * @param path 目录路径
     * @return RemoteStringListResult 包含文件名列表
     */
    RemoteStringListResult getFilesNames(String path);
    
    /**
     * 列出目录下的所有文件信息
     * @param path 目录路径
     * @return RemoteFileListResult 包含文件信息列表
     */
    RemoteFileListResult listFile(String path);
    
    /**
     * 判断文件是否存在
     * @param path 文件路径
     * @return RemoteBoolResult 包含是否存在
     */
    RemoteBoolResult fileExists(String path);
    
    /**
     * 判断是否为文件
     * @param path 路径
     * @return RemoteBoolResult 包含是否为文件
     */
    RemoteBoolResult isFile(String path);
    
    /**
     * 获取文件最后修改时间
     * @param path 文件路径
     * @return RemoteLongResult 包含修改时间戳
     */
    RemoteLongResult getLastModified(String path);
    
    /**
     * 读取文件内容
     * @param path 文件路径
     * @return RemoteStringResult 包含文件内容
     */
    RemoteStringResult readFile(String path);
    
    // ==================== 文件操作 ====================
    
    /**
     * 复制文件
     * @param srcPath 源文件路径
     * @param destPath 目标文件路径
     * @return RemoteResult 操作结果
     */
    RemoteResult copyFile(String srcPath, String destPath);
    
    /**
     * 删除文件或目录
     * @param path 文件路径
     * @return RemoteResult 操作结果
     */
    RemoteResult deleteFile(String path);
    
    /**
     * 移动文件
     * @param srcPath 源文件路径
     * @param destPath 目标文件路径
     * @return RemoteResult 操作结果
     */
    RemoteResult moveFile(String srcPath, String destPath);
    
    /**
     * 写入文件
     * @param srcPath 目录路径
     * @param name 文件名
     * @param content 内容
     * @return RemoteResult 操作结果
     */
    RemoteResult writeFile(String srcPath, String name, String content);
    
    /**
     * 通过流创建文件
     * @param path 目录路径
     * @param filename 文件名
     * @param pfd ParcelFileDescriptor
     * @return RemoteResult 操作结果
     */
    RemoteResult createFileByStream(String path, String filename, in ParcelFileDescriptor pfd);
    
    /**
     * 创建目录
     * @param path 目录路径
     * @return RemoteResult 操作结果
     */
    RemoteResult createDictionary(String path);
    
    /**
     * 重命名目录
     * @param path 原路径
     * @param newName 新名称
     * @return RemoteResult 操作结果
     */
    RemoteResult changDictionaryName(String path, String newName);
    
    /**
     * 修改文件权限
     * @param path 文件路径
     * @return RemoteResult 操作结果
     */
    RemoteResult chmod(String path);


    /**
    * 计算文件MD5
    * @param path 文件路径
    * @return RemoteStringResult
    */
    RemoteStringResult md5(String path);
    /**
    * 获取文件大小
    * @param path 文件路径
    * @return RemoteLongResult
    */
    RemoteLongResult getFileSize(String path);
}