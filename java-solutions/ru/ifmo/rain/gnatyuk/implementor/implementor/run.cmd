@echo off

SET task=implementor
SET class=Implementor
SET MOD=%1
SET task=implementor
SET pkg_name=ru.ifmo.rain.gnatyuk.implementor
SET mod_dir=ru\ifmo\rain\gnatyuk\implementor

SET wd=%cd%
SET root_path=C:\Users\ASUS\IdeaProjects\java_corona
SET mod_path=%root_path%\java-advanced-2020\artifacts;%root_path%\java-advanced-2020\lib;%root_path%\out\production\%task%
SET src=%wd%
SET out=%root_path%\out\production\%task%

javac --module-path %mod_path% %src%\module-info.java %src%\%mod_dir%\*.java -d %out%

cd %out%

@echo on
java --module-path %mod_path% --add-modules %task% -m info.kgeorgiy.java.advanced.%task%^
 %MOD% %pkg_name%.%class% %salt%

cd %wd%