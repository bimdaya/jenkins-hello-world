#!/bin/sh
set -e

if [ -n "$SERVER_XML" ]
then
  echo ${SERVER_XML} > /usr/local/tomcat/conf/server.xml
fi

if [ -n "$CONTEXT_XML" ]
then
  echo ${CONTEXT_XML} > /usr/local/tomcat/conf/context.xml
fi

exec "$@
