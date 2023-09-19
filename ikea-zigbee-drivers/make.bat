
ECHO off
CLS
IF [%1]==[clean] GOTO clean

ECHO Generating driver files:
SET devices=E1810 E2123

for /D %%A in (src/devices/*) do (
    ECHO - %%A.groovy
    tools\yaml-merge.exe src\common.yaml src\devices\%%A\config.yaml | tools\mustache.exe src\blueprint.groovy > %%A.groovy
)
GOTO done

:clean
ECHO Removing output files:
SET delFileList=*.groovy
FOR %%A IN (%delFileList%) DO (
    DEL %%A
    ECHO - %%A deleted
)

:done
ECHO.
ECHO Done!
