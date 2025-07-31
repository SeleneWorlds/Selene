# Selene Game Client and Server

Extensible isometric game client and server developed in Kotlin with libGDX.

This repository is part of the [Selene](https://github.com/SeleneWorlds) project.

## Prerequisites

- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) or another IDE of your choice

## Getting Started

1. Open the project directory in IntelliJ IDEA
2. Confirm the Gradle import if prompted

### Setting up some Bundles for Testing

Normally, the launcher takes care of populating the client bundles. When launching the client from our IDE, 
it's up to us to set things up manually.

1. Create a folder called `bundles` at the root of the project
2. Place any bundles you want to load into the `bundles` folder

For example, to set up a Gobaith server, you could use the following bundles:
```bash
git clone https://github.com/SeleneWorlds/Moonlight.git moonlight
git clone https://github.com/SeleneWorlds/Illarion-Script-Loader.git illarion-script-loader
git clone https://github.com/SeleneWorlds/Illarion-API-Bridge.git illarion-api
git clone https://github.com/SeleneWorlds/Illarion-VBU-Scripts.git illarion-vbu
git clone https://github.com/SeleneWorlds/Illarion-Gobaith-Map.git illarion-gobaith-map
git clone https://github.com/SeleneWorlds/Illarion-Gobaith-Data.git illarion-gobaith-data
git clone https://github.com/SeleneWorlds/Illarion-Gobaith-UI.git illarion-gobaith-ui
```

Note: You will also need an `illarion-gobaith-assets` bundle, which cannot be redistributed 
due to licensing restrictions. We hope to provide an open source alternative in the future.
In the meantime, you'll need to build this bundle yourself. Feel free to ask for guidance within the community.

### Running the Server

A run configuration for IntelliJ is included. The server's working directory is located at `server/run`.

The server expects a `server.properties` file in its working directory.

```properties
save_path=save
bundles_path=../../bundles
port=8147
bundles=illarion-vbu,illarion-script-loader,illarion-gobaith-data,illarion-gobaith-map
```

### Running the Client

A run configuration for IntelliJ is included. The client's working directory is located at `client/run`.

The run configuration assumes you followed the example above. If you are using bundles that differ from the example,
you will need to update the bundle mappings in the launch arguments of the run configuration.