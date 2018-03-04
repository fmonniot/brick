#!/usr/bin/env bash

# Create the local bin directory if it's not already done
if [ ! -d $HOME/.bin ]; then
 mkdir $HOME/.bin
fi

# Update mill only if the desired version isn't cached
if [ ! -f $HOME/.bin/mill-$MILL_VERSION ]; then
    curl -L -o $HOME/.bin/mill-$MILL_VERSION https://github.com/lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION
    chmod +x $HOME/.bin/mill-$MILL_VERSION

    if [ -L $HOME/.bin/mill ]; then
        unlink $HOME/.bin/mill
    fi
    ln -s $HOME/.bin/mill-$MILL_VERSION $HOME/.bin/mill
fi