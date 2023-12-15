#!/bin/bash

# sudo
[ "$UID" -eq 0 ] || exec sudo "$0" "$@"

desktopFile="BitKip.desktop"
uninstallDesktopFile="BitKipUninstall.desktop"
desktopFilePath="./application/$desktopFile"
uninstallDesktopFilePath="./application/$uninstallDesktopFile"


for user in $(cut -d: -f1 /etc/passwd); do
    if [ -d "/home/$user" ]; then
        cp "$desktopFilePath" "/home/$user/.config/autostart/"
        chown $user:$user "/home/$user/.config/autostart/$desktopFile"
    fi
done

mv "$desktopFilePath" /usr/share/applications/
mv "$uninstallDesktopFilePath" /usr/share/applications/

chmod a+rx ./application/bitkip
mv ./application/bitkip /usr/bin/
mkdir BitKip 
mv -v ./application/* ./BitKip
rm -d application
chmod a+rx BitKip/
rm -r /usr/share/BitKip/
mv ./BitKip /usr/share/




