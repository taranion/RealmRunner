import { TelnetInput, TelnetOutput } from 'telnet-stream';

let ws;
let telnetInput;
let telnetOutput;

function prepareTelnet() {
    telnetInput = new TelnetInput();
    telnetOutput = new TelnetOutput();

    // Data from the terminal to the websocket
    term.onData((data) => {
        telnetOutput.write(data);
    });

    telnetOutput.on('data', (data) => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(data);
        }
    });

    telnetInput.on('data', (data) => {
        term.write(data);
    });

    telnetInput.on('command', (command) => {
        console.log('Received command: ', command);
    });
}

function connect(host, port) {
    const protocol = 'ws:';
    const wsUrl = `${protocol}//${host}:${port}/ws`; // Assuming a path like /websocket for the MUD server

    ws = new WebSocket(wsUrl, ["telnet.mudstandards.org"]); // Add telnet subprotocol
    ws.binaryType = "arraybuffer";

    ws.onopen = () => {
        console.log('WebSocket connected');
        term.write('Connected to MUD server.\r\n');
    };

    // Data from the websocket to the terminal
    ws.onmessage = (event) => {
        console.log('WebSocket message received:', event.data);
        if (typeof event.data === 'string') {
            telnetInput.write(event.data);
        } else if (event.data instanceof ArrayBuffer) {
            telnetInput.write(Buffer.from(event.data));
        } else if (event.data instanceof Blob) {
            event.data.arrayBuffer().then(buffer => {
                telnetInput.write(Buffer.from(buffer));
            });
        }
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        term.write(`WebSocket error: ${error.message}\r\n`);
    };

    ws.onclose = () => {
        console.log('WebSocket disconnected');
        term.write('Disconnected from MUD server. Attempting to reconnect...\r\n');
        // Simple reconnect logic (consider exponential backoff for production)
        setTimeout(() => connectWebSocket(host, port), 3000);
    };
}