@echo off
@title Lidium Server Console
set CLASSPATH=.;dist\Lidium.jar;lib\*;lib\graaljs\*
java -server -Dnet.sf.odinms.wzpath=wz\ server.Start
pause