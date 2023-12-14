# sudo
[ "$UID" -eq 0 ] || exec sudo "$0" "$@"

read -p "Are you sure you want to uninstall BitKip? (y/n) " RESP
if [ "$RESP" = "n" ]; then
  exit
elif [ "$RESP" != "y" ]; then
  echo "Wrong answer. Aborting"
  exit
fi

echo "Removing application entry"
rm -f /usr/share/applications/BitKip.desktop
echo "Removing application uninstall entry"
rm -f /usr/share/applications/BitKipUninstall.desktop
echo "Removing application entry from startup"
rm -f /home/$user/.config/autostart/BitKip.desktop
echo "Removing application folder"
rm -r /usr/share/BitKip
echo "Removing application from bin"
rm -r /usr/bin/bitkip