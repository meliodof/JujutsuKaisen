function showToast(message, isError) {
    isError = isError || false;
    var container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    var toast = document.createElement('div');
    toast.className = 'toast-notification' + (isError ? ' error' : '');
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(function() { toast.remove(); }, 2500);
}


document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.modal').forEach(function(modal) {
        makeModalDraggable(modal);
    });

    var observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            mutation.addedNodes.forEach(function(node) {
                if (node.classList && node.classList.contains('modal')) {
                    makeModalDraggable(node);
                }
            });
        });
    });
    observer.observe(document.body, { childList: true, subtree: true });
});

function makeModalDraggable(modal) {
    var dialog = modal.querySelector('.modal-dialog');
    var header = modal.querySelector('.modal-header');
    if (!dialog || !header || dialog.dataset.draggable) return;
    dialog.dataset.draggable = 'true';

    var offsetX = 0, offsetY = 0, mouseX = 0, mouseY = 0;

    header.addEventListener('mousedown', function(e) {
        e.preventDefault();
        var rect = dialog.getBoundingClientRect();
        offsetX = rect.left;
        offsetY = rect.top;
        mouseX = e.clientX;
        mouseY = e.clientY;
        dialog.style.margin = '0';
        dialog.style.position = 'fixed';
        dialog.style.left = offsetX + 'px';
        dialog.style.top = offsetY + 'px';

        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    });

    function onMove(e) {
        var dx = e.clientX - mouseX;
        var dy = e.clientY - mouseY;
        dialog.style.left = (offsetX + dx) + 'px';
        dialog.style.top = (offsetY + dy) + 'px';
    }

    function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
    }
}

window.addEventListener('scroll', function() {
    var navbar = document.querySelector('.navbar');
    if (window.scrollY > 50) {
        navbar.classList.add('scrolled');
    } else {
        navbar.classList.remove('scrolled');
    }
});