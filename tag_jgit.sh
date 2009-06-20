#!/bin/sh

# Updates MANIFEST.MF files for EGit plugins.

v=$1
if [ -z "$v" ]
then
	echo >&2 "usage: $0 version"
	exit 1
fi

MF=$(git ls-files | grep META-INF/MANIFEST.MF)
MV=jgit-maven/jgit/pom.xml
ALL="$MF $MV"

replace() {
	version=$1

	perl -pi -e 's/^(Bundle-Version:).*/$1 '$version/ $MF
	perl -pi -e 's,^    <version>.*</version>,    <version>'$2'</version>,' $MV
}

replace $v $v
git commit -s -m "JGit $v" $ALL &&
c=$(git rev-parse HEAD) &&

replace $v.qualifier $v-SNAPSHOT &&
git commit -s -m "Re-add version qualifier suffix to $v" $ALL &&

echo &&
tagcmd="git tag -s -m 'JGit $v' v$v $c" &&
if ! eval $tagcmd
then
	echo >&2
	echo >&2 "Tag with:"
	echo >&2 "  $tagcmd"
	exit 1
fi || exit
