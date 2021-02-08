# Dev Tokens v0.1.0


## Description


Dev Tokens is a application to facilitate accepting donations with Bitcoin Cash (BCH).  When donations
are received, the application automatically issues (custom) Dev Tokens (SLP) to the sender, which can
then be redeemed for perks.  These perks, and their associated exchange rate, are defined by the admin
running this application.

Donations received via this application are transferred to your destination donation address, so to
minimize risk exposure of the server being compromised.


## Getting Started


To build the application, run:

```
./scripts/make.sh
```

This command will run the gradle command to download dependencies and build the jar file, its
configuration, and run-scripts, located within the `out` directory.

Once the build has completed, from the `out` directory, run the init process which will generate a
private key for creating the Dev Token and prompt you to send a small amount of BCH to the new address.

If you already have an SLP token minted, you can skip run-init and run-genesis and configure the 
`server` properties within `conf/server.conf`.

```
cd out
./run-init.sh
```

After the init process is complete, be sure to update the configuration with the provided values.
Be sure to give your token a new name by modifying the `genesisToken` properties within `conf/server.conf`.
Once complete, generate the dev token with the run-genesis command.  The run-genesis command will generate
your new SLP token with the attributes you've specified in the configuration file.

```
vim conf/server.conf
./run-genesis.sh
```

You may now finilize any configuration changes within the `conf/server.conf` file, including your token
exchange rate, donation destination address (where received donations will be transferred to).  You may
also define static donation addresses via either Private Key (which will then be transferred to your
destination address) or public key (which will monitor the address but not transfer funds to the
destination address).  If running this application in conjunction with a flipstarter, it is recommended
you set the `server.staticDonationWatchedAddresses` to be the recipient address(es) of your flipstarter.

If you are providing perks to redeem with your tokens, you should configure the `conf/redemption-items.json`
file.  Items listed here are displayed at the bottom of the donation page.  When a user redeems tokens for
one of these items, an email is sent via the email client credentials configured within the server.conf file.

Once you're done configuring your application, you may run it via:

```
./run.sh
```

If you'd like to keep the application running as a daemon, you may run it via:

```
nohup ./run.sh >> logs/run.log &
```

By default, the server listens on port 8080.  From the `out` directory, the served files are located within
`www`.  Changes to existing files are updated live, without a server restart.  New assets require a server
reset before they may be served.

If you are looking at keeping the content of your donation page under version control, it is recommended to
make changes to the `www` from the root project directory instead of `out`, and then use
`./scripts/copy-www-data.sh` to push changes to the served content directory.

** CAUTION ** Be sure to back up your token private keys.  Additionally, running `./scripts/make.sh` will
destroy the `out` directory.  If making incremental changes to the java codebase, consider using
`./scripts/make-jar.sh` instead to preserve the existing `out` directory.


## Contact


Feel free to contact Software Verde, LLC at any appropriate softwareverde.com email address.
Generic enquiries may be directed to devtokens@softwareverde.com

