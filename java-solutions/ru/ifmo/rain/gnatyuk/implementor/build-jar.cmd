@echo off

SET pkg_name=ru.ifmo.rain.gnatyuk.implementor
SET mod_dir=ru\ifmo\rain\gnatyuk\implementor
SET mod_name=implementor


SET root_path=%cd%\..\..\..\..\..\..\..\..\java-advanced-2020
SET mod_path=%root_path%\artifacts;%root_path%\lib

SET src=%cd%
SET out=%cd%\_build
SET run=%cd%

javac --module-path %mod_path% %src%\module-info.java %src%\%mod_dir%\*.java -d %out%
cd %out%
jar -c --file=%run%\_implementor.jar --main-class=%pkg_name%.Implementor --module-path=%mod_path% module-info.class %mod_dir%\*.class
cd %run%