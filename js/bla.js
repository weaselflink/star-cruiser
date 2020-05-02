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

function drawCompass(ctx) {
    ctx.resetTransform();
    ctx.strokeStyle = "#fff"
    var r = ctx.canvas.width / 2  - 20;
    var mx = canvas.width / 2;
    var my = canvas.width / 2;
    for (var i = 0; i < 36; i++) {
        var a = i * Math.PI * 2 / 36;
        var inner = r - 10;
        if (i % 3 == 0) {
            inner = r - 20;
        }
        ctx.beginPath()
        ctx.moveTo(mx + Math.sin(a) * inner, my + Math.cos(a) * inner);
        ctx.lineTo(mx + Math.sin(a) * r, my + Math.cos(a) * r);
        ctx.stroke();
    }
}

function drawShip(ctx, ship) {
    var xPos = parseFloat(ship.position.x) + ctx.canvas.width / 2;
    var yPos = -parseFloat(ship.position.y) + ctx.canvas.height / 2;
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
    var xPos = parseFloat(ship.position.x) + ctx.canvas.width / 2;
    var yPos = -parseFloat(ship.position.y) + ctx.canvas.height / 2;
    var rot = parseFloat(ship.rotation);

    ctx.resetTransform();
    for (point of ship.history) {
        var xp = parseFloat(point.second.x)
        var yp = -parseFloat(point.second.y)
        ctx.fillStyle = "#fff";
        ctx.beginPath();
        ctx.fillRect(xp + ctx.canvas.width / 2, yp + ctx.canvas.height / 2, 1, 1)
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
            drawCompass(ctx);

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
