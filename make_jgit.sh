#!/bin/sh

PATH=$JAVA_HOME/bin:$PATH
O=jgit
T=".temp$$.$O"

rm -f $O
rm -rf $T $O+ org.spearce.jgit/bin2
cp org.spearce.jgit/lib/jsch-0.1.37.jar $T &&
mkdir org.spearce.jgit/bin2 &&
(cd org.spearce.jgit/src &&
 find . -name \*.java -type f |
 xargs javac \
	-source 1.5 \
	-target 1.5 \
	-g \
	-d ../bin2 \
	-cp ../lib/jsch-0.1.37.jar) &&
jar uf $T -C org.spearce.jgit/bin2 . &&
jar uf $T -C org.spearce.jgit META-INF &&
sed s/@@use_self@@/1/ jgit.sh >$O+ &&
cat $T >>$O+ &&
chmod 555 $O+ &&
mv $O+ $O &&
echo "Created $O." &&
rm -rf $T $O+ org.spearce.jgit/bin2
