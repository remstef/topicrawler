#!/bin/bash

###
# 
# Setup development environment
# 
###

# setup heritrix
git clone https://github.com/internetarchive/heritrix3.git
cd heritrix3
git checkout tags/3.2.0
# note, java 7 might be necessary
mvn clean install source:jar javadoc:jar -Dadditionalparam="-Xdoclint:none" -DskipTests

# install
# tar --strip 1 -xzvf lt.ltbot-0.4.0c-SNAPSHOT-dist.tar.gz -C heritrix-3.2.0
