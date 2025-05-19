#!/bin/bash

# 确保gradlew有执行权限
chmod +x ./gradlew

# 清理之前的构建
./gradlew clean

# 构建release APK
./gradlew assembleRelease

# 显示APK位置
echo "APK生成完成，位置在:"
echo "app/build/outputs/apk/release/app-release-unsigned.apk"

# 复制APK到项目根目录
mkdir -p output
cp app/build/outputs/apk/release/app-release-unsigned.apk output/WristWave.apk

echo "APK已复制到 output/WristWave.apk"