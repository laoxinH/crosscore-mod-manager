<h1 align="center">
    <img src="https://github.com/laoxinH/crosscore-mod-manager/blob/main/app/src/main/res/drawable/start.webp" width="200" alt="Mod实验室">
  <br>Mod实验室<br>
</h1>

<p align="center">
  <a href="https://github.com/laoxinH/crosscore-mod-manager/actions/workflows/ci.yml">
    <img src="https://github.com/laoxinH/crosscore-mod-manager/actions/workflows/ci.yml/badge.svg" alt="Github Actions">
  </a>
  <a href="/LICENSE">
    <img src="https://img.shields.io/github/license/laoxinH/crosscore-mod-manager" alt="LICENSE">
  </a>
  <a href="https://www.codefactor.io/repository/github/laoxinH/crosscore-mod-manager">
    <img src="https://www.codefactor.io/repository/github/laoxinH/crosscore-mod-manager/badge" alt="CodeFactor">
  </a>
  <a href="https://github.com/laoxinH/crosscore-mod-manager/releases/latest">
    <img src="https://img.shields.io/github/v/release/laoxinH/crosscore-mod-manager" alt="Release">
  </a>
  <a href="https://github.com/laoxinH/crosscore-mod-manager/releases">
    <img src="https://img.shields.io/github/downloads/laoxinH/crosscore-mod-manager/total" alt="Release">
  </a>
</p>

<div align="center">
  <h3>
    <a href="#写在前面">
    写在前面
    </a>
    <span> | </span>
    <a href="#已实现功能">
    已实现功能
    </a>
    <span> | </span>
    <a href="#软件使用说明">
    软件使用说明
    </a>
    <span> | </span>
    <a href="#模组打包">
    模组打包
    </a>
    <span> | </span>
    <a href="#自定义扩展">
    自定义扩展
    </a>
  </h3>
</div>

# 写在前面 [<img src="https://api.gitsponsors.com/api/badge/img?id=800784399" height="30">](https://api.gitsponsors.com/api/badge/link?p=anEQkoqzWiYoaPb+VA6SSIrvpqJp7BOcUsN0s+/a660jlr01Gl4Dr93b4G0yVxnKNEqBxzJYyCnvIuZlRtFz5Qi1wKLPXDBJKl5ZWCii/K82F7W4pXTScwMVUI+wXJN9EGdwHtFz39iajhyoHIqbnA==)

**本软件为开源项目，与任何形式的商业收费行为无关，请勿上当受骗。**

**本软件与官方无任何关联。如有侵犯您的权益，请联系开发者处理。**

**使用本软件所产生的任何后果，由用户自行承担，开发者不承担任何责任。请合理使用，避免与官方产生冲突。**

**本软件由个人开发，功能实现有限。如有更优的实现方式或发现Bug，欢迎提交Issue或PR反馈。**

**本软件仅简化用户本地操作，不提供或传播任何资源文件，同时也不会上传任意用户信息。**


# 已实现功能

> **适配安卓9-15**

