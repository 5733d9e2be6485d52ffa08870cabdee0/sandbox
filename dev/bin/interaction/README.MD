# Shell scripts to interact with SmartEvents

Start by making a copy of the `dev/bin/config/interaction-config-example` file to `dev/bin/config/interaction` and modify it accordingly.  
This new file should not be pushed into the repository.

Then it's important to always have the bearer token set with:

```bash
./dev/bin/interaction/get-token.sh
```

And then call the others such as:

```bash
./dev/bin/interaction/get-all-bridges.sh
```

See other scripts in `dev/bin/interaction` folder for more interactions.

### Note

Some Operating Systems may not have `bash` as the default shell. You may therefore encounter issues with the `source` command used in these scripts.

You can either invoke the scripts with `bash` explicitly, e.g. `bash ./get-token.sh`, or configuring `bash` as the default shell.
