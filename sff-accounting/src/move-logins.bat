set LOGIN_HOME=C:\Login
set JAVA_HOME=C:\Program Files\Java\j2re1.4.2_07

cd %LOGIN_HOME%
"%JAVA_HOME%\bin\javaw" -cp sff-accounting.jar sff.accounting.Dither %LOGIN_HOME% >> move-logins.log
"%JAVA_HOME%\bin\javaw" -cp sff-accounting.jar sff.accounting.MoveLoginRecords %LOGIN_HOME% >> move-logins.log
