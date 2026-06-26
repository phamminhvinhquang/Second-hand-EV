// admin-revenue.js (Auto Sync + Fixed)
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "100px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});

const API_BASE = "/api/revenues";
const USER_SERVICE_BASE = "";

let labelsD = [],
  dataD = [],
  labelsM = [],
  dataM = [],
  labelsY = [],
  dataY = [];
let currentCharts = [];

function formatCurrency(v) {
  if (v == null) return "0 ₫";
  try {
    return Number(v).toLocaleString("vi-VN") + " ₫";
  } catch (e) {
    return String(v) + " ₫";
  }
}

function getAdminUserId() {
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) {
      const u = JSON.parse(userStr);
      let id = u?.userId || u?.id || null;

      // ✅ FIX LỖI: Nếu id bị dính format lạ kiểu "3:1", cắt lấy phần số đầu tiên
      if (id && String(id).includes(":")) {
        id = id.split(":")[0];
      }

      // Đảm bảo trả về số nguyên
      return parseInt(id);
    }
  } catch (e) {
    console.error("Lỗi parsing user ID:", e);
  }
  return null;
}

function getAuthHeaders(withJson = false) {
  const headers = {};
  if (withJson) headers["Content-Type"] = "application/json";
  try {
    const token = localStorage.getItem("token");
    if (token && token !== "null") headers["Authorization"] = `Bearer ${token}`;
  } catch (e) {}
  return headers;
}

function clearCharts() {
  currentCharts.forEach((chart) => {
    try {
      chart.destroy();
    } catch (e) {}
  });
  currentCharts = [];
}

function getRandomColor() {
  return `hsl(${Math.floor(Math.random() * 360)}, 70%, 70%)`;
}

/* ========== FETCH DATA ========== */
async function fetchData(userId) {
  const loadingEl = document.getElementById("loading");
  if (loadingEl) loadingEl.classList.remove("hidden");

  if (!userId) return;

  try {
    const url = `${API_BASE}/stats?userId=${userId}`;
    const res = await fetch(url, { headers: getAuthHeaders(false) }).catch(
      (err) => {
        throw new Error(
          "Không thể kết nối Server (8097). Hãy kiểm tra Docker."
        );
      }
    );

    if (res.status === 403) {
      alert("Truy cập bị từ chối (403)");
      return;
    }
    if (!res.ok) throw new Error(`HTTP Error ${res.status}`);

    const data = await res.json();
    renderData(data);
  } catch (error) {
    console.error("Fetch error:", error);
    if (loadingEl) loadingEl.textContent = `Lỗi: ${error.message}`;
  }
}

