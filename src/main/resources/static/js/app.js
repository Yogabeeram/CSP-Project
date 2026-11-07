document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-sidebar-toggle]").forEach((button) => {
        button.addEventListener("click", () => document.body.classList.toggle("sidebar-open"));
    });

    document.querySelectorAll("[data-confirm]").forEach((button) => {
        button.addEventListener("click", (event) => {
            if (!window.confirm(button.dataset.confirm || "Are you sure?")) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-auto-dismiss]").forEach((alert) => {
        window.setTimeout(() => {
            if (alert.isConnected) {
                bootstrap.Alert.getOrCreateInstance(alert).close();
            }
        }, 5000);
    });
});
