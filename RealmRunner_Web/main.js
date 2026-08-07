import { init, Terminal, FitAddon } from 'ghostty-web';
import { TelnetInput, TelnetOutput } from 'telnet-stream';
import { Buffer } from 'buffer';

const connectionDialog = document.getElementById('connection-dialog');
const connectionForm = document.getElementById('connection-form');
const terminalContainer = document.getElementById('terminal');

let term;
let fitAddon;
let ws;
let telnetInput;
let telnetOutput;

connectionForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const host = document.getElementById('host').value;
    const port = document.getElementById('port').value;

    console.log(`Connecting to ${host}:${port}`);
    connectionDialog.style.display = 'none';
    terminalContainer.style.display = 'block'; // Make terminal visible

    await initGhosttyTerminal();
    connectWebSocket(host, port);
});

async function initGhosttyTerminal() {
    await init();

    term = new Terminal({
        cursorBlink: true,
        fontSize: 14,
        fontFamily: 'Monaco, Menlo, "Courier New", monospace',
        theme: {
            background: '#1e1e1e',
            foreground: '#d4d4d4',
        },
        scrollback: 10000,
    });

    fitAddon = new FitAddon();
    term.loadAddon(fitAddon);

    term.open(terminalContainer);
    fitAddon.fit();
    window.addEventListener('resize', () => fitAddon.fit());

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

    telnetInput.on('negotiation', (command, option) => {
        console.log('Received negotiation: ', command, option);
        // Let's just agree to everything for now
        if (command === 'will') {
            telnetOutput.negotiate('do', option);
        } else if (command === 'do') {
            telnetOutput.negotiate('will', option);
        }
    });

    telnetInput.on('subnegotiation', (option, data) => {
        console.log('Received subnegotiation: ', option, data);
    });

    term.onResize((size) => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            const nawsBuffer = Buffer.alloc(4);
            nawsBuffer.writeUInt16BE(size.cols, 0);
            nawsBuffer.writeUInt16BE(size.rows, 2);
            telnetOutput.subnegotiate(31, nawsBuffer);
        }
    });
}

function connectWebSocket(host, port) {
    const protocol =  "ws:"; //window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${host}:${port}/`; // Assuming a path like /websocket for the MUD server

    ws = new WebSocket(wsUrl, ["telnet.mudstandards.org"]); // Add telnet subprotocol
    ws.binaryType = "arraybuffer";

    ws.onopen = () => {
        console.log('WebSocket connected');
        term.write('Connected to MUD server.\r\n');
    };

    // Data from the websocket to the terminal
    ws.onmessage = (event) => {
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