const wsBaseUri = "ws://127.0.0.1:35667/ws";

let clientSocket = null;

function createSocket(uri) {
    let socket = null;
    const wsUri = wsBaseUri + uri;
    if ("WebSocket" in window) {
       socket = new WebSocket(wsUri);
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

function resizeCanvasToDisplaySize(canvas) {
   const width = canvas.clientWidth;
   const height = canvas.clientHeight;

   if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
   }
}

function clearCanvas(ctx) {
    ctx.resetTransform();
    ctx.fillStyle = "#333333";
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
}

function drawCompass(ctx) {
    const dim = Math.min(ctx.canvas.width, ctx.canvas.height);

    ctx.resetTransform();
    ctx.fillStyle = "#000";
    ctx.beginPath();
    ctx.ellipse(ctx.canvas.width / 2, ctx.canvas.height / 2,
            dim / 2 - 15, dim / 2 - 15,
            0, 0, 2 * Math.PI);
    ctx.fill();

    ctx.strokeStyle = "#fff"
    const r = dim / 2 - 20;
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
    const xPos = parseFloat(contact.relativePosition.x);
    const yPos = parseFloat(contact.relativePosition.y);
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

function UpdateAcknowledge(counter) {
    this.type = "de.bissell.starcruiser.Command.UpdateAcknowledge"
    this.counter = counter
}

function CommandTogglePause() {
    this.type = "de.bissell.starcruiser.Command.CommandTogglePause"
}

function CommandChangeThrottle(diff) {
    this.type = "de.bissell.starcruiser.Command.CommandChangeThrottle"
    this.diff = diff
}

function CommandChangeRudder(diff) {
    this.type = "de.bissell.starcruiser.Command.CommandChangeRudder"
    this.diff = diff
}

function keyHandler(event) {
    if (clientSocket) {
        switch(event.code) {
            case "KeyP":
                clientSocket.send(JSON.stringify(new CommandTogglePause()));
                break;
            case "KeyW":
                clientSocket.send(JSON.stringify(new CommandChangeThrottle(10)));
                break;
            case "KeyS":
                clientSocket.send(JSON.stringify(new CommandChangeThrottle(-10)));
                break;
            case "KeyA":
                clientSocket.send(JSON.stringify(new CommandChangeRudder(-10)));
                break;
            case "KeyD":
                clientSocket.send(JSON.stringify(new CommandChangeRudder(10)));
                break;
        }
    }
}

document.addEventListener("DOMContentLoaded", function() {
    resizeCanvasToDisplaySize(document.getElementById("canvas"));

    clientSocket = createSocket("/client");

    if (clientSocket) {
        clientSocket.onmessage = function (event) {
            const state = JSON.parse(event.data);
            const ship = state.snapshot.ship;

            document.getElementById("heading").innerHTML = ship.heading;
            document.getElementById("velocity").innerHTML = ship.velocity;

            const canvas = document.getElementById("canvas");
            const ctx = canvas.getContext("2d");
            ctx.resetTransform();

            clearCanvas(ctx);
            drawCompass(ctx);

            const dim = Math.min(ctx.canvas.width, ctx.canvas.height);

            ctx.resetTransform();
            ctx.beginPath();
            ctx.ellipse(ctx.canvas.width / 2, ctx.canvas.height / 2,
                    dim / 2 - 17, dim / 2 - 17,
                    0, 0, 2 * Math.PI);
            ctx.clip();

            drawShip(ctx, ship);
            drawHistory(ctx, ship);
            for (let contact of state.snapshot.contacts) {
                drawContact(ctx, ship, contact);
            }

            clientSocket.send(JSON.stringify(new UpdateAcknowledge(state.counter)));
        }
    }
});

document.addEventListener("keydown", keyHandler);

window.addEventListener("resize", function() {
    resizeCanvasToDisplaySize(document.getElementById("canvas"))
});
