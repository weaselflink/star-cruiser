const wsBaseUri = "ws://127.0.0.1:8080/ws";

let clientSocket = null;

function createSocket(uri) {
    let socket = null;
    const wsUri = wsBaseUri + uri;
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
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
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
    const r = ctx.canvas.width / 2 - 20;
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (let i = 0; i < 36; i++) {
        const a = i * Math.PI * 2 / 36;
        let inner = r - 10;
        if (i % 3 === 0) {
            inner = r - 20;
        }
        ctx.beginPath()
        ctx.moveTo(Math.sin(a) * inner, Math.cos(a) * inner);
        ctx.lineTo(Math.sin(a) * r, Math.cos(a) * r);
        ctx.stroke();
    }
}

function drawShipSymbol(ctx, rot) {
    ctx.rotate(-rot);
    ctx.moveTo(-5, -5);
    ctx.lineTo(10, 0);
    ctx.lineTo(-5, 5);
    ctx.lineTo(-2, 0);
    ctx.lineTo(-5, -5);
    ctx.stroke();
}

function drawShip(ctx, ship) {
    const rot = parseFloat(ship.rotation);

    ctx.resetTransform();
    ctx.strokeStyle = "#1e90ff"
    ctx.beginPath();
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    drawShipSymbol(ctx, rot);
}

function drawContact(ctx, ship, contact) {
    const xPos = parseFloat(contact.position.x) - parseFloat(ship.position.x);
    const yPos = parseFloat(contact.position.y) - parseFloat(ship.position.y);
    const rot = parseFloat(contact.rotation);

    ctx.resetTransform();
    ctx.strokeStyle = "#333"
    ctx.beginPath();
    ctx.translate(ctx.canvas.width / 2 + xPos, ctx.canvas.height / 2 - yPos);
    drawShipSymbol(ctx, rot);
}

function drawHistory(ctx, ship) {
    const xPos = parseFloat(ship.position.x) + ctx.canvas.width / 2;
    const yPos = -parseFloat(ship.position.y) + ctx.canvas.height / 2;

    ctx.resetTransform();
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (let point of ship.history) {
        const xp = parseFloat(point.second.x) - xPos;
        const yp = -parseFloat(point.second.y) - yPos;
        ctx.fillStyle = "#fff";
        ctx.beginPath();
        ctx.fillRect(xp + ctx.canvas.width / 2, yp + ctx.canvas.height / 2, 1, 1)
    }
}

function keyHandler(event) {
    if (clientSocket) {
        switch(event.code) {
            case "KeyP":
                clientSocket.send(JSON.stringify({
                    type: "de.bissell.starcruiser.Command.CommandTogglePause"
                }));
                break;
            case "KeyW":
                clientSocket.send(JSON.stringify({
                    type: "de.bissell.starcruiser.Command.CommandChangeThrottle",
                    diff: 10
                }));
                break;
            case "KeyS":
                clientSocket.send(JSON.stringify({
                    type: "de.bissell.starcruiser.Command.CommandChangeThrottle",
                    diff: -10
                }));
                break;
            case "KeyA":
                clientSocket.send(JSON.stringify({
                    type: "de.bissell.starcruiser.Command.CommandChangeRudder",
                    diff: -10
                }));
                break;
            case "KeyD":
                clientSocket.send(JSON.stringify({
                    type: "de.bissell.starcruiser.Command.CommandChangeRudder",
                    diff: 10
                }));
                break;
        }
    }
}

document.addEventListener("DOMContentLoaded", function() {
    clientSocket = createSocket("/client");

    if (clientSocket) {
        clientSocket.onmessage = function (event) {
            const state = JSON.parse(event.data);
            const ship = state.ship;

            document.getElementById("heading").innerHTML = ship.heading;
            document.getElementById("velocity").innerHTML = ship.velocity;

            const canvas = document.getElementById("canvas");
            const ctx = canvas.getContext("2d");
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
            for (let contact of state.contacts) {
                drawContact(ctx, ship, contact);
            }
        }
    }
});

document.addEventListener("keydown", keyHandler);
