var wsBaseUri = "ws://127.0.0.1:8080/ws";

var updateSocket = null;
var commandSocket = null;

function createSocket(uri) {
    var socket = null;
    var wsUri = wsBaseUri + uri
    if ("WebSocket" in window) {
       socket = new WebSocket(wsUri);
    } else if ("MozWebSocket" in window) {
       socket = new MozWebSocket(wsUri);
    } else {
       console.log("Browser does not support WebSocket!");
    }

    if (socket) {
        socket.onopen = function() {
            console.log("Connected");
        }
        socket.onclose = function(e) {
            console.log("Connection closed (wasClean = " + e.wasClean + ", code = " + e.code + ", reason = '" + e.reason + "')");
            socket = null;
        }
    }

    return socket;
}

document.addEventListener("DOMContentLoaded", function() {
    updateSocket = createSocket("/updates");

    if (updateSocket) {
        updateSocket.onmessage = function (event) {
            document.getElementById("pos").innerHTML = event.data
        }
    }

    commandSocket = createSocket("/command");
});

document.addEventListener("keydown", function(event) {
    if (commandSocket) {
        commandSocket.send(event.keyCode)
    }
});
