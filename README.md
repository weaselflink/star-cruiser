[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fatrox%2Fsync-dotenv%2Fbadge&style=flat-square)](https://actions-badge.atrox.dev/atrox/sync-dotenv/goto) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/weaselflink/star-cruiser?style=flat-square)

## Star Cruiser Crew Simulator

An experiment with a browser based game.

Client/Server communication is handled via websocket. 
All code ist written in Kotlin (client/server/shared).

### Preconditions

* Java 9+ (e.g. https://www.azul.com/downloads/zulu-community/)

### How to run

    ./gradlew :server:run
    
Then open the URL shown in the logs in a browser

    http://localhost:35667

Spawn a ship and click it, now you should see a radar-like UI.
Try pressing W,A,S,D,P or clicking stuff.

### Thanks to

Initial space ship model created by [niko-3d-models](https://niko-3d-models.itch.io). 
The model is from the [free sci-fi spaceships pack](https://niko-3d-models.itch.io/free-sc-fi-spaceships-pack).

Textures for sky box created with [Spacescape](http://wwwtyro.github.io/space-3d) 
(Source: https://github.com/petrocket/spacescape).
