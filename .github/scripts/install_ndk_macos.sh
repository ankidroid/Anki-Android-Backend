: ${2?"Usage: $0 NDK Short Version (r22), NDK Long Version (22.0.7026061) "}
# We don't use sdkmanager due to https://stackoverflow.com/questions/46402772/failed-to-install-android-sdk-java-lang-noclassdeffounderror-javax-xml-bind-a

if [ ! -d "$ANDROID_SDK_ROOT/ndk/$2/" ]
then
  echo 'Downloading NDK'
  wget --no-verbose -O android-ndk.zip "https://dl.google.com/android/repository/android-ndk-$1-darwin-x86_64.zip"
  echo 'Unzipping NDK'
  unzip -q -d $2 android-ndk.zip
  mkdir $ANDROID_SDK_ROOT/ndk/$2/
  mv $2/*/* $ANDROID_SDK_ROOT/ndk/$2
else
  echo 'Skipping NDK download - already installed'
fi