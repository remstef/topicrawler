#!/bin/bash

VERSION=0.4.0d
DIR="topicrawler-${VERSION}"

mkdir -p ${DIR}
cd ${DIR}

# download heritrix if dir doesn't exist
if [ ! -e "heritrix-3.2.0" ]; then
    wget http://builds.archive.org/maven2/org/archive/heritrix/heritrix/3.2.0/heritrix-3.2.0-dist.tar.gz
    tar -xvzf heritrix-*.tar.gz
fi


# download ltbot if doesn't exist
if [ ! -e "lt.ltbot-${VERSION}-dist.tar.gz" ]; then
    wget https://github.com/tudarmstadt-lt/topicrawler/releases/download/v${VERSION}/lt.ltbot-${VERSION}-dist.tar.gz
fi

tar -xvzf lt.ltbot-*.tar.gz --strip-components 1 -C heritrix-3.2.0









