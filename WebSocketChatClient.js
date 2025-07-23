/**
 * 🚀 Ultra-Fast WebSocket Chat Client
 * Replaces slow API calls with instant WebSocket messaging
 */
class WebSocketChatClient {
    constructor(config = {}) {
        this.config = {
            apiBase: 'http://localhost:8081/api',
            wsUrl: 'http://localhost:8081/ws',
            autoReconnect: true,
            heartbeatInterval: 30000,
            ...config
        };
        
        this.stompClient = null;
        this.jwtToken = null;
        this.currentUser = null;
        this.subscriptions = new Map();
        this.isConnected = false;
        this.messageQueue = [];
        this.eventListeners = new Map();
        
        // Bind methods
        this.connect = this.connect.bind(this);
        this.disconnect = this.disconnect.bind(this);
        this.sendMessage = this.sendMessage.bind(this);
        this.joinChat = this.joinChat.bind(this);
        this.leaveChat = this.leaveChat.bind(this);
    }

    // ================================
    // 🔐 AUTHENTICATION & CONNECTION
    // ================================

    async login(phoneNumber, password) {
        try {
            const response = await fetch(`${this.config.apiBase}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ phoneNumber, password })
            });

            if (!response.ok) {
                throw new Error('Login failed');
            }

            const authData = await response.json();
            this.jwtToken = authData.token;

            // Get user info
            const userResponse = await fetch(`${this.config.apiBase}/users/me`, {
                headers: { 'Authorization': `Bearer ${this.jwtToken}` }
            });

            this.currentUser = await userResponse.json();
            this.emit('userLoggedIn', this.currentUser);

            return this.currentUser;
        } catch (error) {
            this.emit('error', { type: 'login', error });
            throw error;
        }
    }

    async connect() {
        if (!this.jwtToken) {
            throw new Error('Please login first');
        }

        return new Promise((resolve, reject) => {
            try {
                this.emit('connecting');

                // Load SockJS and STOMP if not already loaded
                this.loadDependencies().then(() => {
                    const socket = new SockJS(`${this.config.wsUrl}?token=${this.jwtToken}`);
                    this.stompClient = Stomp.over(socket);

                    // Disable debug for performance
                    this.stompClient.debug = null;

                    this.stompClient.connect({},
                        (frame) => {
                            this.isConnected = true;
                            this.emit('connected', frame);
                            
                            // Process queued messages
                            this.processMessageQueue();
                            
                            // Start heartbeat
                            this.startHeartbeat();
                            
                            resolve(frame);
                        },
                        (error) => {
                            this.isConnected = false;
                            this.emit('disconnected', error);
                            
                            if (this.config.autoReconnect) {
                                setTimeout(() => this.connect(), 5000);
                            }
                            
                            reject(error);
                        }
                    );
                });
            } catch (error) {
                reject(error);
            }
        });
    }

    disconnect() {
        if (this.stompClient) {
            this.subscriptions.clear();
            this.stompClient.disconnect();
            this.stompClient = null;
        }
        this.isConnected = false;
        this.emit('disconnected');
    }

    // ================================
    // 💬 REAL-TIME MESSAGING (FAST!)
    // ================================

    async joinChat(chatId) {
        try {
            // Leave previous chat
            this.leaveChat();

            // Subscribe to real-time messages - INSTANT! ⚡
            const subscription = this.stompClient.subscribe(`/topic/chat/${chatId}`, (message) => {
                const messageData = JSON.parse(message.body);
                this.emit('messageReceived', { chatId, message: messageData });
            });

            this.subscriptions.set(`chat_${chatId}`, subscription);

            // Load existing messages (one-time API call)
            const response = await fetch(`${this.config.apiBase}/chats/${chatId}/messages`, {
                headers: { 'Authorization': `Bearer ${this.jwtToken}` }
            });

            if (response.ok) {
                const messages = await response.json();
                this.emit('messagesLoaded', { chatId, messages });
            }

            this.emit('chatJoined', { chatId });
            return true;

        } catch (error) {
            this.emit('error', { type: 'joinChat', error, chatId });
            throw error;
        }
    }

    leaveChat(chatId = null) {
        if (chatId) {
            const subscription = this.subscriptions.get(`chat_${chatId}`);
            if (subscription) {
                subscription.unsubscribe();
                this.subscriptions.delete(`chat_${chatId}`);
            }
        } else {
            // Leave all chats
            this.subscriptions.forEach((subscription, key) => {
                if (key.startsWith('chat_')) {
                    subscription.unsubscribe();
                }
            });
            this.subscriptions.clear();
        }
    }

    // 🚀 INSTANT MESSAGE SENDING - No API delays!
    sendMessage(chatId, messageData) {
        const message = {
            type: messageData.type || 'TEXT',
            content: messageData.content || null,
            mediaUrl: messageData.mediaUrl || null
        };

        if (this.isConnected && this.stompClient) {
            // Send instantly via WebSocket! ⚡
            this.stompClient.send(`/app/chat/${chatId}/send`, {}, JSON.stringify(message));
            this.emit('messageSent', { chatId, message });
        } else {
            // Queue message if not connected
            this.messageQueue.push({ chatId, message });
            this.emit('messageQueued', { chatId, message });
        }
    }

    // 🚀 INSTANT FILE UPLOAD + SEND
    async sendFile(chatId, file) {
        try {
            this.emit('fileUploading', { chatId, file });

            // 1. Upload file
            const formData = new FormData();
            formData.append('file', file);

            const uploadResponse = await fetch(`${this.config.apiBase}/files/upload`, {
                method: 'POST',
                body: formData
            });

            if (!uploadResponse.ok) {
                throw new Error('File upload failed');
            }

            const uploadData = await uploadResponse.json();

            // 2. Send file message instantly via WebSocket! ⚡
            const messageData = {
                type: this.getFileType(file.type),
                content: null,
                mediaUrl: uploadData.fileUrl
            };

            this.sendMessage(chatId, messageData);
            this.emit('fileSent', { chatId, file, messageData });

            return uploadData;

        } catch (error) {
            this.emit('error', { type: 'fileUpload', error, chatId, file });
            throw error;
        }
    }

    // ================================
    // 🎯 CHAT MANAGEMENT (FAST!)
    // ================================

    async createChat(participantIds, chatType = 'INDIVIDUAL', name = null) {
        try {
            const response = await fetch(`${this.config.apiBase}/chats`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.jwtToken}`
                },
                body: JSON.stringify({
                    type: chatType,
                    name: name,
                    participantIds: participantIds
                })
            });

