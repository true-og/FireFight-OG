#!/usr/bin/bash

# Fetch all submodule content.
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git submodule update --force --recursive --init
fi
