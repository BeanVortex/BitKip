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
Page components
Page directory
Page instfiles

; The actual installation process
Section "Install"
    SetOutPath $INSTDIR
    ; Copy application files to the installation directory
    SetOverwrite on
    File /r "..\..\build\jpackage\${appName}\*"
    File "..\..\src\main\resources\ir\darkdeveloper\bitkip\icons\logo.ico"

    ; Create a shortcut in the Start Menu
    CreateShortCut "$SMPROGRAMS\${appName}.lnk" "$INSTDIR\${appName}.exe"

    ; Add the application to the Windows startup
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "${appName}" "$INSTDIR\${appName}.exe"
SectionEnd


Function .onInit
    ; Add the uninstall registry entry during initialization
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayName" "${appName}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "InstallLocation" $INSTDIR
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "UninstallString" '"$INSTDIR\${uninstallerName}"'
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayIcon" "$INSTDIR\logo.ico"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "Publisher" "${publisher}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${appName}" "DisplayVersion" "${version}"
FunctionEnd