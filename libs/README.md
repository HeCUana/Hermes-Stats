# libs/

本目录用于存放 **Hermes Lib 1.0.0** 的本地 jar（`hermes-lib-1.0.0.jar`）。

> 该 jar 是另一个项目的**构建产物**（约 11.7 MB，内含 sqlite-jdbc 的多平台原生库），
> 不适合塞进本仓库的 git 历史，因此被 `.gitignore` 排除。首次构建前请按下面任一方式放入。

## 方式一：从 Hermes Lib 源码构建（推荐）

```bash
git clone https://github.com/HeCUana/Hermes-Lib.git
cd hermes-lib
./gradlew build
cp build/libs/hermes-lib-1.0.0.jar <Hermes-Stats>/libs/
```

Windows PowerShell：

```powershell
git clone https://github.com/HeCUana/Hermes-Lib.git
cd hermes-lib
.\gradlew.bat build
Copy-Item build\libs\hermes-lib-1.0.0.jar ..\Hermes-Stats\libs\
```

## 方式二：使用已有的 jar

把 `hermes-lib-1.0.0.jar` 直接复制到本目录即可。

> 如果你希望本仓库「克隆即构建」，删掉 `.gitignore` 里的 `libs/*.jar` 一行，
> 再执行 `git add libs/hermes-lib-1.0.0.jar` 提交该二进制即可（代价是仓库体积增加约 12 MB）。

## 依赖声明

`build.gradle` 中以本地 jar 的方式声明：

```groovy
implementation files("libs/hermes-lib-1.0.0.jar")
```

若 Hermes Lib 之后发布到 Maven，把上面这行换成 Maven 坐标即可：

```groovy
implementation "com.hermeslib:hermes-lib:1.0.0"
```
