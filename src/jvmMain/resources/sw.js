console.log("SW ENTRY")

self.addEventListener('install', e => {
    console.log('Service worker installed');
});

self.addEventListener('activate', e => {
    console.log('Service worker activated');
});


self.addEventListener('push', e => {
    var payload = null;
    try {
        payload = JSON.parse(e.data.text());
    } finally {}

    console.log('Received a push message', e, payload);

    if (payload == null) return; // Don't know what to do with this

    e.waitUntil(
        self.registration.showNotification(payload.title, payload)
    );
});


self.addEventListener('notificationclick', e => {
    var action = e.action;
    var [type,...data] = e.notification.tag.split(":");
    console.log('Notification clicked', e, action, type, data);

    e.notification.close();

    switch (type) {
    case "DM": {
        var id = data[0];
        e.waitUntil(
            clients.openWindow(`/?ch=${id}`)
        );
    }; break;
    default: {
        e.waitUntil(
            skipWaiting()
        );
    }; break;
    }
});