function renderData(data) {
  labelsD = (data.dailyList || []).map((r) => r.date).reverse();
  dataD = (data.dailyList || []).map((r) => r.total || 0).reverse();

  labelsM = (data.monthlyList || []).map((r) => r.label).reverse();
  dataM = (data.monthlyList || []).map((r) => r.total || 0).reverse();

  labelsY = (data.yearlyList || []).map((r) => r.year).reverse();
  dataY = (data.yearlyList || []).map((r) => r.total || 0).reverse();

  const renderTable = (list, tableId) => {
    const tbody = document.getElementById(tableId).querySelector("tbody");
    tbody.innerHTML = "";
    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="2">Chưa có dữ liệu</td></tr>`;
      return;
    }
    list.forEach((r) => {
      const row = document.createElement("tr");
      const dateCell = r.date || r.label || r.year;
      row.innerHTML = `<td>${dateCell}</td><td>${formatCurrency(r.total)}</td>`;
      tbody.appendChild(row);
    });
  };

  renderTable(data.dailyList || [], "dailyTable");
  renderTable(data.monthlyList || [], "monthlyTable");
  renderTable(data.yearlyList || [], "yearlyTable");

  drawSelectedChart();
  const loadingEl = document.getElementById("loading");
  if (loadingEl) loadingEl.classList.add("hidden");
}

/* ========== AUTO SYNC (Thay cho Manual Sync) ========== */
async function triggerAutoSync(userId) {
  const loadingEl = document.getElementById("loading");
  if (loadingEl) {
    loadingEl.textContent = "Đang đồng bộ dữ liệu...";
    loadingEl.classList.remove("hidden");
  }

  try {
    // Gọi API sync để backend kéo dữ liệu mới nhất từ purchase-service
    const url = `${API_BASE}/sync-now?userId=${userId}`;
    await fetch(url, {
      method: "POST",
      headers: getAuthHeaders(true),
    });
    // Không cần alert thành công, cứ âm thầm chạy
  } catch (error) {
    console.warn("Auto sync failed (non-fatal):", error);
  }

  // Đợi 1.5 giây để RabbitMQ kịp xử lý dữ liệu vừa kéo về
  // await new Promise((r) => setTimeout(r, 1500));

  // Tải dữ liệu lên biểu đồ
  await fetchData(userId);
}

function drawSelectedChart() {
  clearCharts(); // Xóa biểu đồ cũ
  const chartType = document.getElementById("chartType").value;
  const chartTarget = document.getElementById("chartSelect").value;

  // 1. Ẩn nội dung của TẤT CẢ các phần (nhưng tiêu đề H2 vẫn hiện do nó nằm ngoài class .stats-content)
  document.querySelectorAll(".stats-content").forEach((el) => {
    el.classList.remove("active");
  });

  // 2. Chỉ hiện nội dung của phần đang được chọn
  // chartTarget sẽ là 'daily', 'monthly' hoặc 'yearly' -> cộng thêm 'Content' để khớp id trong HTML
  const contentId = chartTarget + "Content";
  const activeContent = document.getElementById(contentId);
  if (activeContent) {
    activeContent.classList.add("active");
  }

  // --- Phần cấu hình dữ liệu cho Chart.js giữ nguyên như cũ ---
  const chartMap = {
    daily: {
      labels: labelsD,
      data: dataD,
      id: "chartDaily",
      label: "Theo ngày",
    },
    monthly: {
      labels: labelsM,
      data: dataM,
      id: "chartMonthly",
      label: "Theo tháng",
    },
    yearly: {
      labels: labelsY,
      data: dataY,
      id: "chartYearly",
      label: "Theo năm",
    },
  };

  const sel = chartMap[chartTarget];
  const canvas = document.getElementById(sel.id);

  // Fix lỗi: Nếu canvas đang ẩn thì không vẽ được, nhưng logic class active ở trên đã xử lý việc hiện canvas rồi
  const ctx = canvas.getContext("2d");

  const isPie = chartType === "pie" || chartType === "doughnut";
  const dataset = {
    label: sel.label,
    data: sel.data,
    backgroundColor: isPie
      ? sel.labels.map(() => getRandomColor())
      : "rgba(73, 190, 100, 0.4)",
    borderColor: "#49be64",
    borderWidth: 1,
    fill: chartType === "line",
    tension: 0.4,
  };

  const chart = new Chart(ctx, {
    type: chartType,
    data: { labels: sel.labels, datasets: [dataset] },
    options: {
      responsive: true,
      plugins: {
        tooltip: {
          callbacks: {
            label: (ctx) => ctx.label + ": " + formatCurrency(ctx.raw),
          },
        },
      },
      scales: isPie
        ? {}
        : {
            y: {
              beginAtZero: true,
              ticks: { callback: (val) => formatCurrency(val) },
            },
          },
    },
  });
  currentCharts.push(chart);
}

function exportToExcel() {
  const chartType = document.getElementById("chartSelect").value;
  let tableId, filename, sheetName;
  if (chartType === "daily") {
    tableId = "dailyTable";
    filename = "DoanhThu_Ngay";
    sheetName = "Ngay";
  } else if (chartType === "monthly") {
    tableId = "monthlyTable";
    filename = "DoanhThu_Thang";
    sheetName = "Thang";
  } else if (chartType === "yearly") {
    tableId = "yearlyTable";
    filename = "DoanhThu_Nam";
    sheetName = "Nam";
  }

  const table = document.getElementById(tableId);
  if (!table) {
    alert("Không tìm thấy bảng!");
    return;
  }
  const wb = XLSX.utils.table_to_book(table, { sheet: sheetName });
  XLSX.writeFile(wb, filename + ".xlsx");
}

// Init
window.addEventListener("DOMContentLoaded", () => {
  const userId = getAdminUserId();
  if (userId) {
    document
      .getElementById("chartType")
      ?.addEventListener("change", drawSelectedChart);
    document
      .getElementById("chartSelect")
      ?.addEventListener("change", drawSelectedChart);
    document
      .getElementById("exportBtn")
      ?.addEventListener("click", exportToExcel);

    // TỰ ĐỘNG ĐỒNG BỘ VÀ TẢI DỮ LIỆU
    // triggerAutoSync(userId);
    fetchData(userId);
  } else {
    alert("Không tìm thấy User ID. Vui lòng đăng nhập lại.");
  }
});
