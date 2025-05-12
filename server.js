const SocketServer = require('websocket').server;
const http = require('http');
const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args));
const { JWT } = require('google-auth-library');

const serviceAccount = require('D:\\6th Sem\\CN\\ProjectWork\\WebSocket\\a19202-d270a8720022.json');
const projectId = 'a19202';

const server = http.createServer((req, res) => { });
server.listen(3000, () => {
    console.log("Listening on port 3000...");
});

const wsServer = new SocketServer({
    httpServer: server,
    maxReceivedFrameSize: 10 * 1024 * 1024,
    maxReceivedMessageSize: 10 * 1024 * 1024
});

const connections = [];
const connectionTokenMap = new Map();

wsServer.on('request', (req) => {
    const connection = req.accept();
    console.log('New WebSocket connection');
    connections.push(connection);

    connection.on('message', async (mes) => {
        try {
            const data = JSON.parse(mes.utf8Data);

            // Register FCM token
            if (data.type === 'register_token') {
                connectionTokenMap.set(connection, data.token);
                console.log('FCM token registered:', data.token);
                return;
            }

            // Broadcast message to other clients and send FCM
            connections.forEach(async (element) => {
                if (element !== connection) {
                    element.sendUTF(mes.utf8Data);

                    const targetToken = connectionTokenMap.get(element);
                    if (targetToken) {
                        const notificationBody =
                            data.message ? data.message :
                                data.image ? "Sent an image" :
                                    data.audio ? "Sent an audio message" :
                                        data.video ? "Sent a video" :
                                            data.gif ? "Sent a GIF" :
                                                "New message";

                        const sender = data.name || "Unknown";

                        await sendPushNotification(targetToken, "New Message", notificationBody, sender);
                    }
                }
            });
        } catch (err) {
            console.error('Error handling message:', err.message);
        }
    });

    connection.on('close', () => {
        console.log('Connection closed');
        connectionTokenMap.delete(connection);
        connections.splice(connections.indexOf(connection), 1);
    });
});

async function getAccessToken() {
    const jwtClient = new JWT(
        serviceAccount.client_email,
        null,
        serviceAccount.private_key,
        ['https://www.googleapis.com/auth/firebase.messaging']
    );
    const tokens = await jwtClient.authorize();
    return tokens.access_token;
}

async function sendPushNotification(token, title, body, sender) {
    const accessToken = await getAccessToken();

    const message = {
        message: {
            token: token,
            data: {
                title: title,
                body: body,
                sender: sender || "Unknown"
            }
        }
    };

    try {
        const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(message)
        });

        const text = await response.text();

        try {
            const json = JSON.parse(text);
            console.log('FCM success:', json);
        } catch (e) {
            console.error('Non-JSON response from FCM:', text);
        }

    } catch (err) {
        console.error('Error sending FCM:', err);
    }
}
