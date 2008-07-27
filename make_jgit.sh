#!/bin/sh

O=jgit
PLUGINS="
	org.spearce.jgit
	org.spearce.jgit.pgm
"
JARS="
	org.spearce.jgit/lib/jsch-0.1.37.jar
	org.spearce.jgit.pgm/lib/args4j-2.0.9.jar
"

if [ -n "$JAVA_HOME" ]
then
	PATH=$JAVA_HOME/bin:$PATH
fi

T=".temp$$.$O"
T_MF="$T.MF"
R=`pwd`

cleanup_bin() {
	rm -f $T $O+ $T_MF
	for p in $PLUGINS
	do
		rm -rf $p/bin2
	done
}

die() {
	cleanup_bin
	rm -f $O
	echo >&2 "$@"
	exit 1
}

cleanup_bin
rm -f $O

CLASSPATH=
for j in $JARS
do
	if [ -z "$CLASSPATH" ]
	then
		CLASSPATH="$R/$j"
	else
		CLASSPATH="$CLASSPATH:$R/$j"
	fi
done
export CLASSPATH

for p in $PLUGINS
do
	echo "Entering $p ..."
	(cd $p/src &&
	 mkdir ../bin2 &&
	 find . -name \*.java -type f |
	 xargs javac \
		-source 1.5 \
		-target 1.5 \
		-encoding UTF-8 \
		-g \
		-d ../bin2) || die "Building $p failed."
	CLASSPATH="$CLASSPATH:$R/$p/bin2"
done

echo Manifest-Version: 1.0 >$T_MF &&
echo Implementation-Title: jgit >>$T_MF &&
echo Implementation-Version: `git describe HEAD` >>$T_MF &&

sed s/@@use_self@@/1/ jgit.sh >$O+ &&
java org.spearce.jgit.pgm.build.JarLinkUtil \
	`for p in $JARS   ; do printf %s " -include $p"     ;done` \
	`for p in $PLUGINS; do printf %s " -include $p/bin2";done` \
	-file META-INF/services/org.spearce.jgit.pgm.TextBuiltin=org.spearce.jgit.pgm/src/META-INF/services/org.spearce.jgit.pgm.TextBuiltin \
	-file META-INF/MANIFEST.MF=$T_MF \
	>>$O+ &&
chmod 555 $O+ &&
mv $O+ $O &&
echo "Created $O." || die "Creating $O failed."

cleanup_bin
