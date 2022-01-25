#!/bin/sh

# from https://stackoverflow.com/a/13777424/9360757
function valid_ip(){
  local  ip=$1
  local  stat=1

  if [[ $ip =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    stat=0
  else
    stat=1
  fi
  return $stat
}