#!/bin/bash

if [ "$EUID" -ne 0 ]
then
echo "Please run as root to copy the application folder and files to /usr/share and /usr/bin"
exit
fi

desktopFile="BitKip.desktop"
desktopFilePath="./application/$desktopFile"


for user in $(cut -d: -f1 /etc/passwd); do
    if [ -d "/home/$user" ]; then
        cp "$desktopFilePath" "/home/$user/.config/autostart/"
        chown $user:$user "/home/$user/.config/autostart/$desktopFile"
    fi
done

mv "$desktopFilePath" /usr/share/applications/

mv ./application/bitkip /usr/bin/
mkdir BitKip 
mv -v ./application/* ./BitKip
rm -d application
chmod a+rx BitKip/
rm -r /usr/share/BitKip/
mv ./BitKip /usr/share/




