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

After clicking on one of the ship names on the left you should see a radar-like UI.
Try pressing W,A,S,D,P or clicking stuff.
