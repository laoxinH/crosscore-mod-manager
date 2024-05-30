// IFileExplorerService.aidl
package top.laoxin.modmanager.useservice;
import top.laoxin.modmanager.bean.BeanFile;
import top.laoxin.modmanager.bean.ModBean;
// Declare any non-default types here with import statements

interface IFileExplorerService {
    List<ModBean> listFiles(String path, String appPath, String gameModPath, String downloadModPath);
    boolean deleteFile(String path);
    boolean copyFile(String srcPath, String destPath);
    boolean writeFile(String srcPath,String name, String content);
    boolean fileExists(String path);
    boolean chmod(String path);
    boolean unZipFile(String zipPath,String unzipPath,String filename, String password);
    boolean scanMods(String path, String appPath, String gameModPath, String downloadModPath);
}