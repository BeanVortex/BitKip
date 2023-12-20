FILE_NAME=$NAME-extension-$EXT_VERSION


echo "creating releases folder"
mkdir ./build/releases/

echo "zipping folder"
zip -r $FILE_NAME.zip ./extensions

echo "moving file to releases"
mv ./$FILE_NAME.zip ./build/releases

ls
