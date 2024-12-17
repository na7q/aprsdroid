#!/bin/sh

# Move to the working directory
cd /home/user/aprsdroid || exit 1

# Print the current user
whoami


git submodule update --init --recursive

# Set the local.properties file
echo "mapsApiKey=a" > local.properties

# Remove specific lines from build.gradle using sed
sed -i '/id "app.brant.amazonappstorepublisher" version "0.1.0"/d' build.gradle
sed -i '/amazon {/,/}/d' build.gradle

./gradlew clean
# Run the Gradle assemble task
./gradlew assemble

cp /home/user/aprsdroid/build/outputs/apk/debug/aprsdroid-debug.apk /home/user/aprsdroid/aprsdroid-release.apk
 
