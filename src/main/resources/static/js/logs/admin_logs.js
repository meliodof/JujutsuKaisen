var allLogs = [];

function loadLogs() {
    fetch('/api/logs?limit=500')
        .then(r => r.json())
        .then(data => { allLogs = data; filterLogs(); });
}

function filterLogs() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var typeFilter = document.getElementById('typeFilter').value;

    var filtered = allLogs.filter(l => {
        var matchSearch = !search ||
            (l.username && l.username.toLowerCase().indexOf(search) !== -1) ||
            (l.entity_name && l.entity_name.toLowerCase().indexOf(search) !== -1) ||
            (l.details && l.details.toLowerCase().indexOf(search) !== -1);
        return matchSearch && (typeFilter === 'all' || l.operation_type === typeFilter);
    });

    var byType = {};
    filtered.forEach(l => {
        var type = l.operation_type || 'UNKNOWN';
        if (!byType[type]) byType[type] = [];
        byType[type].push(l);
    });

    var html = '';
    for (var type in byType) {
        html += '<h3>' + type + '</h3><table class="table"><thead><tr><th>№</th><th>Сущность</th><th>ID</th><th>Пользователь</th><th>Детали</th><th>Дата</th></tr></thead><tbody>';
        html += byType[type].map((l, i) =>
            '<tr><td>' + (i + 1) + '</td><td>' + (l.entity_name || '—') + '</td><td>' + (l.entity_id || '—') + '</td><td>' + (l.username || '—') + '</td><td>' + (l.details || '—') + '</td><td>' + new Date(l.created_at).toLocaleString() + '</td></tr>'
        ).join('');
        html += '</tbody></table>';
    }
    document.getElementById('tablesContainer').innerHTML = html || '<p class="empty-state">Нет записей</p>';
}

window.onload = loadLogs;