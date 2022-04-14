# Shell scripts to interact with SmartEvents

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

Some Operating Systems may not have `bash` as the default shell. You may therefore encounter issues with the `source` command used in these scripts.

You can either invoke the scripts with `bash` explicitly, e.g. `bash ./get-token.sh`, or configuring `bash` as the default shell.