1. [X] 扫描QQ下载目录中的Mod
2. [X] 扫描系统Download目录中的Mod
3. [X] 一键开启反和谐
4. [X] 支持加密Mod
5. [X] 支持Mod预览
6. [X] 支持在Mod中添加作者信息（详见[模组打包](#模组打包)）
7. [X] 支持任意的Mod打包方式，例如多级目录，不在局限于单一形式（详见[模组打包](#模组打包)）
8. [X] 支持一个压缩文件中打包多个不同Mod（详见[模组打包](#模组打包)）
9. [X] 支持自定义游戏配置文件（详见[自定义扩展](#自定义扩展)）
10. [X] 支持常见压缩包格式如7z、rar、zip
11. [X] 支持批量管理MOD

# 软件使用说明

**[下载地址](https://github.com/laoxinH/crosscore-mod-manager/releases)**

**运行界面（部分设备需要shizuku授权，参考[shizuku官方使用说明](https://shizuku.rikka.app/zh-hans/guide/setup/#%E9%80%9A%E8%BF%87%E6%97%A0%E7%BA%BF%E8%B0%83%E8%AF%95%E5%90%AF%E5%8A%A8)）**

![1715962256872](image/readme/1715962256872.png?msec=1715965017839 "app主页")![1715962345763](image/readme/1715962345763.png?msec=1715965017839 "首次打开Mod页面")

![1715962378369](/image/readme/1715962378369.png?msec=1715965017831 "shizuku授权")![1715962396435](/image/readme/1715962396435.png?msec=1715965017839 "扫描到的mod")![1715962416212](/image/readme/1715962416212.png?msec=1715965017839 "设置页面")

- **首次运行打开Mod页面会请仔细阅读后点击*同意并授权*才能继续使用软件**
- **授权后需要打开Mod页面点击刷新将会扫描Mod**
- **默认扫描控制台*配置的Mod目录*，需要扫描QQ目录和系统下载目录请去控制台开启**
- **扫描的的Mod会自动移动到配置的Mod目录，方便管理**
- **新曾Mod建议直接放置到配置的Mod目录**
- ~~在设置页面可以点击给作者买杯卡布奇洛支持一下~~

# 模组打包

**注意打包MOD如果使用ZIP格式压缩请不要使用中文密码，RAR和7z无所谓**

- [X] **MOD压缩文件推荐结构**

![1715963940732](/image/readme/1715963940732.png?msec=1715965017840)

- **如果不存在readme文件，MOD实验室会提示：未适配MOD管理器，可能存在未知问题**

**readme.txt文件内容示例**

![1715964083599](/image/readme/1715964083599.png?msec=1715965017836)

名称：刃齿爱心眼小玩具
描述：替换默认和动画
作者：laoxin
版本：1.0

**注意冒号为中文标点**

- [X] 支持一个压缩包打包多份MOD

![1715964191813](/image/readme/1715964191813.png?msec=1715965017839)

- **MOD实验室判断不同MOD的标准之一就是以文件夹区分**
- **在实验中显示的MOD名称将会是压缩文件名（文件夹名称），如果存在readme.txt文件将会显示其中的名称**
- **压缩在同一份压缩文件中的MOD都可以存在一份readme文件用于描述，例如在testmod1和testmod2都可以存在readme文件**
- **支持多级目录，但是不推荐**
- **一个推荐的方案是将mod文件（必须存在），预览图（可有可无，推荐添加）、readme（可有可无，推荐添加）三类文件放置到同一个文件夹中**
- **支持加密压缩文件，注意加密后将无法预览MOD详细信息，谨慎使用**

## 特殊类型模组打包

> **关于碧蓝航线等存在不同目录中有重名文件的情况下，mod打包的特殊说明**

由于部分游戏的游戏目录下，两个文件夹中存在同名的游戏文件：如：

文件夹1/文件

文件夹2/文件

如果直接按照“mod打包”的说明进行打包mod，由于同名的文件会导致软件无法确认游戏文件所在路径具体为文件夹1还是文件夹2，进而导致无法正常识别你的mod。

你需要在mod.zip里额外创建一个和你的mod类型对应的文件夹，再在里面存放mod本体，预览图和自述文件等内容。这样就可以使得软件可以正确识别到mod。

# 自定义扩展

## 自定义游戏配置文件

配置文件是**json**格式

> **文件内容如下（可以参照尘白禁区的示例配置修改）：**

- `gameName`: 游戏名
- `serviceName`: 服务器名
- `packageName`: 游戏包名
- `version`: 配置文件版本
- `gamePath`: 游戏的data目录
- `antiHarmonyFile`: 反和谐文件路径，没有则不填
- `antiHarmonyContent`: 写入到反和谐文件中的内容，换行使用 \n ，无内容时留空
- `gameFilePath`: 用于替换 mod 文件的目录
- `modType`: mod类型，必须和 `gameFilePath` 中的目录一一对应，可为中文
- `modSavePath`: mod 文件保存路径
- `isGameFileRepeat`: 游戏文件是否存在重复
- `enableBackup`: 是否启用备份
- `tips`: 使用提示

```json
{
    "gameName": "尘白禁区",  
    "serviceName": "官服2.0.0",  
    "packageName": "com.dragonli.projectsnow.lhm",  
    "version": "2.0.0",  
    "gamePath": "Android/data/com.dragonli.projectsnow.lhm/",  
    "antiHarmonyFile": "",  
    "antiHarmonyContent": "",  
    "gameFilePath": [
        "Android/data/com.dragonli.projectsnow.lhm/files/2.0.0/"
    ],
    "modType": [ "人物模型" ],
    "modSavePath": "",
    "isGameFileRepeat": false,
    "enableBackup": false,
    "tips": "第一次选择尘白禁区游戏需要先清除游戏数据..."
}
```

> **命名为*`xxx.json`*放入MOD实验 `配置的MOD目录`里的 `GameConfig`文件夹，再到设置中点击 `读取游戏配置`，然后点击 `选择游戏`即可使用自定义配置文件**
