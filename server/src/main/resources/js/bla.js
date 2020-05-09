const wsBaseUri = "ws://127.0.0.1:35667/ws";

let clientSocket = null;
let state = null;
let lastRender = 0;
let canvas = null;
let ctx = null;
let scopeRadius = 0;

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

function resizeCanvasToDisplaySize() {
    const width = window.innerWidth;
    const height = window.innerHeight;
    const dim = Math.min(width, height);

    if (canvas.width !== dim || canvas.height !== dim) {
         canvas.width = dim;
         canvas.height = dim;
    }

    canvas.style.left = ((width - dim) / 2) + "px";
    canvas.style.top = ((height - dim) / 2) + "px";
    canvas.style.width = dim + "px";
    canvas.style.height = dim + "px";
}

function clearCanvas() {
    ctx.resetTransform();
    ctx.fillStyle = "#333333";
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
}

function drawCompass() {
    const dim = Math.min(ctx.canvas.width, ctx.canvas.height);

    ctx.resetTransform();
    ctx.fillStyle = "#000";
    ctx.beginPath();
    ctx.ellipse(ctx.canvas.width / 2, ctx.canvas.height / 2,
            dim / 2 - 15, dim / 2 - 15,
            0, 0, 2 * Math.PI);
    ctx.fill();

    ctx.strokeStyle = "#fff"
    scopeRadius = dim / 2 - 20;
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (let i = 0; i < 36; i++) {
        const a = i * Math.PI * 2 / 36;
        let inner = scopeRadius - 10;
        if (i % 3 === 0) {
            inner = scopeRadius - 20;
        }
        ctx.beginPath()
        ctx.moveTo(Math.sin(a) * inner, Math.cos(a) * inner);
        ctx.lineTo(Math.sin(a) * scopeRadius, Math.cos(a) * scopeRadius);
        ctx.stroke();
    }
}

function drawShipSymbol(rot) {
    ctx.rotate(-rot);
    ctx.moveTo(-5, -5);
    ctx.lineTo(10, 0);
    ctx.lineTo(-5, 5);
    ctx.lineTo(-2, 0);
    ctx.lineTo(-5, -5);
    ctx.stroke();
}

function drawShip(ship) {
    const rot = parseFloat(ship.rotation);

    ctx.resetTransform();
    ctx.strokeStyle = "#1e90ff"
    ctx.beginPath();
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    drawShipSymbol(rot);
}

function drawContact(ship, contact) {
    const xPos = parseFloat(contact.relativePosition.x);
    const yPos = parseFloat(contact.relativePosition.y);
    const rot = parseFloat(contact.rotation);

    const dist = Math.sqrt(xPos * xPos + yPos * yPos);
    if (dist < scopeRadius - 10) {
        ctx.resetTransform();
        ctx.strokeStyle = "#333"
        ctx.beginPath();
        ctx.translate(ctx.canvas.width / 2 + xPos, ctx.canvas.height / 2 - yPos);
        drawShipSymbol(rot);
    }
}

function drawHistory(ship) {
    const xPos = parseFloat(ship.position.x);
    const yPos = -parseFloat(ship.position.y);

    ctx.resetTransform();
    ctx.translate(ctx.canvas.width / 2, ctx.canvas.height / 2);
    for (let point of ship.history) {
        const xp = parseFloat(point.second.x) - xPos;
        const yp = -parseFloat(point.second.y) - yPos;

        const dist = Math.sqrt(xp * xp + yp * yp);
        if (dist < scopeRadius - 10) {
            ctx.fillStyle = "#fff";
            ctx.beginPath();
            ctx.fillRect(xp, yp, 1, 1);
        }
    }
}

function UpdateAcknowledge(counter) {
    this.type = "de.bissell.starcruiser.Command.UpdateAcknowledge"
    this.counter = counter
}

function CommandTogglePause() {
    this.type = "de.bissell.starcruiser.Command.CommandTogglePause"
}

function CommandJoinShip(shipId) {
    this.type = "de.bissell.starcruiser.Command.CommandJoinShip",
    this.shipId = shipId
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

function updateInfo(ship) {
    const headingElement = document.getElementById("heading");
    const velocityElement = document.getElementById("velocity");
    if (ship) {
        headingElement.innerHTML = ship.heading;
        velocityElement.innerHTML = ship.velocity;
    } else {
        headingElement.innerHTML = "unknown";
        velocityElement.innerHTML = "unknown";
    }
}

function selectPlayerShip(event) {
    if (clientSocket) {
        clientSocket.send(JSON.stringify(new CommandJoinShip(event.target.getAttribute("id"))));
    }
}

function updatePlayerShips(state) {
    let playerShipsList = document.getElementById("playerShips");
    let listElements = playerShipsList.getElementsByTagName("li");

    let max = Math.max(state.snapshot.playerShips.length, listElements.length);

    for (let index = 0; index < max; index++) {
        if (index < state.snapshot.playerShips.length) {
            let playerShip = state.snapshot.playerShips[index];
            if (index < listElements.length) {
                let entry = listElements.item(index);
                if (entry.getAttribute("id") != playerShip.id) {
                    entry.setAttribute("id", playerShip.id);
                    entry.innerHTML = playerShip.name;
                }
            } else {
                let entry = document.createElement("li");
                entry.setAttribute("id", playerShip.id);
                entry.innerHTML = playerShip.name;
                entry.addEventListener("click", selectPlayerShip);
                playerShipsList.appendChild(entry);
            }
        } else {
            if (index < listElements.length) {
                let entry = listElements.item(index);
                entry.remove();
            }
        }
    }
}

function drawHelm(state) {
    if (!state) {
        return;
    }

    const ship = state.snapshot.ship;

    updateInfo(ship);
    updatePlayerShips(state);

    ctx.resetTransform();

    clearCanvas();
    drawCompass();

    if (ship) {
        drawShip( ship);
        drawHistory(ship);
    }

    for (let contact of state.snapshot.contacts) {
        drawContact(ship, contact);
    }
}

function step(time) {
    if (state) {
        const stateCopy = state;

        drawHelm(stateCopy);
    }

    window.requestAnimationFrame(step);
}

document.addEventListener("DOMContentLoaded", function() {
    canvas = document.getElementById("canvas");
    ctx = canvas.getContext("2d", { alpha: false });

    resizeCanvasToDisplaySize();

    clientSocket = createSocket("/client");

    if (clientSocket) {
        clientSocket.onmessage = function (event) {
            state = JSON.parse(event.data);

            clientSocket.send(JSON.stringify(new UpdateAcknowledge(state.counter)));
        }
    }

    document.addEventListener("keydown", keyHandler);

    window.requestAnimationFrame(step);
});

window.addEventListener('resize', resizeCanvasToDisplaySize);
