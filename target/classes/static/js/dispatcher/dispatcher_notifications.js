var notifLastId = 0;
var notifEventSource = null;

function initDispatcherNotifications() {
    var nav = document.querySelector('.nav-links');
    if (!nav || document.getElementById('notifBell')) return;

    var bell = document.createElement('div');
    bell.id = 'notifBell';
    bell.className = 'notif-bell-wrap';
    bell.innerHTML = '<button type="button" class="notif-bell" onclick="toggleNotifPanel()" title="Уведомления">'
        + '🔔 <span id="notifBadge" class="notif-badge d-none">0</span></button>'
        + '<div id="notifPanel" class="notif-panel d-none"></div>';
    var logout = nav.querySelector('.btn-logout');
    if (logout) nav.insertBefore(bell, logout);
    else nav.appendChild(bell);

    document.addEventListener('click', function(e) {
        if (!e.target.closest('.notif-bell-wrap')) {
            document.getElementById('notifPanel').classList.add('d-none');
        }
    });

    loadNotifications();
    refreshUnreadBadge();
    connectNotificationStream();
    setInterval(refreshUnreadBadge, 30000);
}

function connectNotificationStream() {
    if (notifEventSource) {
        notifEventSource.close();
    }
    notifEventSource = new EventSource('/api/dispatcher/notifications/stream');
    notifEventSource.addEventListener('notification', function(e) {
        try {
            var n = JSON.parse(e.data);
            onNewNotification(n);
        } catch (err) { /* ignore */ }
    });
    notifEventSource.onerror = function() {
        notifEventSource.close();
        setTimeout(connectNotificationStream, 5000);
    };
}

function onNewNotification(n) {
    if (n.id && n.id > notifLastId) notifLastId = n.id;
    refreshUnreadBadge();
    loadNotifications();
    showNotifToast(n.title, n.message);
    if (typeof loadTrips === 'function') loadTrips();
}

function showNotifToast(title, message) {
    var d = document.createElement('div');
    d.className = 'notif-toast';
    d.innerHTML = '<strong>' + escapeHtml(title) + '</strong><br>' + escapeHtml(message);
    document.body.appendChild(d);
    setTimeout(function() { d.classList.add('show'); }, 10);
    setTimeout(function() { d.remove(); }, 6000);
}

function escapeHtml(s) {
    if (!s) return '';
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

function refreshUnreadBadge() {
    fetch('/api/dispatcher/notifications/unread-count')
        .then(function(r) { return r.json(); })
        .then(function(d) {
            var badge = document.getElementById('notifBadge');
            if (!badge) return;
            if (d.count > 0) {
                badge.textContent = d.count > 99 ? '99+' : d.count;
                badge.classList.remove('d-none');
            } else {
                badge.classList.add('d-none');
            }
        });
}

function loadNotifications() {
    fetch('/api/dispatcher/notifications?limit=30')
        .then(function(r) { return r.json(); })
        .then(function(list) {
            if (list.length && list[0].id > notifLastId) {
                notifLastId = list[0].id;
            }
            var panel = document.getElementById('notifPanel');
            if (!panel) return;
            if (list.length === 0) {
                panel.innerHTML = '<div class="notif-empty">Нет уведомлений</div>';
                return;
            }
            panel.innerHTML = '<div class="notif-header">'
                + '<span>Уведомления</span>'
                + '<button type="button" class="btn btn-sm btn-link" onclick="markAllNotificationsRead()">Прочитать все</button>'
                + '</div>'
                + list.map(renderNotifItem).join('');
        });
}

function renderNotifItem(n) {
    var time = n.created_at ? new Date(n.created_at).toLocaleString('ru') : '';
    var unread = n.is_read ? '' : ' notif-unread';
    var icon = n.notification_type === 'MAINTENANCE_DUE' ? '🔧'
        : n.notification_type === 'TRIP_CANCEL' ? '🚨' : '📢';
    return '<div class="notif-item' + unread + '" onclick="markNotificationRead(' + n.id + ')">'
        + '<div class="notif-item-title">' + icon + ' ' + escapeHtml(n.title) + '</div>'
        + '<div class="notif-item-msg">' + escapeHtml(n.message) + '</div>'
        + '<div class="notif-item-time">' + time + '</div></div>';
}

function toggleNotifPanel() {
    var panel = document.getElementById('notifPanel');
    panel.classList.toggle('d-none');
    if (!panel.classList.contains('d-none')) loadNotifications();
}

function markNotificationRead(id) {
    fetch('/api/dispatcher/notifications/' + id + '/read', { method: 'PATCH' })
        .then(function() { refreshUnreadBadge(); loadNotifications(); });
}

function markAllNotificationsRead() {
    fetch('/api/dispatcher/notifications/read-all', { method: 'PATCH' })
        .then(function() { refreshUnreadBadge(); loadNotifications(); });
}

document.addEventListener('DOMContentLoaded', initDispatcherNotifications);
