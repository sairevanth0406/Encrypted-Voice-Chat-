🛡️ Encrypted Voice Chat Application

A Java-based secure, real-time, multi-client audio communication system using AES encryption.

📌 Overview

The Encrypted Voice Chat Application is a real-time, multi-client audio communication system built using Java Sockets, AES encryption, multi-threading, and voice activity detection (VAD).
It enables multiple users to securely communicate over a local network through encrypted voice packets, supporting features like active speaker detection, mute/unmute control, and smooth client disconnection.

This project demonstrates core Computer Networks concepts including TCP socket programming, concurrency, encryption, and network fault tolerance.

🚀 Features
🔐 1. End-to-End AES Encryption

All audio packets are encrypted using AES before transmission, ensuring confidentiality.

🔊 2. Real-Time Audio Streaming

Supports low-latency microphone capture and playback using Java Sound API.

🧵 3. Multi-Threaded Architecture

Each client receives a dedicated server thread, enabling simultaneous communication.

🎤 4. Active Speaker UI

Shows who is speaking using an animated visual indicator.

🔇 5. Mute / Unmute Control

Users can instantly toggle their microphone without interrupting the connection.

🔌 6. Smooth Client Disconnection

Clients can exit gracefully at any time without causing server errors.

🌐 7. Network Fault Handling

Handles connection loss, abrupt exits, and blocked ports effectively.

📂 Project Structure
CN PROJECT
│
├── Server.java
├── ServerAudioConnection.class
├── ServerControlClient.class
├── Client.java
├── MixerUtils.java
├── SpeakerUI.java
├── FrameUtilsFrame.java
├── CryptoUtils.java
├── TestAudioLoopback.java
└── README.md

⚙️ How It Works
1. Server

Listens on two ports (audio + control).

Accepts multiple clients.

Creates a separate thread per client.

Broadcasts decrypted → encrypted → forwarded audio.

2. Client

Connects using:
java Client <server-ip> <audio-port> <password>

Derives AES key.

Sends encrypted microphone audio to server.

Plays decrypted incoming audio.

Supports mute/unmute and graceful shutdown.

🧪 Test Cases
✔️ TC01 — Connect Client to Server

Client successfully connects using TCP sockets, derives AES key, and initializes audio streams.

✔️ TC02 — Client Connecting From a Different Network

Connection fails due to private IP restrictions (10.x.x.x / 192.168.x.x).
System remains stable, and server shows no errors.

✔️ TC03 — Mute/Unmute + Active Speaker

UI updates correctly, microphone toggles instantly, and the Active Speaker window reflects the correct state.

✔️ TC04 — Smooth Client Disconnection

Client exits cleanly, all streams close properly, and server removes the client thread without any exception.

📋 Test Case Table
Test Case ID	Test Description	Expected Result	Status
TC01	Connect client to server	Client successfully establishes connection and joins the session	Pass
TC02	Client connecting from a different network	Connection fails due to private/local IP restrictions; server remains unaffected	Pass
TC03	Mute/Unmute functionality & Active Speaker response	UI updates correctly, microphone toggles instantly, and Active Speaker reflects speaking state	Pass
TC04	Smooth client disconnection	Client exits without errors; server handles disconnection gracefully	Pass
🛠️ Technologies Used

Java Sockets (TCP)

AES Encryption (javax.crypto)

Java Multi-threading

Java Sound API

Swing UI (Active Speaker Window)

▶️ How to Run
Start the Server
java Server 6000 password

Start the Client
java Client 127.0.0.1 6000 password abcd1234

Client Actions

Mute: Disables microphone transmission

Unmute: Resumes microphone transmission

Close UI / Ctrl+C: Graceful disconnection

❗ Limitations

Works only in the same LAN unless port forwarding or ngrok tunneling is used.

Requires microphone and speaker access.

🔮 Future Enhancements

GUI-based server dashboard

RSA-based key exchange

Group audio rooms

Chat logging & history

WAN connectivity via STUN/TURN
