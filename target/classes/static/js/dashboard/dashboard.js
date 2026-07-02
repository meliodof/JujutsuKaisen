Promise.all([
    fetch('/api/vehicles').then(r => r.json()),
    fetch('/api/drivers').then(r => r.json()),
    fetch('/api/routes').then(r => r.json()),
    fetch('/api/dispatcher/trips?date=' + new Date().toISOString().split('T')[0]).then(r => r.json())
]).then(function(data) {
    document.getElementById('totalVehicles').textContent = data[0].length;
    document.getElementById('totalDrivers').textContent = data[1].length;
    document.getElementById('totalRoutes').textContent = data[2].length;
    var trips = data[3];
    document.getElementById('todayTrips').textContent = trips.length;

    var scheduled = trips.filter(t => t.status === 'scheduled').length;
    var progress = trips.filter(t => t.status === 'in_progress').length;
    var completed = trips.filter(t => t.status === 'completed').length;
    var cancelled = trips.filter(t => t.status === 'cancelled').length;

    new Chart(document.getElementById('statusChart'), {
        type: 'doughnut',
        data: {
            labels: ['Запланирован', 'В пути', 'Завершён', 'Отменён'],
            datasets: [{ data: [scheduled, progress, completed, cancelled], backgroundColor: ['#3b82f6','#f59e0b','#16a34a','#ef4444'] }]
        }
    });

    var last7 = [];
    var labels = [];
    for (var i = 6; i >= 0; i--) {
        var d = new Date(); d.setDate(d.getDate() - i);
        labels.push(d.toLocaleDateString('ru', {weekday: 'short', day: 'numeric'}));
        last7.push(fetch('/api/dispatcher/trips?date=' + d.toISOString().split('T')[0]).then(r => r.json()));
    }
    Promise.all(last7).then(function(days) {
        new Chart(document.getElementById('tripsChart'), {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: 'Рейсов', data: days.map(d => d.length), backgroundColor: '#3b82f6' }] }
        });
    });
});