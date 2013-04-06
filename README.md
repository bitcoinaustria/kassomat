kassomat
========

Bitcoin backend for the Metalab Kassomat

Testing
=======

to run/test this project do the following:

install: git, jvm-7, maven

install bitcoinj to local maven repo as described here - it is not provided in any maven repo for security reasons

https://code.google.com/p/bitcoinj/wiki/UsingMaven

git clone https://code.google.com/p/bitcoinj/ bitcoinj

cd bitcoinj

git reset --hard a9bd8631b904     # Force yourself to the 0.7 release

mvn install

now

git clone github...

mvn jetty:run -Dkassomat.env=test

wait until blockchain is up to date (2-5 mins)  - this should happen only once.

go to

http://localhost:8080/tester.html

to test quote reply, you need testnet coins to send test funds

http://tpfaucet.appspot.com/

http://testnet.mojocoin.com/
