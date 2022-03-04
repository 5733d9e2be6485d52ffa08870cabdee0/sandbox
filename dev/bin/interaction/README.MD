# Shell scripts to interact with OpenBridge

Start by making a copy of the `environment-example` file called `environment` and modify it accordingly, it won't be pushed into the repository.

Then it's important to always have the bearer token set 

```shell

source get-token.sh

```

And then call the others such as 


```shell

./get-all-bridges.sh

```