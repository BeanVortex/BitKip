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
tar -czf ./build/releases/$FILE_NAME-mac-bin.zip ./build/image/

echo "moving files to releases"
mv ./build/jpackage/$FILE_NAME* ./build/releases/
mv ./build/libs/*.jar ./build/releases/$FILE_NAME-mac.jar