            if (!response.ok) {
                throw new Error('Failed to create chat');
            }

            const chat = await response.json();
            this.emit('chatCreated', chat);
            
            // Auto-join the new chat
            await this.joinChat(chat.id);
            
            return chat;

        } catch (error) {
            this.emit('error', { type: 'createChat', error });
            throw error;
        }
    }

    async getChats() {
        try {
            const response = await fetch(`${this.config.apiBase}/chats`, {
                headers: { 'Authorization': `Bearer ${this.jwtToken}` }
            });

            if (!response.ok) {
                throw new Error('Failed to get chats');
            }

            const chats = await response.json();
            this.emit('chatsLoaded', chats);
            return chats;

        } catch (error) {
            this.emit('error', { type: 'getChats', error });
            throw error;
        }
    }

    // ================================
    // 🔧 UTILITY & PERFORMANCE
    // ================================

    processMessageQueue() {
        while (this.messageQueue.length > 0) {
            const { chatId, message } = this.messageQueue.shift();
            this.sendMessage(chatId, message);
        }
    }

    startHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
        }

        this.heartbeatInterval = setInterval(() => {
            if (this.isConnected && this.stompClient) {
                try {
                    this.stompClient.send('/app/heartbeat', {}, '{}');
                } catch (error) {
                    console.warn('Heartbeat failed:', error);
                }
            }
        }, this.config.heartbeatInterval);
    }

    getFileType(mimeType) {
        if (mimeType.startsWith('image/')) return 'IMAGE';
        if (mimeType.startsWith('video/')) return 'VIDEO';
        if (mimeType.startsWith('audio/')) return 'AUDIO';
        return 'FILE';
    }

    async loadDependencies() {
        // Load SockJS and STOMP if not available
        if (typeof SockJS === 'undefined') {
            await this.loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js');
        }
        if (typeof Stomp === 'undefined') {
            await this.loadScript('https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js');
        }
    }

    loadScript(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    // ================================
    // 📡 EVENT SYSTEM
    // ================================

    on(event, callback) {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, []);
        }
        this.eventListeners.get(event).push(callback);
    }

    off(event, callback) {
        if (this.eventListeners.has(event)) {
            const listeners = this.eventListeners.get(event);
            const index = listeners.indexOf(callback);
            if (index > -1) {
                listeners.splice(index, 1);
            }
        }
    }

    emit(event, data) {
        if (this.eventListeners.has(event)) {
            this.eventListeners.get(event).forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`Error in event listener for ${event}:`, error);
                }
            });
        }
    }

    // ================================
    // 🧹 CLEANUP
    // ================================

    destroy() {
        this.disconnect();
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
        }
        this.eventListeners.clear();
        this.messageQueue = [];
    }
}

// ================================
// 🚀 USAGE EXAMPLES
// ================================

/*
// Basic Usage:
const chatClient = new WebSocketChatClient();

// Login and connect
await chatClient.login('+1234567890', 'password123');
await chatClient.connect();

// Join a chat for instant messaging
await chatClient.joinChat(1);

// Send messages instantly! ⚡
chatClient.sendMessage(1, {
    type: 'TEXT',
    content: 'Hello World!'
});

// Send files instantly! ⚡
const fileInput = document.getElementById('fileInput');
await chatClient.sendFile(1, fileInput.files[0]);

// Listen to events
chatClient.on('messageReceived', (data) => {
    console.log('New message:', data.message);
    displayMessage(data.message);
});

chatClient.on('connected', () => {
    console.log('Connected! Ready for real-time messaging!');
});

chatClient.on('error', (error) => {
    console.error('Chat error:', error);
});

// React Example:
const [messages, setMessages] = useState([]);
const [chatClient] = useState(() => new WebSocketChatClient());

useEffect(() => {
    chatClient.on('messageReceived', (data) => {
        setMessages(prev => [...prev, data.message]);
    });
    
    return () => chatClient.destroy();
}, []);

// Vue Example:
export default {
    data() {
        return {
            messages: [],
            chatClient: new WebSocketChatClient()
        }
    },
    mounted() {
        this.chatClient.on('messageReceived', (data) => {
            this.messages.push(data.message);
        });
    },
    beforeDestroy() {
        this.chatClient.destroy();
    }
}
*/

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebSocketChatClient;
}

// Global for browser
if (typeof window !== 'undefined') {
    window.WebSocketChatClient = WebSocketChatClient;
} 