// IFileExplorerService.aidl
package top.laoxin.modmanager.useservice;
import top.laoxin.modmanager.bean.BeanFile;
import top.laoxin.modmanager.bean.ModBean;
import top.laoxin.modmanager.bean.GameInfoBean;
// Declare any non-default types here with import statements

interface IFileExplorerService {
    List<String> getFilesNames(String path);
    boolean deleteFile(String path);
    boolean copyFile(String srcPath, String destPath);
    boolean writeFile(String srcPath,String name, String content);
    boolean fileExists(String path);
    boolean chmod(String path);
    boolean unZipFile(String zipPath,String unzipPath,String filename, String password);
    boolean scanMods(String sacnPath, in GameInfoBean gameInfo);
    boolean moveFile(String srcPath, String destPath);
    boolean isFile(String path);
    // 文件是否发生变化
    long isFileChanged(String path);
    boolean changDictionaryName (String path, String newName);
    boolean createDictionary (String path);
}