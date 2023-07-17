; JavaFX Application Installer Script
!define version "$%BitKip-version%"
!define appName "$%BitKip%"
!define filename "${appName}-${version}.exe"
!define publisher "DarkDeveloper"
!define uninstallerName "uninstall.exe"

; Set the output directory and executable name
Outfile ${fileName}
; Set the default installation directory
InstallDir $PROGRAMFILES\${appName}
; Request application privileges
RequestExecutionLevel admin

; Installer pages
Page directory
Page instfiles

Var PrevInstallFolder
Var InstallFolder

; The actual installation process
Section "Install"
    ; Check if the application is already installed
    StrCpy $InstallFolder $INSTDIR
    ReadRegStr $0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "UninstallString"
    StrCmp $0 "" ContinueInstallation ExistingInstallation

ExistingInstallation:
    ; Show a message to inform the user about the existing installation
    ReadRegStr $PrevInstallFolder HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "InstallLocation"
    StrCpy $InstallFolder $PrevInstallFolder
    MessageBox MB_ICONQUESTION|MB_YESNO "An existing installation of ${appName} is detected in $PrevInstallFolder.$\r$\nDo you want to upgrade it? (no for fresh install)" IDYES UpgradeInstallation IDNO FreshInstallation


;CancelInstallation:
    ;MessageBox MB_ICONINFORMATION|MB_OK "Installation Aborted."
   ; Abort


FreshInstallation:
    StrCpy $InstallFolder $INSTDIR
    Goto UpgradeInstallation

ContinueInstallation:
MessageBox MB_OK "DS"
    Goto UpgradeInstallation

UpgradeInstallation:
    SetOutPath $InstallFolder
    ; Copy application files to the installation directory
    SetOverwrite on
    File /r "..\..\build\jpackage\${appName}\*"
    File "..\..\src\main\resources\ir\darkdeveloper\bitkip\icons\logo.ico"

    ; Create a shortcut in the Start Menu
    CreateShortCut "$SMPROGRAMS\${appName}.lnk" "$InstallFolder\${appName}.exe"

    ; Add the application to the Windows startup
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "${appName}" "$InstallFolder\${appName}.exe"


SectionEnd

Function .onInit
    ; Add the uninstall registry entry during initialization
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayName" "${appName}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "Publisher" "${publisher}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayVersion" "${version}"
FunctionEnd

Function .onInstSuccess
    ; Add the uninstall registry entry after installation
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "UninstallString" "$InstallFolder\${uninstallerName}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayIcon" "$InstallFolder\logo.ico"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "InstallLocation" $InstallFolder
FunctionEnd