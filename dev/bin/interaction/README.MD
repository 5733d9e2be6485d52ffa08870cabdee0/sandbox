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

### Note

If you are using Ubuntu the default shell is `dash` that does not support the `source` command.

You can either invoke the scripts with `bash` explicitly, e.g. `bash ./get-token.sh`, or configuring `dash` as the default shell.

See [here](https://unix.stackexchange.com/questions/442510/how-to-use-bash-for-sh-in-ubuntu/442517#442517).