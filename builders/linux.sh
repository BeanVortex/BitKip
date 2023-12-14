FILE_NAME=$NAME-$VERSION

# create installers and runtime (options will be set in build.gradle)
echo "creating installers and runtime"
./gradlew jpackage


# create fat jar of project (see build.gradle)
echo "creating jar"
./gradlew fatJar

echo "creating releases folder"
mkdir ./build/releases/

echo "zipping runtime folder"
tar -czf ./build/releases/$FILE_NAME-linux-bin.tar.gz ./build/image/


echo "creating run file for other linux distributions"
mkdir ./builders/linux-installer/application
mv ./build/image/* ./builders/linux-installer/application
chmod +x ./builders/linux-installer/install.sh
chmod +x ./builders/linux-installer/uninstall.sh
makeself ./builders/linux-installer $FILE_NAME-linux.run "Description" ./install.sh
chmod +x ./builders/linux-installer/$FILE_NAME-linux.run

echo "moving files to releases"
#ls -R | grep ":$" | sed -e 's/:$//' -e 's/[^-][^\/]*\//--/g' -e 's/^/   /' -e 's/-/|/'
#mv ./build/jpackage/$FILE_NAME* ./build/releases/
mv ./build/libs/*.jar ./build/releases/$FILE_NAME-linux.jar
mv ./*.run ./build/releases
ls ./build/releases
