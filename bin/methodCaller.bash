#!/usr/bin/env bash

# To make this easy to write, it's only intended to invoke from the
# root jsonproxy project directory.

GROOVY_SCRIPT="${0%.bash}.groovy"
[ -f  "$GROOVY_SCRIPT" ] || {
    echo "Where did '$GROOVY_SCRIPT' go to?" 1>&2
    exit 2
}

# Somehow adding a -D switch to this command breaks the JDBC lookups???Ho
groovy -cp /local/hsqldb/lib/hsqldb.jar:src/main/groovy:build/classes/java/main "$GROOVY_SCRIPT" "$@"
