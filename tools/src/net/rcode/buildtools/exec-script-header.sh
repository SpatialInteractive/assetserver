#!/bin/sh
thisfile="$0"

exec java -jar "$thisfile" "$@"
echo "ERROR: Could not execute JVM"
exit 1
#### Jar contents follow
