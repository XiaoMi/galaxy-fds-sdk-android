#文件存储(FDS)Android SDK使用介绍

## 编译安装sdk的jar:
```
mvn clean package -DskipTests
# 或者使用以下命令打出包含所有依赖的包
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```
## 使用Android Studio导入SDK
* 在Gradle中添加依赖：
```
allprojects {
    repositories {
            mavenCentral()     
    }
}
dependencies {
    ...
    compile 'com.google.guava:guava:15.0'
    compile 'com.xiaomi.infra.galaxy:galaxy-fds-core:3.0.5'
    compile 'com.xiaomi.infra.galaxy:galaxy-fds-sdk-android:3.0.5'
    compile 'com.google.code.gson:gson:2.6.2'
    ...
}
```
* 在Manifest中添加权限申明：
```
    ...
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    ...
```

#FDS Android SDK User Guide

## Build jar from the source:
```
mvn clean package -DskipTests
# or use following command to build jar with all dependencies
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```

## Import SDK to Android Studio project
* Add dependency in gradle：
```
allprojects {
    repositories {
            mavenCentral()     
    }
}
dependencies {
    ...
    compile 'com.google.guava:guava:15.0'
    compile 'com.xiaomi.infra.galaxy:galaxy-fds-core:3.0.8'
    compile 'com.xiaomi.infra.galaxy:galaxy-fds-sdk-android:3.0.8'
    compile 'com.google.code.gson:gson:2.6.2'
    ...
}
```
* Add permission declaration in Manifest：
```
    ...
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    ...
```
