@echo off
set appjar=spotbugs.jar
set javahome=
set launcher=java.exe
set jvmheap="-Xmx8g"
set jvmargs="-Xss100m"
set debugArg=
set conserveSpaceArg=
set workHardArg=
set args=

:: Try finding the default SPOTBUGS_HOME directory
:: from the directory path of this script
set jar_home=%~dp0

:: Honor JAVA_HOME environment variable if it is set
if "%JAVA_HOME%"=="" goto nojavahome
if not exist "%JAVA_HOME%\bin\javaw.exe" goto nojavahome
set javahome=%JAVA_HOME%\bin\

"%javahome%%launcher%" %debugArg% %jvmheap% %jvmargs% -jar "%jar_home%\spotbugsGUI-1.0-SNAPSHOT.jar"
