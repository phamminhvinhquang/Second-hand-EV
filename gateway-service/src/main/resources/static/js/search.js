// ------------------------Search --------------------
const SEARCH_API = "/api/searchs/search";
const CLIENT_SEARCH_CACHE_KEY = "search_cache_v1"; // đổi nếu schema thay đổi

function makeCacheStore() {
  try {
    const raw = sessionStorage.getItem(CLIENT_SEARCH_CACHE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (e) {
    return {};
  }
}

function saveCacheStore(store) {
  try {
    sessionStorage.setItem(CLIENT_SEARCH_CACHE_KEY, JSON.stringify(store));
  } catch (e) {
    /* ignore */
  }
}

function makeClientCacheKey(params) {
  // params: URLSearchParams or simple object -> produce deterministic key
  if (params instanceof URLSearchParams) {
    // sort keys for determinism
    const keys = Array.from(params.keys()).sort();
    return keys.map((k) => `${k}=${params.get(k)}`).join("&");
  } else {
    // object
    return Object.keys(params)
      .sort()
      .map((k) => `${k}=${params[k]}`)
      .join("&");
  }
}

function clientCacheGet(key, ttlMs = 3 * 60 * 1000) {
  // default 3 minutes
  const store = makeCacheStore();
  const entry = store[key];
  if (!entry) return null;
  const now = Date.now();
  if (now - entry.ts > ttlMs) {
    // expired
    delete store[key];
    saveCacheStore(store);
    return null;
  }
  return entry.value;
}

function clientCacheSet(key, value) {
  const store = makeCacheStore();
  store[key] = {
    ts: Date.now(),
    value: value,
  };
  saveCacheStore(store);
}

document.addEventListener("DOMContentLoaded", () => {
  const tabs = document.querySelectorAll(".filter-tab");
  const filterBtn = document.querySelector(".filter-btn");
  const mainSearchInput = document.querySelector(".main-search-bar input");
  const filterGroups = document.querySelectorAll(".filter-group");
  const resultsContainer = document.getElementById("filter-results");

  let activeType = (() => {
    const a = document.querySelector(".filter-tab.active");
    return a ? a.dataset.type : "battery";
  })();

  // helper: create option
  function createOption(text, value = "") {
    const o = document.createElement("option");
    o.value = value;
    o.textContent = text;
    return o;
  }

  // Years, price ranges, capacities, etc.
  const YEARS = (() => {
    const now = new Date().getFullYear();
    const r = [];
    for (let y = now; y >= 1975; y--) r.push(String(y));
    return r;
  })();

  const PRICE_RANGES = {
    car: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 1 tỷ", value: "0-1000000000" },
      { label: "1 tỷ - 5 tỷ", value: "1000000000-5000000000" },
      { label: "Trên 5 tỷ", value: "5000000000-10000000000000" },
    ],
    bike: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 10 triệu", value: "0-10000000" },
      { label: "10 triệu - 30 triệu", value: "10000000-30000000" },
      {
        label: "Trên 30 triệu",
        value: "30000000-10000000000000",
      },
    ],
    motorcycle: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 20 triệu", value: "0-20000000" },
      { label: "20 triệu - 50 triệu", value: "20000000-50000000" },
      {
        label: "Trên 50 triệu",
        value: "50000000-10000000000000",
      },
    ],
    battery: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 10 triệu", value: "0-10000000" },
      { label: "10 triệu - 100 triệu", value: "10000000-100000000" },
      {
        label: "Trên 100 triệu",
        value: "100000000-10000000000000",
      },
    ],
  };

  // MILEAGE_RANGES theo từng loại (car / bike / motorcycle)
  const MILEAGE_RANGES = {
    car: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 50000(km)", value: "0-50000" },
      { label: "50000 - 200000(km)", value: "50000-200000" },
      {
        label: "Trên 200000(km)",
        value: "200000-10000000000000",
      },
    ],
    bike: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 500(km)", value: "0-500" },
      { label: "500 - 2000(km)", value: "500-2000" },
      { label: "Trên 2000(km)", value: "2000-10000000000000" },
    ],
    motorcycle: [
      { label: "Tất cả", value: "" },
      { label: "Dưới 50000(km)", value: "0-50000" },
      { label: "50000 - 200000(km)", value: "50000-200000" },
      {
        label: "Trên 200000(km)",
        value: "200000-10000000000000",
      },
    ],
  };

  // CONDITIONS object per type (label + value)
  // Keep as-is; we'll build a union from this so all types share the same options
  const CONDITIONS = {
    car: [
      { label: "Mới 99% (Lướt)", value: "99-100" },
      { label: "Tốt 85-98% (Đã sử dụng)", value: "85-98" },
      { label: "Khá 70-84% (Cần bảo dưỡng)", value: "70-84" },
      { label: "Trung bình dưới 70%", value: "0-69" },
    ],
    bike: [
      { label: "Mới 99% (Lướt)", value: "99-100" },
      { label: "Tốt 85-98% (Đã sử dụng)", value: "85-98" },
      { label: "Khá 70-84% (Cần bảo dưỡng)", value: "70-84" },
      { label: "Trung bình dưới 70%", value: "0-69" },
    ],
    motorcycle: [
      { label: "Mới 99% (Lướt)", value: "99-100" },
      { label: "Tốt 85-98% (Đã sử dụng)", value: "85-98" },
      { label: "Khá 70-84% (Cần bảo dưỡng)", value: "70-84" },
      { label: "Trung bình dưới 70%", value: "0-69" },
    ],
    battery: [
      { label: "Mới 99% (Lướt)", value: "99-100" },
      { label: "Tốt 85-98% (Đã sử dụng)", value: "85-98" },
      { label: "Khá 70-84% (Cần bảo dưỡng)", value: "70-84" },
      { label: "Trung bình dưới 70%", value: "0-69" },
    ],
  };

  // show/hide filter groups according to activeType
  function updateVisibility(type) {
    filterGroups.forEach((g) => {
      const types = (g.dataset.filterType || "").split(/\s+/).filter(Boolean);
      const show = types.includes(type);
      g.style.display = show ? "" : "none";
    });
  }

  // populate visible selects / create plain inputs for brand & battery-type
  function populateSelects(type) {
    filterGroups.forEach((group) => {
      const label = (group.querySelector("label")?.innerText || "").trim();
      const existingSelect = group.querySelector("select");
      const existingInput = group.querySelector("input[type='text']");

      // don't populate hidden groups to preserve selection
      if (group.style.display === "none") return;

      // Thương hiệu -> input text (no datalist)
      if (label.includes("Thương hiệu")) {
        if (existingInput) {
          existingInput.placeholder = "Nhập thương hiệu";
        } else {
          const input = document.createElement("input");
          input.type = "text";
          input.placeholder = "Nhập thương hiệu";
          if (existingSelect) existingSelect.replaceWith(input);
          else group.appendChild(input);
        }
        return;
      }

      // Loại pin -> input text (no datalist)
      if (label.includes("Loại pin")) {
        if (existingInput) {
          existingInput.placeholder = "Nhập Loại Pin";
        } else {
          const input = document.createElement("input");
          input.type = "text";
          input.placeholder = "Nhập Loại Pin";
          if (existingSelect) existingSelect.replaceWith(input);
          else group.appendChild(input);
        }
        return;
      }

      // Dung lượng pin -> input text (cho phép tự nhập)
      if (label.includes("Dung lượng")) {
        if (existingInput) {
          existingInput.placeholder = "Nhập dung lượng (Ah/kWh)";
        } else {
          const input = document.createElement("input");
          input.type = "text"; // Cho phép nhập text (hoặc number nếu muốn chặn chữ)
          input.placeholder = "Nhập dung lượng (Ah/kWh)";

          if (existingSelect) existingSelect.replaceWith(input);
          else group.appendChild(input);
        }
        return; // Kết thúc xử lý cho trường này, không chạy xuống logic tạo select bên dưới
      }

      // Other groups use a select
      let select = group.querySelector("select");
      if (!select) {
        // if previously replaced by input, replace back to select
        select = document.createElement("select");
        if (existingInput) existingInput.replaceWith(select);
        else group.appendChild(select);
      }
      select.innerHTML = "";

      if (label.includes("Năm sản xuất")) {
        select.appendChild(createOption("Chọn năm", ""));
        YEARS.forEach((y) => select.appendChild(createOption(y, y)));
      } else if (label.includes("Khoảng giá")) {
        select.appendChild(createOption("Chọn khoảng giá", ""));
        const ranges = PRICE_RANGES[type] || PRICE_RANGES.battery;
        ranges.forEach((r) =>
          select.appendChild(createOption(r.label, r.value))
        );
      } else if (label.includes("Quãng đường")) {
        select.appendChild(createOption("Chọn quãng đường", ""));
        // lấy bộ quãng đường theo type hiện tại
        const ranges = MILEAGE_RANGES[type] || [];
        ranges.forEach((m) =>
          select.appendChild(createOption(m.label, m.value))
        );
      } else if (label.includes("Tình trạng")) {
        // ---------- FIXED: put union logic here (populate once for all types) ----------
        select.appendChild(createOption("Chọn tình trạng", ""));

        // union tất cả condition từ mọi loại (để car, battery, bike, motorcycle giống nhau)
        const union = [];
        for (const k in CONDITIONS) {
          if (!Array.isArray(CONDITIONS[k])) continue;
          CONDITIONS[k].forEach((c) => {
            // tránh duplicate value
            if (!union.find((u) => u.value === c.value)) union.push(c);
          });
        }

        // optional: sắp xếp theo phạm vi phần trăm giảm dần (99-100, 85-98, 70-84, 0-69)
        union.sort((a, b) => {
          const pa = a.value.split("-").map((x) => Number(x));
          const pb = b.value.split("-").map((x) => Number(x));
          if (pa.length < 2 || pb.length < 2) return 0;
          return pb[0] - pa[0];
        });

        union.forEach((c) => {
          select.appendChild(createOption(c.label, c.value));
        });
      } else {
        select.appendChild(
          createOption(select.getAttribute("placeholder") || "Chọn", "")
        );
      }
    });
  }

  // build URLSearchParams from visible selects & inputs
  function buildParams() {
    const params = new URLSearchParams();
    params.set("type", activeType);
    const q = (mainSearchInput?.value || "").trim();
    if (q) params.set("q", q);

    filterGroups.forEach((g) => {
      if (g.style.display === "none") return;
      const label = (g.querySelector("label")?.innerText || "").trim();
      const control = g.querySelector("input[type='text'], select");
      if (!control) return;
      const val = (control.value || "").trim();
      if (!val) return;

      if (label.includes("Thương hiệu")) {
        // send exactly what user typed; backend should handle partial/case-insensitive matching
        params.set("brand", val);
      } else if (label.includes("Loại pin") && activeType === "battery") {
        params.set("batteryType", val);
      } else if (label.includes("Năm sản xuất")) {
        params.set("yearOfManufacture", val);
      } else if (label.includes("Khoảng giá")) {
        if (val.includes("-")) {
          const [min, max] = val.split("-");
          if (min) params.set("priceMin", min);
          if (max) params.set("priceMax", max);
        }
      } else if (label.includes("Dung lượng")) {
        params.set("batteryCapacity", val);
      } else if (label.includes("Quãng đường")) {
        params.set("mileageRange", val);
      } else if (label.includes("Tình trạng")) {
        // ---------- FIXED: no DOM manipulation here, just send selected value ----------
        params.set("conditionName", val);
      }
    });

    return params;
  }

  // render results (simple card grid)
  function renderResults(items) {
    resultsContainer.innerHTML = "";
    const wrapper = document.createElement("div");
    wrapper.className = "filter-results-wrapper";

    const header = document.createElement("div");
    header.className = "filter-results-header";
    header.innerHTML = `<h3>Kết quả tìm kiếm (${items.length})</h3>`;
    wrapper.appendChild(header);

    if (!items || items.length === 0) {
      const p = document.createElement("p");
      p.textContent = "Không tìm thấy sản phẩm phù hợp.";
      p.style.padding = "18px 0";
      wrapper.appendChild(p);
      resultsContainer.appendChild(wrapper);
      window.scrollTo({
        top: resultsContainer.offsetTop - 20,
        behavior: "smooth",
      });
      return;
    }

    // helper: find condition label from CONDITIONS object or numeric percent
    function getConditionLabelFromValue(val, percent) {
      // nếu null/empty
      if (!val && (percent === undefined || percent === "")) return "";

      // 1) Nếu backend đã trả một "label" rõ ràng (chứa chữ không phải chỉ range)
      //    ví dụ: "Tốt 85-94% (Đã sử dụng)" -> giữ nguyên (trường hợp muốn show exact label)
      const isProbablyLabel =
        typeof val === "string" && /[a-zA-ZÀ-ỹ\u00C0-\u017F]/.test(val);
      if (
        isProbablyLabel &&
        !/^\s*\d{1,3}\s*-\s*\d{1,3}\s*$/.test(val.trim())
      ) {
        // nếu nó chứa chữ (Tiếng Việt) và không phải đúng format "num-num", trả label thô
        return val;
      }

      // 2) Build a unified list of condition ranges from CONDITIONS (cover car,bike, motorcycle, battery)
      //    so mapping is consistent across types.
      const unionConditions = [];
      for (const k in CONDITIONS) {
        if (Array.isArray(CONDITIONS[k])) {
          CONDITIONS[k].forEach((c) => {
            // avoid duplicates by value
            if (!unionConditions.find((u) => u.value === c.value))
              unionConditions.push(c);
          });
        }
      }

      // helper: parse "val" if it is a range "85-94" or if it contains one or more numbers
      function parseRangeFromString(s) {
        if (!s || typeof s !== "string") return null;
        // try explicit range like 85-94
        const rangeMatch = s.match(/(\d{1,3})\s*-\s*(\d{1,3})/);
        if (rangeMatch) {
          const a = Number(rangeMatch[1]),
            b = Number(rangeMatch[2]);
          if (!isNaN(a) && !isNaN(b))
            return { min: Math.min(a, b), max: Math.max(a, b) };
        }
        // else try to find first single percent/number e.g. "87" or "87%" or in a label "Tốt 87% (...)"
        const numMatch = s.match(/(\d{1,3})\s*%?/);
        if (numMatch) {
          const v = Number(numMatch[1]);
          if (!isNaN(v)) return { min: v, max: v };
        }
        return null;
      }

      // try to parse from val first, then fall back to percent argument
      let parsed = parseRangeFromString(String(val || ""));
      if (!parsed && percent) {
        parsed = parseRangeFromString(String(percent));
      }

      // 3) If parsed numeric range / percent found -> find overlapping condition in unionConditions
      if (parsed) {
        for (const c of unionConditions) {
          const parts = String(c.value)
            .split("-")
            .map((s) => Number(s.trim()));
          if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
            const cmin = parts[0],
              cmax = parts[1];
            // overlap check: parsed range [parsed.min, parsed.max] overlaps condition range [cmin, cmax]
            if (!(parsed.max < cmin || parsed.min > cmax)) {
              return c.label; // return matching label (first overlap)
            }
          }
        }
      }

      // 4) As a final fallback: if val is exactly a known value key or exact label in unionConditions, return its label
      if (val) {
        const raw = String(val).trim();
        // exact value match
        const f1 = unionConditions.find(
          (c) => c.value === raw || c.label === raw
        );
        if (f1) return f1.label;
        // try stripping spaces
        const f2 = unionConditions.find(
          (c) => c.value.replace(/\s+/g, "") === raw.replace(/\s+/g, "")
        );
        if (f2) return f2.label;
      }

      // 5) nothing matched -> return empty string (no badge)
      return "";
    }

    const grid = document.createElement("div");
    grid.className = "filter-results-grid";

    items.forEach((it) => {
      const card = document.createElement("div");
      card.className = "filter-card-item";

      // ---------- THUMB (ảnh bên trái) ----------
      const img = document.createElement("img");
      img.alt = it.productName || "";

      const PRODUCT_SERVICE_BASE = "";

      let mainUrl = "";
      if (it.imageUrls && it.imageUrls.length > 0) mainUrl = it.imageUrls[0];
      else if (it.imageUrl) mainUrl = it.imageUrl;

      if (mainUrl) {
        if (!mainUrl.match(/^https?:\/\//)) {
          if (mainUrl.startsWith("/")) {
            mainUrl = PRODUCT_SERVICE_BASE.replace(/\/$/, "") + mainUrl;
          } else {
            mainUrl = PRODUCT_SERVICE_BASE.replace(/\/$/, "") + "/" + mainUrl;
          }
        }
      }

      img.src = mainUrl || "/images/product.jpg";
      img.onerror = () => (img.src = "/images/product.jpg");

      const thumb = document.createElement("div");
      thumb.className = "filter-card-thumb";
      thumb.style.position = "relative"; // đảm bảo vị trí tương đối để nút absolute hoạt động

      // tạo nút favorite giống style đã dùng cho product-card
      const favBtn = document.createElement("button");
      favBtn.className = "favorite-btn";
      const pid =
        it.productId || it.productId === 0 ? Number(it.productId) : null;
      if (
        typeof likedSet !== "undefined" &&
        pid !== null &&
        likedSet.has(pid)
      ) {
        favBtn.classList.add("active");
      }
      favBtn.setAttribute("data-productid", pid !== null ? pid : "");
      favBtn.setAttribute(
        "aria-label",
        favBtn.classList.contains("active") ? "Bỏ thích" : "Thêm vào yêu thích"
      );
      // icon bên trong (fas: solid khi active, far: regular khi chưa)
      const icon = document.createElement("i");
      icon.className =
        (favBtn.classList.contains("active") ? "fas" : "far") + " fa-heart";
      favBtn.appendChild(icon);

      // optional: nếu bạn muốn tooltip/hỗ trợ truy cập: title
      favBtn.title = favBtn.classList.contains("active")
        ? "Bỏ thích"
        : "Thêm vào yêu thích";

      // append nút trước ảnh (hiển thị trên góc ảnh)
      thumb.appendChild(favBtn);
      thumb.appendChild(img);

      // ---------- BODY (phần bên phải) ----------
      const body = document.createElement("div");
      body.className = "filter-card-body";

      // Title
      const title = document.createElement("h4");
      title.textContent =
        it.productName || `${it.brand || ""} ${it.yearOfManufacture || ""}`;

      // --- BRAND / BATTERY type (hiển thị dưới productName và trước giá) ---
      // backend có thể trả spec ở nhiều tên khác nhau -> thử lấy thương hiệu từ nhiều поля
      const spec =
        it.spec || it.specification || it.productSpec || it.details || {};
      const brandName = (
        it.brand ||
        spec.brand ||
        it.manufacturer ||
        ""
      ).trim();
      const batteryType = (
        it.batteryType ||
        spec.batteryType ||
        spec.type ||
        ""
      ).trim();

      // create brand / battery element
      const infoLine = document.createElement("p");
      infoLine.className = "product-subinfo";

      if (
        (it.productType || "").toLowerCase() === "battery" ||
        it.type === "battery"
      ) {
        // cho pin: hiển thị "Loại Pin: xyz"
        if (batteryType) infoLine.textContent = `Loại pin: ${batteryType}`;
        else infoLine.textContent = ""; // nếu không có thì ẩn text (CSS sẽ giữ layout)
      } else {
        // cho các loại khác: hiển thị brand
        if (brandName) infoLine.textContent = `Thương hiệu: ${brandName}`;
        else infoLine.textContent = ""; // có thể để "N/A" nếu muốn
      }

      // Price + Condition row (placed under title, left side)
      const priceRow = document.createElement("div");
      priceRow.className = "filter-card-price-row";

      const price = document.createElement("div");
      price.className = "filter-card-price";
      price.textContent = it.price ? formatPrice(it.price) : "Thương lượng";

      // determine condition label: prefer explicit field 'conditionName', fallback to 'conditionPercent' or numeric 'condition'
      const condField =
        it.conditionName || it.condition || it.conditionLabel || "";
      const condPercent = it.conditionPercent || it.conditionValue || "";
      const condLabel = getConditionLabelFromValue(condField, condPercent);

      const condBadge = document.createElement("div");
      condBadge.className = "condition-badge";
      condBadge.textContent = condLabel || ""; // empty if none

      priceRow.appendChild(price);
      if (condLabel) priceRow.appendChild(condBadge);

      // meta (location, year, batteryCapacity, mileage)
      const meta = document.createElement("div");
      meta.className = "filter-card-meta";
      meta.innerHTML = `${
        it.location
          ? `<span><i class="fas fa-map-marker-alt"></i> ${it.location}</span>`
          : ""
      }
    ${
      it.yearOfManufacture
        ? `<span><i class="fas fa-calendar-alt"></i> ${it.yearOfManufacture}</span>`
        : ""
    }
    ${
      it.batteryCapacity
        ? `<span><i class="fas fa-bolt"></i> ${it.batteryCapacity}</span>`
        : ""
    }
    ${
      it.mileage
        ? `<span><i class="fas fa-road"></i> ${it.mileage} km</span>`
        : ""
    }`;

      const actions = document.createElement("div");
      actions.className = "filter-card-actions";
      actions.innerHTML = `<a class="btn-search btn-transparent-dark" href="/product_detail.html?id=${
        it.productId || ""
      }">Xem chi tiết</a>
                         <button class="btn-search btn-transparent-dark btn-buy-now" data-productid="${
                           it.productId || ""
                         }">Mua ngay</button>`;

      // assemble body: title, priceRow, meta, actions
      body.appendChild(title);
      // only append if there's text (tránh thừa khoảng trắng)
      if (infoLine.textContent && infoLine.textContent.trim() !== "") {
        body.appendChild(infoLine);
      }
      body.appendChild(priceRow);
      body.appendChild(meta);
      body.appendChild(actions);

      // assemble card
      card.appendChild(thumb);
      card.appendChild(body);
      grid.appendChild(card);
    });

    wrapper.appendChild(grid);
    resultsContainer.appendChild(wrapper);
    window.scrollTo({
      top: resultsContainer.offsetTop - 16,
      behavior: "smooth",
    });
  }

  function formatPrice(v) {
    const n = Number(v);
    if (isNaN(n)) return v;
    return n.toLocaleString("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    });
  }

  // --- helper: đọc giá trị 'Tình trạng' đang được chọn trên UI ---
  function getSelectedConditionValueFromUI() {
    // tìm filter-group có label chứa "Tình trạng" và đang hiển thị
    const condGroup = Array.from(filterGroups).find(
      (g) =>
        (g.querySelector("label")?.innerText || "")
          .toLowerCase()
          .includes("tình trạng") && g.style.display !== "none"
    );
    if (!condGroup) return "";
    const control = condGroup.querySelector("select, input[type='text']");
    return control ? (control.value || "").trim() : "";
  }

  // --- helper: kiểm tra 1 item có thỏa condition (condVal: "85-94" hoặc label text) ---
  function matchesCondition(item, condVal) {
    if (!condVal) return true;

    const itemLabel = (
      item.conditionName ||
      item.conditionLabel ||
      item.condition ||
      ""
    ).toString();

    // helper: try parse explicit percent/range from item label -> returns {min,max} or null
    function parsePercentRangeFromLabel(s) {
      if (!s || typeof s !== "string") return null;
      const lower = s.toLowerCase();

      // 1) explicit range "x-y"
      const rng = lower.match(/(\d{1,3})\s*-\s*(\d{1,3})/);
      if (rng) {
        const a = Number(rng[1]),
          b = Number(rng[2]);
        if (!isNaN(a) && !isNaN(b))
          return { min: Math.min(a, b), max: Math.max(a, b) };
      }

      // 2) "dưới 70", "duoi 70", "below 70", "< 70" -> treat as [0, n-1]
      const below = lower.match(/(?:dưới|duoi|below|under|<)\s*(\d{1,3})/i);
      if (below) {
        const n = Number(below[1]);
        if (!isNaN(n)) return { min: 0, max: Math.max(0, n - 1) };
      }

      // 3) "trên 70", "tren 70", "above 70", "> 70" -> treat as [n+1,100]
      const above = lower.match(/(?:trên|tren|above|over|>)\s*(\d{1,3})/i);
      if (above) {
        const n = Number(above[1]);
        if (!isNaN(n)) return { min: Math.min(100, n + 1), max: 100 };
      }

      // 4) single percent "87%" or "87" -> exact
      const single = lower.match(/(\d{1,3})\s*%?/);
      if (single) {
        const v = Number(single[1]);
        if (!isNaN(v)) return { min: v, max: v };
      }

      return null;
    }

    // parse requested condition (condVal) into numeric range if possible
    function parseCondValToRange(s) {
      if (!s || typeof s !== "string") return null;
      const m = s.match(/(\d{1,3})\s*-\s*(\d{1,3})/);
      if (m) return { min: Number(m[1]), max: Number(m[2]) };
      // else try single number
      const single = s.match(/(\d{1,3})/);
      if (single) {
        const v = Number(single[1]);
        return { min: v, max: v };
      }
      return null;
    }

    const itemRange = parsePercentRangeFromLabel(itemLabel);
    const condRange = parseCondValToRange(condVal);

    // If both are numeric/range -> check overlap properly
    if (condRange && itemRange) {
      if (!(itemRange.max < condRange.min || itemRange.min > condRange.max))
        return true;
      return false;
    }

    // If selected cond is range and item has a single numeric percent -> check containment
    if (condRange && !itemRange) {
      // try to extract single percent from itemLabel
      const single = itemLabel.match(/(\d{1,3})\s*%?/);
      if (single) {
        const v = Number(single[1]);
        if (!isNaN(v) && v >= condRange.min && v <= condRange.max) return true;
      }
    }

    // fallback: text matching using normalized labels (keeps existing mapping behavior)
    const itemLabelLower = itemLabel.toLowerCase();
    const condValLower = condVal.toLowerCase();

    if (
      itemLabelLower.includes(condValLower) ||
      condValLower.includes(itemLabelLower)
    )
      return true;

    return false;
  }

  // ---------- New doSearch() with client cache fallback ----------
  async function doSearch() {
    const params = buildParams();
    const cacheKey = makeClientCacheKey(params);
    // Try client-side cache first
    const cached = clientCacheGet(cacheKey);
    if (cached) {
      console.debug("[doSearch] served from client cache:", cacheKey);
      renderResults(cached);
      return;
    }

    resultsContainer.innerHTML = `<p style="padding:18px 0">Đang tìm...</p>`;
    try {
      const url = `${SEARCH_API}?${params.toString()}`;
      const r = await fetch(url);
      if (!r.ok) {
        const txt = await r.text();
        resultsContainer.innerHTML = `<p style="color:#c00;padding:18px 0">Lỗi: ${r.status} ${r.statusText} - ${txt}</p>`;
        return;
      }
      const data = await r.json();
      let items = Array.isArray(data) ? data : data.items || [];

      // client-side condition filter (unchanged)
      const selectedCond = getSelectedConditionValueFromUI();
      if (selectedCond) {
        const before = items.length;
        items = items.filter((it) => matchesCondition(it, selectedCond));
        console.debug(
          `[doSearch] applied client condition filter: selected='${selectedCond}', before=${before}, after=${items.length}`
        );
      }

      // save into client cache
      clientCacheSet(cacheKey, items);

      renderResults(items);
    } catch (err) {
      resultsContainer.innerHTML = `<p style="color:#c00;padding:18px 0">Không thể kết nối: ${err.message}</p>`;
      console.error(err);
    }
  }

  // events
  tabs.forEach((t) =>
    t.addEventListener("click", () => {
      tabs.forEach((x) => x.classList.remove("active"));
      t.classList.add("active");
      activeType = t.dataset.type;
      updateVisibility(activeType);
      populateSelects(activeType);
    })
  );

  if (filterBtn)
    filterBtn.addEventListener("click", (e) => {
      e.preventDefault();
      doSearch();
    });
  if (mainSearchInput)
    mainSearchInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        doSearch();
      }
    });

  // initial setup
  updateVisibility(activeType);
  populateSelects(activeType);
});
