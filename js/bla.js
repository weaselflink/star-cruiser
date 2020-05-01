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
            var state = JSON.parse(event.data);
            var ship = state.ships[0];

            document.getElementById("pos").innerHTML = event.data;

            var canvas = document.getElementById("canvas")
            var ctx = canvas.getContext("2d");
            ctx.resetTransform();

            ctx.clearRect(0, 0, canvas.width, canvas.height);

            ctx.beginPath();
            ctx.translate(parseFloat(ship.position.x) + 400.0, parseFloat(ship.position.y) + 400.0);
            ctx.rotate(parseFloat(ship.rotation));
            ctx.moveTo(-5, -5);
            ctx.lineTo(10, 0);
            ctx.lineTo(-5, 5);
            ctx.lineTo(-2, 0);
            ctx.lineTo(-5, -5);
            ctx.stroke();
        }
    }

    commandSocket = createSocket("/command");
});

document.addEventListener("keydown", function(event) {
    if (commandSocket) {
        commandSocket.send(event.code)
    }
});
