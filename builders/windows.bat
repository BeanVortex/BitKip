@ECHO OFF

set FILE_NAME=%NAME%-%VERSION%


@REM create installers and runtime (options are set in build.gradle)
echo creating installers and runtime
call gradlew.bat jpackage


@REM create fat jar of project (see build.gradle)
echo creating jar
call gradlew.bat fatJar

echo creating releases folder
mkdir build\releases\


echo zipping runtime folder
powershell -command "Compress-Archive -Path 'build\jpackage\%NAME%' -DestinationPath 'build\releases\%FILE_NAME%-win-bin.zip'"

echo creating nsis installer
makensis builders\windows-installer\uninstall.nsi
move builders\windows-installer\uninstall.exe build\jpackage\%NAME%\
makensis builders\windows-installer\installer.nsi

echo moving files to releases
move builders\windows-installer\*.exe build\releases\
move .\build\libs\%FILE_NAME%.jar .\build\releases\%FILE_NAME%-win.jar
dir .\build\releases

