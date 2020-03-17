@echo off
SET run=%cd%
cd ../../../../../../../
SET root=%cd%
SET mod_name=ru.ifmo.rain.gnatyuk.implementor
SET mod_path=ru\ifmo\rain\gnatyuk\implementor
SET path=%root%\lib;%root%\artifacts;%root%\java-advanced-2020-solutions

@echo on
java --module-path %path% -m %mod_name%
cd %run%
