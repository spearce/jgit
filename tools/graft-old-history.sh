#!/bin/sh
#
# Downloads and grafts in the old JGit project history, before
# the project moved to eclipse.org.
#
# It is recommended that you DO NOT use this script on your main
# work repository, or that if you do, you remove the graft before
# attempting to push content to a remote repository.  Grafts cause
# the history traversal system to change behavior, which can break
# other algorithms that depend upon it.


URL=git://repo.or.cz/jgit.git
PRE=3a2dd9921c8a08740a9e02c421469e5b1a9e47cb
POST=046198cf5f21e5a63e8ec0ecde2ef3fe21db2eae

GIT_DIR=$(git rev-parse --git-dir) &&
grafts="$GIT_DIR/info/grafts" &&

if grep $PRE "$grafts" >/dev/null 2>/dev/null
then
  echo 'Graft already installed; doing nothing.' >&2
else
  git remote add old-jgit "$URL" &&
  git fetch old-jgit &&
  echo $POST $PRE >>"$GIT_DIR/info/grafts" &&
  echo 'Graft installed.' >&2
fi
