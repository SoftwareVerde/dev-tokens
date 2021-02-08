#!/bin/bash

USER='root'
PASSWORD='6137cde4893c59f76f005a8123d8e8e6'
DATABASE='devtokens'
PORT='8337'
HOST='127.0.0.1'

QUERY="$1"

if [ -z "${QUERY}" ]; then
    mysql --binary-as-hex -u ${USER} -h ${HOST} -P${PORT} -p${PASSWORD} ${DATABASE}
else
    mysql --binary-as-hex -u ${USER} -h ${HOST} -P${PORT} -p${PASSWORD} ${DATABASE} -e "${QUERY}"
fi

