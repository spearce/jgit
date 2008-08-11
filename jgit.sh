#!/bin/sh

if [ "@@use_self@@" = "1" ]
then
	this_script=`which "$0" 2>/dev/null`
	[ $? -gt 0 -a -f "$0" ] && this_script="$0"
	cp=$this_script
else
	jgit_home=`dirname $0`
	cp="$jgit_home/org.spearce.jgit/bin"
	cp="$cp:$jgit_home/org.spearce.jgit/lib/jsch-0.1.37.jar"
	cp="$cp:$jgit_home/org.spearce.jgit.pgm/bin"
	cp="$cp:$jgit_home/org.spearce.jgit.pgm/lib/args4j-2.0.9.jar"
	unset jgit_home
	java_args=
fi

if [ -n "$JGIT_CLASSPATH" ]
then
	cp="$cp:$JGIT_CLASSPATH"
fi

# Cleanup paths for Cygwin.
#
case "`uname`" in
CYGWIN*)
	cp=`cygpath --windows --mixed --path "$cp"`
	;;
Darwin)
	if test -e /System/Library/Frameworks/JavaVM.framework
	then
		java_args='
			-Dcom.apple.mrj.application.apple.menu.about.name=JGit
			-Dcom.apple.mrj.application.growbox.intrudes=false
			-Dapple.laf.useScreenMenuBar=true
			-Xdock:name=JGit
		'
	fi
	;;
esac

CLASSPATH="$cp"
export CLASSPATH

java=java
if test -n "$JAVA_HOME"
then
	java="$JAVA_HOME/bin/java"
fi

exec "$java" $java_args org.spearce.jgit.pgm.Main "$@"
exit 1
