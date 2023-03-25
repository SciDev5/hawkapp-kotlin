console.log("SW ENTRY")

self.addEventListener('install', event => {
    console.log('Service worker installed');
});

self.addEventListener('activate', event => {
    console.log('Service worker activated');
});


self.addEventListener('push', function(event) {
  console.log('Received a push message', event);

  var payload = event.data ? JSON.parse(event.data.text()) : {};

  var title = payload.title || 'Notification Title';
  var body = payload.body || 'Notification Body';
  var tag = payload.tag || 'notification-tag';

  event.waitUntil(
    self.registration.showNotification(title, {
      body: body,
      tag: tag
    })
  );
});