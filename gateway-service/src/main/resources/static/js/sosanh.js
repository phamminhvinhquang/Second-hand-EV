(() => {
  
  // ========== config ==========
  const COMPARE_API_BASE = "/api/compares";
  const PRODUCT_SERVICE_BASE = "";
  const STORAGE_KEY = "compareSelections";
  const selections = [null, null];

  // ========== client cache ==========
  const CLIENT_COMPARE_CACHE_KEY = "compare_listings_cache_v1";

  function makeCacheStore() {
    try {
      const raw = sessionStorage.getItem(CLIENT_COMPARE_CACHE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch (e) {
      return {};
    }
  }

  function saveCacheStore(store) {
    try {
      sessionStorage.setItem(CLIENT_COMPARE_CACHE_KEY, JSON.stringify(store));
    } catch (e) {
      /* ignore */
    }
  }

  function makeClientCacheKey(params) {
    if (params instanceof URLSearchParams) {
      const keys = Array.from(params.keys()).sort();
      return keys.map((k) => `${k}=${params.get(k)}`).join("&");
    } else {
      return Object.keys(params)
        .sort()
        .map((k) => `${k}=${params[k]}`)
        .join("&");
    }
  }

  function clientCacheGet(key, ttlMs = 3 * 60 * 1000) {
    const store = makeCacheStore();
    const entry = store[key];
    if (!entry) return null;
    if (Date.now() - entry.ts > ttlMs) {
      delete store[key];
      saveCacheStore(store);
      return null;
    }
    return entry.value;
  }

  function clientCacheSet(key, value) {
    const store = makeCacheStore();
    store[key] = { ts: Date.now(), value: value };
    saveCacheStore(store);
  }

  // ========== persist selections ==========
  function saveSelectionsToStorage() {
    try {
      const payload = {
        ts: Date.now(),
        selections: selections.map((s) => (s ? s : null)),
      };
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch (err) {
      console.warn("saveSelectionsToStorage failed:", err);
    }
  }

  function loadSelectionsFromStorage() {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      const loaded =
        parsed && Array.isArray(parsed.selections)
          ? parsed.selections
          : parsed || null;
      if (!loaded || !Array.isArray(loaded)) return;
      for (let i = 0; i < selections.length; i++) {
        const item = loaded[i];
        selections[i] = item && Object.keys(item).length ? item : null;
      }
    } catch (err) {
      console.warn("loadSelectionsFromStorage failed:", err);
    }
  }

  // ========== helpers ==========
  function escapeHtml(s) {
    if (!s) return "";
    return s.replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        }[c])
    );
  }

  function resolveImageUrl(url) {
    if (!url) return "/placeholder-100x70.png";
    const s = url.toString().trim();
    if (s.startsWith("http://") || s.startsWith("https://")) return s;
    if (s.startsWith("//")) return window.location.protocol + s;
    if (s.startsWith("/")) return PRODUCT_SERVICE_BASE.replace(/\/$/, "") + s;
    return PRODUCT_SERVICE_BASE.replace(/\/$/, "") + "/" + s;
  }

  function setImageWithFallback(imgEl, url) {
    if (!imgEl) return;
    imgEl.src = resolveImageUrl(url);
    imgEl.loading = "lazy";
    imgEl.addEventListener(
      "error",
      () => {
        imgEl.src = "/placeholder-100x70.png";
        imgEl.style.objectFit = "contain";
      },
      { once: true }
    );
  }

  function formatMoney(n) {
    if (n == null) return "";
    return Number(n).toLocaleString("vi-VN");
  }
  function formatNumber(n) {
    if (n == null) return "";
    return Number(n).toLocaleString("vi-VN");
  }

  // ========== UI helpers ==========
  function getModalEl() {
    return (
      document.getElementById("compare-modal") ||
      document.querySelector(".compare-modal")
    );
  }

  function openCompareModal(slotIndex) {
    const modal = getModalEl();
    if (!modal) return;
    modal.classList.add("show");
    modal.setAttribute("aria-hidden", "false");
    modal.dataset.slot = slotIndex;
    const activeTab = modal.querySelector(".type-tab.active");
    const type = activeTab
      ? activeTab.dataset.type
      : modal.querySelector(".type-tab")?.dataset.type || "oto";
    loadListings(type);
  }

  function closeCompareModal() {
    const modal = getModalEl();
    if (!modal) return;
    modal.classList.remove("show");
    modal.setAttribute("aria-hidden", "true");
    const container =
      modal.querySelector("#compare-listings") ||
      document.getElementById("compare-listings");
    if (container) container.innerHTML = '<p class="loading">Chưa tải</p>';
  }

  // ========== rendering listing list ==========
  async function loadListings(type) {
    const modal = getModalEl();
    if (!modal) return;
    const container =
      modal.querySelector("#compare-listings") ||
      document.getElementById("compare-listings");
    if (!container) return;
    container.innerHTML = '<p class="loading">Đang tải...</p>';

    const params = new URLSearchParams();
    if (type) params.set("type", type);
    params.set("limit", "50");
    const cacheKey = makeClientCacheKey(params);

    const cached = clientCacheGet(cacheKey);
    if (cached && Array.isArray(cached)) {
      renderListingList(cached, container);
      return;
    }

    try {
      let res = await fetch(
        `${COMPARE_API_BASE}/listings?type=${encodeURIComponent(type)}&limit=50`
      );
      if (!res.ok) throw new Error(`Lỗi khi lấy danh sách (${res.status})`);
      let list = await res.json();

      // Fallback logic: nếu list rỗng, gọi lấy tất cả và filter client
      if (!list || list.length === 0) {
        const fallbackRes = await fetch(
          `${COMPARE_API_BASE}/listings?limit=100`
        );
        if (fallbackRes.ok) {
          const all = await fallbackRes.json();
          list = (all || []).filter((l) => {
            const prod = l.product || {};
            const prodType = (prod.productType || "").toString().toLowerCase();
            const spec = prod.specification || {};
            const brand = (spec.brand || "").toString().toLowerCase();
            const name = (prod.productName || "").toString().toLowerCase();
            return typeMatchesClient(type, prodType, brand, name, spec);
          });
        } else {
          throw new Error(`Fallback lỗi (${fallbackRes.status})`);
        }
      }
      clientCacheSet(cacheKey, list);
      renderListingList(list, container);
    } catch (err) {
      container.innerHTML = `<p class="error">Không thể tải dữ liệu: ${escapeHtml(
        err.message
      )}</p>`;
      console.error("loadListings error:", err);
    }
  }

  function typeMatchesClient(requestedType, prodType, brand, name, spec) {
    if (!requestedType) return true;
    const r = requestedType.toString().toLowerCase();

    const p = (prodType || "").toLowerCase();
    const n = (name || "").toLowerCase();

    // Helper checks
    const isCar = (s) =>
      s.includes("car") ||
      s.includes("oto") ||
      s.includes("ô tô") ||
      s.includes("vf") ||
      s.includes("sedan") ||
      s.includes("suv");
    const isMotor = (s) =>
      s.includes("motor") ||
      s.includes("xemay") ||
      s.includes("xe máy") ||
      s.includes("scooter");
    const isBike = (s) =>
      s.includes("bicycle") ||
      s.includes("xedap") ||
      s.includes("xe đạp") ||
      s.includes("bike");
    const isBattery = (s) => s.includes("pin") || s.includes("battery");

    // --- LỌC Ô TÔ ---
    if (r.includes("oto") || r.includes("ô tô") || r.includes("car")) {
      // Positive: Type hoặc Name có chứa từ khóa ô tô
      const positive = isCar(p) || isCar(n);
      // Negative: TUYỆT ĐỐI KHÔNG chứa từ khóa xe máy/xe đạp/motor
      const negative =
        isMotor(p) || isMotor(n) || p.includes("bike") || n.includes("bike");

      return positive && !negative;
    }

    // --- LỌC XE MÁY ---
    if (r.includes("xemay") || r.includes("xe máy") || r.includes("motor")) {
      return isMotor(p) || isMotor(n);
    }

    // --- LỌC XE ĐẠP ---
    if (r.includes("xedap") || r.includes("xe đạp") || r.includes("bicycle")) {
      const positive = isBike(p) || isBike(n);
      // Xe đạp thì không được chứa "motor" hay "máy" (motorbike)
      const negative =
        isMotor(p) || isMotor(n) || p.includes("may") || n.includes("may");
      return positive && !negative;
    }

    // --- LỌC PIN ---
    if (r.includes("pin") || r.includes("battery")) {
      return isBattery(p) || isBattery(n);
    }

    return false;
  }

  function renderListingList(list, container) {
    if (!list || list.length === 0) {
      container.innerHTML = `<p>Không có mục nào.</p>`;
      return;
    }
    container.innerHTML = "";
    list.forEach((item) => {
      const product = item.product || {};
      const imgUrl =
        product.images && product.images.length
          ? product.images[0].imageUrl
          : product.imageUrl
          ? product.imageUrl
          : null;
      const el = document.createElement("div");
      el.className = "listing-item";
      el.innerHTML = `
        <img class="listing-thumb" alt="${escapeHtml(
          product.productName || ""
        )}" />
        <div class="meta">
          <div class="name"><strong>${escapeHtml(
            product.productName || "Không tên"
          )}</strong></div>
          <div class="price">${
            product.price ? formatMoney(product.price) + " VND" : ""
          }</div>
          <div class="desc">${escapeHtml(
            product.description ? product.description.slice(0, 120) : ""
          )}</div>
        </div>
        <div>
          <button class="select-btn" data-listing-id="${
            item.listingId || ""
          }" data-product-id="${product.productId || ""}">Chọn</button>
        </div>
      `;
      const imgEl = el.querySelector("img.listing-thumb");
      setImageWithFallback(imgEl, imgUrl);
      container.appendChild(el);
    });
  }

  // ========== select product ==========
  async function selectProductAndRender(productId, listingId, slotIndex) {
    if (!productId) return;
    try {
      const res = await fetch(
        `${COMPARE_API_BASE}/product-detail/${productId}`
      );
      if (!res.ok)
        throw new Error(`Lỗi khi lấy chi tiết sản phẩm (${res.status})`);
      const detail = await res.json();
      if (slotIndex == null || slotIndex < 0 || slotIndex >= selections.length)
        slotIndex = 0;
      selections[slotIndex] = detail;
      saveSelectionsToStorage();
      updateCompareCardsUI();
      populateAccordionWithSelections();
    } catch (err) {
      alert("Lỗi: " + err.message);
    }
  }

  // ========== update choose-cards UI ==========
  function updateCompareCardsUI() {
    const cards = document.querySelectorAll(".choose-card");
    cards.forEach((card, idx) => {
      const detail = selections[idx];
      let imgWrap = card.querySelector(".img-wrap");
      if (!imgWrap) {
        imgWrap = document.createElement("div");
        imgWrap.className = "img-wrap";
        imgWrap.style.display = "none";
        card.prepend(imgWrap);
      }
      let imgNode = imgWrap.querySelector("img.compare-thumb");
      if (!imgNode) {
        imgNode = document.createElement("img");
        imgNode.className = "compare-thumb";
        imgNode.alt = "image";
        imgWrap.appendChild(imgNode);
      }
      let removeBtn = card.querySelector(".remove-btn");
      if (!removeBtn) {
        removeBtn = document.createElement("button");
        removeBtn.className = "remove-btn";
        removeBtn.innerHTML = "&times;";
        removeBtn.title = "Xóa";
        removeBtn.style.display = "none";
        card.appendChild(removeBtn);
      }
      let textNode = card.querySelector(".choose-text");
      if (!textNode) {
        textNode = document.createElement("div");
        textNode.className = "choose-text";
        card.appendChild(textNode);
      }
      let subNode = card.querySelector(".choose-sub");
      if (!subNode) {
        subNode = document.createElement("div");
        subNode.className = "choose-sub";
        if (textNode) textNode.insertAdjacentElement("afterend", subNode);
      }

      if (detail) {
        card.classList.add("selected");
        imgWrap.style.display = "flex";
        const url =
          detail.imageUrls && detail.imageUrls.length
            ? detail.imageUrls[0]
            : detail.image || null;
        if (imgNode) imgNode.style.objectFit = "contain";
        setImageWithFallback(imgNode, url);
        textNode.innerHTML = `<a href="/product_detail.html?id=${
          detail.productId
        }" class="product-link">${escapeHtml(
          detail.productName || "Không tên"
        )}</a>`;
        textNode.style.textAlign = "center";
        subNode.textContent = detail.price
          ? `${formatMoney(detail.price)} VND`
          : "";
        subNode.style.textAlign = "center";
        removeBtn.style.display = "inline-flex";
      } else {
        card.classList.remove("selected");
        imgWrap.style.display = "none";
        setImageWithFallback(imgNode, "/placeholder-220x130.png");
        textNode.textContent = "Lựa chọn xe";
        textNode.style.textAlign = "center";
        subNode.textContent = "";
        subNode.style.textAlign = "center";
        removeBtn.style.display = "none";
      }
      let plusBtn = card.querySelector(".choose-plus");
      if (!selections[idx] && !plusBtn) {
        plusBtn = document.createElement("button");
        plusBtn.className = "choose-plus";
        plusBtn.setAttribute("aria-label", "Thêm xe");
        plusBtn.innerHTML = '<i class="fa-solid fa-plus"></i>';
        card.appendChild(plusBtn);
      } else if (selections[idx] && plusBtn) plusBtn.remove();
    });
  }

  // ========== populate accordion ==========
  function populateAccordionWithSelections() {
    const accordion = document.querySelector(".accordion-list");
    if (!accordion) return;
    const fieldMap = [
      { key: "yearOfManufacture", label: "Năm sản xuất" },
      { key: "brand", label: "Thương hiệu" },
      { key: "mileage", label: "Quãng đường đã đi" },
      { key: "conditionName", label: "Tình trạng" },
      { key: "maxSpeed", label: "Tốc độ tối đa" },
      { key: "rangePerCharge", label: "Quãng đường một lần sạc đầy" },
      { key: "color", label: "Màu sắc" },
      { key: "batteryType", label: "Loại pin" },
      { key: "batteryCapacity", label: "Dung lượng pin" },
      { key: "chargeTime", label: "Thời gian đã sử dụng pin / Thời gian sạc" },
      { key: "chargeCycles", label: "Số lần sạc" },
      { key: "compatibleVehicle", label: "Tương thích với dòng xe nào" },
      { key: "warrantyPolicy", label: "Chính sách bảo hành" },
    ];
    accordion.innerHTML = "";
    fieldMap.forEach((field, idx) => {
      const item = document.createElement("div");
      item.className = "accordion-item";
      item.dataset.id = idx;
      const left = document.createElement("button");
      left.className = "accordion-toggle";
      left.type = "button";
      left.innerHTML = `<span>${escapeHtml(
        field.label
      )}</span><i class="chev">▾</i>`;
      item.appendChild(left);
      const panel = document.createElement("div");
      panel.className = "accordion-panel";
      panel.style.display = "block";
      panel.style.maxHeight = null;
      panel.style.padding = "0 20px";
      panel.style.background = null;
      const row = document.createElement("div");
      row.style.display = "flex";
      row.style.gap = "12px";
      row.style.padding = "12px 0";
      selections.forEach((sel) => {
        const col = document.createElement("div");
        col.style.flex = "1";
        col.style.minWidth = "150px";
        if (!sel) col.textContent = "-";
        else {
          let v = sel[field.key];
          if (field.key === "mileage" && v != null) v = formatNumber(v) + " km";
          if (
            (field.key === "batteryCapacity" ||
              field.key === "chargeCycles" ||
              field.key === "rangePerCharge") &&
            v != null
          )
            v = v + (field.key === "rangePerCharge" ? " km" : "");
          if (v == null) v = "-";
          col.textContent = v;
        }
        row.appendChild(col);
      });
      panel.appendChild(row);
      item.appendChild(panel);
      accordion.appendChild(item);
    });
  }

  // ========== event delegation ==========
  document.addEventListener("click", (e) => {
    const plus = e.target.closest(".choose-plus");
    if (plus) {
      e.preventDefault();
      const card = plus.closest(".choose-card");
      let index = Array.from(document.querySelectorAll(".choose-card")).indexOf(
        card
      );
      if (index < 0) index = 0;
      openCompareModal(index);
      return;
    }
    const remove = e.target.closest(".remove-btn");
    if (remove) {
      e.preventDefault();
      const card = remove.closest(".choose-card");
      const idx = Array.from(document.querySelectorAll(".choose-card")).indexOf(
        card
      );
      if (idx >= 0) {
        selections[idx] = null;
        saveSelectionsToStorage();
        updateCompareCardsUI();
        populateAccordionWithSelections();
      }
      return;
    }
    if (e.target.matches(".modal-close")) {
      closeCompareModal();
      return;
    }
    const typeTab = e.target.closest(".type-tab");
    if (typeTab) {
      const modal = getModalEl();
      if (!modal) return;
      modal
        .querySelectorAll(".type-tab")
        .forEach((t) => t.classList.remove("active"));
      typeTab.classList.add("active");
      const type = typeTab.dataset.type;
      loadListings(type);
      return;
    }
    const selectBtn = e.target.closest(".select-btn");
    if (selectBtn) {
      e.preventDefault();
      const productId = selectBtn.dataset.productId;
      const listingId = selectBtn.dataset.listingId;
      const modal = getModalEl();
      const slot =
        modal && modal.dataset.slot ? parseInt(modal.dataset.slot, 10) : 0;
      selectProductAndRender(productId, listingId, slot).then(() =>
        closeCompareModal()
      );
      return;
    }
    const accordionToggle = e.target.closest(".accordion-toggle");
    if (accordionToggle) {
      const item = accordionToggle.parentElement;
      const panel = item.querySelector(".accordion-panel");
      if (!panel) return;
      const isOpen = panel.style.maxHeight && panel.style.maxHeight !== "0px";
      if (!isOpen) {
        panel.style.maxHeight = panel.scrollHeight + "px";
        accordionToggle.classList.add("open");
        accordionToggle.setAttribute("aria-expanded", "true");
      } else {
        panel.style.maxHeight = null;
        accordionToggle.classList.remove("open");
        accordionToggle.setAttribute("aria-expanded", "false");
      }
      return;
    }
  });

  document.addEventListener("click", (e) => {
    const modal = getModalEl();
    if (!modal) return;
    if (e.target.matches(".compare-modal-backdrop")) closeCompareModal();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      const modal = getModalEl();
      if (modal && modal.classList.contains("show")) closeCompareModal();
    }
  });

  // ========== other UI ==========
  document.addEventListener("click", (e) => {
    const tab = e.target.closest(".compare-tabs .tab");
    if (!tab) return;
    document
      .querySelectorAll(".compare-tabs .tab")
      .forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    const target = tab.dataset.target;
    if (!target) return;
    const item = document.querySelector(`.accordion-item[data-id="${target}"]`);
    if (!item) return;
    const panel = item.querySelector(".accordion-panel");
    const toggleBtn = item.querySelector(".accordion-toggle");
    document
      .querySelectorAll(".accordion-panel")
      .forEach((p) => (p.style.maxHeight = null));
    document
      .querySelectorAll(".accordion-toggle")
      .forEach((t) => t.classList.remove("open"));
    if (panel) {
      panel.style.maxHeight = panel.scrollHeight + "px";
      if (toggleBtn) toggleBtn.classList.add("open");
      item.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  });

  window.addEventListener("load", () => {
    const navbar = document.querySelector(".navbar");
    document.body.style.paddingTop = "80px";
  });

  document.addEventListener("DOMContentLoaded", () => {
    loadSelectionsFromStorage();
    const container = document.querySelector(".choose-cards");
    if (container) {
      const existing = container.querySelectorAll(".choose-card");
      if (existing.length < 2) {
        for (let i = existing.length; i < 2; i++) {
          const div = document.createElement("div");
          div.className = "choose-card";
          div.dataset.card = i;
          div.innerHTML = `<button class="choose-plus" aria-label="Thêm xe"><i class="fa-solid fa-plus"></i></button><div class="choose-text">Lựa chọn xe</div>`;
          container.appendChild(div);
        }
      } else existing.forEach((c, idx) => c.setAttribute("data-card", idx));
    }
    updateCompareCardsUI();
    populateAccordionWithSelections();
    document.querySelectorAll(".choose-card").forEach((card) => {
      const text = card.querySelector(".choose-text");
      const sub = card.querySelector(".choose-sub");
      if (text) text.style.textAlign = "center";
      if (sub) sub.style.textAlign = "center";
    });
    const modal = getModalEl();
    if (modal) {
      const first = modal.querySelector(".type-tab");
      if (first && !modal.querySelector(".type-tab.active"))
        first.classList.add("active");
    }
  });
})();
