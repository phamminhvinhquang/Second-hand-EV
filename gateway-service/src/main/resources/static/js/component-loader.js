// File: /js/component-loader.js
document.addEventListener("DOMContentLoaded", async () => {
  try {
    // Dùng Promise.all để tải song song cả Navbar và Footer
    await Promise.all([
      loadComponent("/navbar.html", "navbar-placeholder"),
      loadComponent("/footer.html", "footer-placeholder"),
    ]);

    // Sau khi HTML đã chèn xong, phát sự kiện để các file JS logic (Notification, Chat...) bắt đầu chạy
    console.log("Components loaded. Dispatching event...");
    const event = new Event("componentsLoaded");
    document.dispatchEvent(event);
  } catch (error) {
    console.error("Error loading components:", error);
  }
});

// Hàm hỗ trợ tải file HTML
async function loadComponent(url, placeholderId) {
  const placeholder = document.getElementById(placeholderId);
  if (!placeholder) return;

  try {
    const response = await fetch(url);
    if (response.ok) {
      placeholder.innerHTML = await response.text();
    } else {
      console.error(`Failed to load ${url}: ${response.status}`);
    }
  } catch (err) {
    console.error(`Error fetching ${url}:`, err);
  }
}
