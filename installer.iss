[Setup]
AppName=盛韵建筑账目管理系统
AppVersion=1.0.4
AppPublisher=盛韵建筑
AppPublisherURL=https://www.example.com
AppSupportURL=https://www.example.com/support
AppUpdatesURL=https://www.example.com/updates
DefaultDirName={pf}\盛韵建筑
DefaultGroupName=盛韵建筑
OutputDir=./output
OutputBaseFilename=盛韵建筑安装程序
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin

[Files]
; 应用程序文件
Source: "app\ceams-1.0-SNAPSHOT.jar"; DestDir: "{app}\app"
Source: "app\lib\*"; DestDir: "{app}\app\lib"
; MySQL文件
Source: "mysql\*"; DestDir: "{app}\mysql"
; 脚本文件
Source: "start.bat"; DestDir: "{app}"
Source: "stop.bat"; DestDir: "{app}"
Source: "init-mysql.bat"; DestDir: "{app}"
; 资源文件
Source: "src\main\resources\images\app-icon.png"; DestDir: "{app}\app\images"

[Icons]
Name: "{group}\盛韵建筑账目管理系统"; Filename: "{app}\start.bat"
Name: "{group}\卸载盛韵建筑账目管理系统"; Filename: "{uninstallexe}"
Name: "{commondesktop}\盛韵建筑账目管理系统"; Filename: "{app}\start.bat"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加任务:"

[Run]
Filename: "{app}\start.bat"; Description: "启动盛韵建筑账目管理系统"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{app}\stop.bat"; Flags: runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{app}"