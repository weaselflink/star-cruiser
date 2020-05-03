var wsBaseUri = "ws://127.0.0.1:8080/ws";

var clientSocket = null;

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
    ctx.fillStyle = "#333";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
}

function drawCompass(ctx) {
    ctx.resetTransform();
    ctx.fillStyle = "#000";
    ctx.beginPath();
    ctx.ellipse(ctx.canvas.width / 2, ctx.canvas.height / 2,
            ctx.canvas.width / 2 - 15, ctx.canvas.height / 2 - 15,
            0, 0, 2 * Math.PI);
    ctx.fill();

    ctx.strokeStyle = "#fff"
    var r = ctx.canvas.width / 2  - 20;
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (var i = 0; i < 36; i++) {
        var a = i * Math.PI * 2 / 36;
        var inner = r - 10;
        if (i % 3 == 0) {
            inner = r - 20;
        }
        ctx.beginPath()
        ctx.moveTo(Math.sin(a) * inner, Math.cos(a) * inner);
        ctx.lineTo(Math.sin(a) * r, Math.cos(a) * r);
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
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
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
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (point of ship.history) {
        var xp = parseFloat(point.second.x) - xPos;
        var yp = -parseFloat(point.second.y) - yPos;
        ctx.fillStyle = "#fff";
        ctx.beginPath();
        ctx.fillRect(xp + ctx.canvas.width / 2, yp + ctx.canvas.height / 2, 1, 1)
    }
}

document.addEventListener("DOMContentLoaded", function() {
    clientSocket = createSocket("/client");

    if (clientSocket) {
        clientSocket.onmessage = function (event) {
            var state = JSON.parse(event.data);
            var ship = state.ships[0];

            document.getElementById("heading").innerHTML = ship.heading;
            document.getElementById("velocity").innerHTML = ship.velocity;

            var canvas = document.getElementById("canvas")
            var ctx = canvas.getContext("2d");
            ctx.resetTransform();

            clearCanvas(ctx);
            drawCompass(ctx);

            ctx.resetTransform();
            ctx.beginPath();
            ctx.ellipse(ctx.canvas.width / 2, ctx.canvas.height / 2,
                    ctx.canvas.width / 2 - 17, ctx.canvas.height / 2 - 17,
                    0, 0, 2 * Math.PI);
            ctx.clip();

            drawShip(ctx, ship);
            drawHistory(ctx, ship);
        }
    }
});

document.addEventListener("keydown", function(event) {
    if (clientSocket) {
        clientSocket.send(event.code)
    }
});
