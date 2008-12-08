#!/bin/sh

# Updates MANIFEST.MF files for EGit plugins.

v=$1
if [ -z "$v" ]
then
	echo >&2 "usage: $0 version"
	exit 1
fi

MF=$(git ls-files | grep META-INF/MANIFEST.MF)
FX=org.spearce.egit-feature/feature.xml
SX=org.spearce.egit-updatesite/site.xml
ALL="$MF $FX $SX"

replace() {
	version=$1

	perl -pi -e 's/^(Bundle-Version:).*/$1 '$version/ $MF
	perl -pi -e '
		if (/^.*version=/ && ++$f == 2) {
			s/^(.*version)=".*"/$1="'$version'"/
		}' $FX
	perl -pi -e '
		next if /^<\?xml/;
		s/(egit_).*?(.jar)/${1}'$version'$2/;
		s/(version)=".*?"/$1="'$version'"/;
		' $SX
}

replace $v
git commit -s -m "EGit $v" $ALL &&
c=$(git rev-parse HEAD) &&

replace $v.qualifier &&
git commit -s -m "Re-add version qualifier suffix to $v" $ALL &&

echo &&
tagcmd="git tag -s -m 'EGit $v' v$v $c" &&
if ! eval $tagcmd
then
	echo >&2
	echo >&2 "Tag with:"
	echo >&2 "  $tagcmd"
	exit 1
fi || exit
