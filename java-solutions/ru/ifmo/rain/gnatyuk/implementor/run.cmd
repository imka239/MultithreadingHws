@ECHO off

SET current=%cd%

cd ..\..\..\..\..\..\..\

SET ROOT=%cd%

SET BASIC_PATH=%ROOT%\java-advanced-2020
SET SOLUTION_PATH=%ROOT%\java-advanced-2020-solutions

SET MODULE_NAME=java-solutions

SET LIB_PATH=%BASIC_PATH%\lib;%BASIC_PATH%\artifacts;
@ECHO on

java -cp java-advanced-2020-solutions\java-solutions\ru.ifmo.rain.gnatyuk.implementor -p %LIB_PATH% -m info.kgeorgiy.java.advanced.implementor advanced java-advanced-2020-solutions\java-solutions\ru.ifmo.rain.gnatyuk.implementor.Implementor
cd %current%