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
            document.getElementById("conn").innerHTML = "connected"
        }
        socket.onclose = function(e) {
            document.getElementById("conn").innerHTML = "disconnected"
            console.log("Connection closed (wasClean = " + e.wasClean + ", code = " + e.code + ", reason = '" + e.reason + "')");
            socket = null;
        }
    }

    return socket;
}

function clearCanvas(ctx) {
    ctx.resetTransform();
    ctx.fillStyle = "#000";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
}

function drawShip(ctx, ship) {
    var xPos = parseFloat(ship.position.x) + 400.0;
    var yPos = -parseFloat(ship.position.y) + 400.0;
    var rot = parseFloat(ship.rotation);

    ctx.resetTransform();
    ctx.strokeStyle = "#fff"
    ctx.beginPath();
    ctx.translate(xPos, yPos);
    ctx.rotate(-rot);
    ctx.moveTo(-5, -5);
    ctx.lineTo(10, 0);
    ctx.lineTo(-5, 5);
    ctx.lineTo(-2, 0);
    ctx.lineTo(-5, -5);
    ctx.stroke();
}

function drawHistory(ctx, ship) {
    var xPos = parseFloat(ship.position.x) + 400.0;
    var yPos = -parseFloat(ship.position.y) + 400.0;
    var rot = parseFloat(ship.rotation);

    ctx.resetTransform();
    for (point of ship.history) {
        var xp = parseFloat(point.second.x)
        var yp = -parseFloat(point.second.y)
        ctx.fillStyle = "#fff";
        ctx.beginPath();
        ctx.fillRect(xp + 400, yp + 400, 1, 1)
    }
}

document.addEventListener("DOMContentLoaded", function() {
    updateSocket = createSocket("/updates");

    if (updateSocket) {
        updateSocket.onmessage = function (event) {
            var state = JSON.parse(event.data);
            var ship = state.ships[0];

            document.getElementById("heading").innerHTML = ship.heading;
            document.getElementById("velocity").innerHTML = ship.velocity;

            var canvas = document.getElementById("canvas")
            var ctx = canvas.getContext("2d");
            ctx.resetTransform();

            clearCanvas(ctx);

            drawShip(ctx, ship);
            drawHistory(ctx, ship);
        }
    }

    commandSocket = createSocket("/command");
});

document.addEventListener("keydown", function(event) {
    if (commandSocket) {
        commandSocket.send(event.code)
    }
});
