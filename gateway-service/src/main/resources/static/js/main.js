// /js/main.js — SUPER-ENHANCED interactions (tilt, parallax, particles, reveal, lazy-load, tabs)
document.addEventListener("DOMContentLoaded", () => {
  const prefersReducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)"
  ).matches;

  /* ------------------------------------------------------------------------
       UTILITIES
       ------------------------------------------------------------------------ */
  const debounce = (fn, wait = 200) => {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn.apply(this, args), wait);
    };
  };
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

  /* ------------------------------------------------------------------------
       1) Product tabs & Filter tabs (Enhanced)
       ------------------------------------------------------------------------ */
  const productTabButtons = Array.from(
    document.querySelectorAll(".products-section .tab-btn")
  );
  productTabButtons.forEach((button) => {
    button.addEventListener("click", () => {
      productTabButtons.forEach((b) => b.classList.remove("active"));
      button.classList.add("active");
      button.animate(
        [{ transform: "scale(0.95)" }, { transform: "scale(1)" }],
        {
          duration: 150,
          easing: "ease-out",
        }
      );
    });
  });

  const filterTabButtons = Array.from(
    document.querySelectorAll(".search-filter-section .filter-tab")
  );
  const mainSearchBarInput = document.querySelector(".main-search-bar input");
  const allFilterGroups = Array.from(
    document.querySelectorAll(".filter-grid .filter-group")
  );

  const updateFilterGrid = (searchType) => {
    allFilterGroups.forEach((group) => {
      const filterTypes = group.getAttribute("data-filter-type") || "";
      group.style.transition = "opacity 300ms, transform 300ms";
      if (filterTypes.includes(searchType)) {
        group.style.display = "block";
        requestAnimationFrame(() => {
          group.style.opacity = 1;
          group.style.transform = "translateY(0)";
        });
      } else {
        group.style.opacity = 0;
        group.style.transform = "translateY(10px)";
        setTimeout(() => (group.style.display = "none"), 300);
      }
    });
  };

  filterTabButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      filterTabButtons.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const type = btn.getAttribute("data-type") || "car";
      mainSearchBarInput.placeholder =
        type === "battery"
          ? "Tìm kiếm pin điện..."
          : type === "bike"
          ? "Tìm kiếm xe đạp điện..."
          : type === "motorcycle"
          ? "Tìm kiếm xe máy điện..."
          : "Tìm kiếm ô tô điện...";
      updateFilterGrid(type);
    });
  });

  const initialActive = document.querySelector(
    ".filter-tabs .filter-tab.active"
  );
  const initType = initialActive
    ? initialActive.getAttribute("data-type")
    : "battery";
  updateFilterGrid(initType);
  if (initType === "battery")
    mainSearchBarInput.placeholder = "Tìm kiếm pin, phụ kiện...";

  const searchButton = document.querySelector(".filter-actions .btn-primary");
  const handleSearch = debounce(() => {
    const q = (mainSearchBarInput && mainSearchBarInput.value) || "";
    console.log("Search query:", q);
  }, 250);

  if (searchButton) {
    searchButton.addEventListener("click", (e) => {
      e.preventDefault();
      handleSearch();
    });
  }

  /* ------------------------------------------------------------------------
       2) TILT Effect (3D Glare/Reflexion on Product Cards)
       ------------------------------------------------------------------------ */
  const productCards = Array.from(document.querySelectorAll(".product-card"));

  const initTilt = (card) => {
    if (prefersReducedMotion) return;

    let glare = card.querySelector(".glare");
    if (!glare) {
      glare = document.createElement("div");
      glare.className = "glare";
      glare.style.cssText =
        "position:absolute;top:0;left:0;width:100%;height:100%;border-radius:12px;opacity:0;transition:opacity 300ms;pointer-events:none;background:radial-gradient(circle at center, rgba(255,255,255,0.2) 0%, transparent 80%);";
      card.appendChild(glare);
    }

    const strength = 18;
    const maxGlareOpacity = 0.4;
    let animationFrameId = null;

    const onMove = (e) => {
      if (animationFrameId) cancelAnimationFrame(animationFrameId);

      animationFrameId = requestAnimationFrame(() => {
        const rect = card.getBoundingClientRect();
        const px = (e.clientX - rect.left) / rect.width - 0.5;
        const py = (e.clientY - rect.top) / rect.height - 0.5;
        const rx = clamp(-py * strength, -strength, strength);
        const ry = clamp(px * strength, -strength, strength);

        const glareX = (px + 0.5) * 100;
        const glareY = (py + 0.5) * 100;

        glare.style.opacity = maxGlareOpacity;
        glare.style.background = `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.4) 0%, transparent 70%)`;

        card.style.transform = `rotateX(${rx}deg) rotateY(${ry}deg) translateZ(10px)`;
      });
    };

    const onEnter = () => {
      card.style.transition = `transform 150ms ease-out`;
      glare.style.opacity = 0;
      glare.style.transition = "opacity 50ms";
    };

    const onLeave = () => {
      if (animationFrameId) cancelAnimationFrame(animationFrameId);
      card.style.transition = `transform 450ms cubic-bezier(.2,.9,.3,1.1)`;
      card.style.transform = "rotateX(0deg) rotateY(0deg) translateZ(0px)";
      glare.style.opacity = 0;
      glare.style.transition = "opacity 400ms";
    };

    card.addEventListener("mouseenter", onEnter);
    card.addEventListener("mousemove", onMove);
    card.addEventListener("mouseleave", onLeave);
    card.addEventListener("blur", onLeave);
    card.addEventListener("focus", onEnter);
  };

  productCards.forEach((card) => {
    card.style.perspective = "1000px";
    card.style.transformStyle = "preserve-3d";
    card.tabIndex = 0;
    initTilt(card);
  });

  /* ------------------------------------------------------------------------
       3) Ripple Effect for Primary Buttons
       ------------------------------------------------------------------------ */
  const primaryButtons = Array.from(document.querySelectorAll(".btn-primary"));

  const triggerRippleGlow = (button, x = 50, y = 50) => {
    button.style.setProperty("--x", x + "px");
    button.style.setProperty("--y", y + "px");
    button.classList.remove("ripple");
    requestAnimationFrame(() => button.classList.add("ripple"));
    button.style.boxShadow =
      "0 0 0 4px rgba(255,255,255,0.3), 0 15px 35px rgba(23,138,72,0.4)";
  };

  primaryButtons.forEach((button) => {
    button.addEventListener("click", function (e) {
      const x = e.clientX - e.target.getBoundingClientRect().left;
      const y = e.clientY - e.target.getBoundingClientRect().top;
      triggerRippleGlow(this, x, y);
    });
    button.addEventListener("focus", function () {
      triggerRippleGlow(this, this.offsetWidth / 2, this.offsetHeight / 2);
    });
    button.addEventListener("blur", function () {
      this.style.boxShadow = "";
    });
  });

  /* ------------------------------------------------------------------------
       4) Scroll Reveal (Staggered Effect)
       ------------------------------------------------------------------------ */
  const revealEls = Array.from(
    document.querySelectorAll(
      ".reveal, .fade-in-up, .feature-card, .product-card, .hero-text"
    )
  );
  if (!document.querySelector("style[data-reveal-css]")) {
    const style = document.createElement("style");
    style.setAttribute("data-reveal-css", "true");
    style.textContent = `
      .pre-reveal { opacity:0!important; transform:translateY(20px)!important; transition:none!important; }
      .visible { opacity:1!important; transform:translateY(0)!important; transition:opacity 700ms cubic-bezier(0.2,0.9,0.3,1.1), transform 700ms cubic-bezier(0.2,0.9,0.3,1.1)!important; }
    `;
    document.head.appendChild(style);
  }

  if ("IntersectionObserver" in window) {
    revealEls.forEach((el) => el.classList.add("pre-reveal"));
    const revealObserver = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const target = entry.target;
            const isStaggered =
              target.classList.contains("product-card") ||
              target.classList.contains("feature-card");
            if (isStaggered) {
              const index = Array.from(target.parentNode.children).indexOf(
                target
              );
              const delay = index * 100;
              setTimeout(
                () => {
                  target.classList.remove("pre-reveal");
                  target.classList.add("visible");
                },
                prefersReducedMotion ? 0 : delay
              );
            } else {
              target.classList.remove("pre-reveal");
              target.classList.add("visible");
            }
            obs.unobserve(target);
          }
        });
      },
      { threshold: 0.1 }
    );
    revealEls.forEach((el) => revealObserver.observe(el));
  }

  /* ------------------------------------------------------------------------
       5) Hero Parallax
       ------------------------------------------------------------------------ */
  const hero = document.querySelector(".hero-section");
  const heroImageContainer = document.querySelector(".hero-image-container");
  const heroImage = document.querySelector(".image-card");
  let currentParallaxX = 0,
    currentParallaxY = 0,
    targetParallaxX = 0,
    targetParallaxY = 0;
  const easing = 0.1;

  const animateParallax = () => {
    currentParallaxX += (targetParallaxX - currentParallaxX) * easing;
    currentParallaxY += (targetParallaxY - currentParallaxY) * easing;
    if (heroImage)
      heroImage.style.transform = `rotateY(${
        currentParallaxX * 10
      }deg) rotateX(${-currentParallaxY * 5}deg) translateZ(0)`;
    if (heroImageContainer)
      heroImageContainer.style.transform = `translate3d(${
        currentParallaxX * 25
      }px, ${currentParallaxY * 15}px, 0)`;
    requestAnimationFrame(animateParallax);
  };

  if (hero && heroImage && !prefersReducedMotion) {
    animateParallax();
    hero.addEventListener("mousemove", (e) => {
      const r = hero.getBoundingClientRect();
      targetParallaxX = (e.clientX - r.left) / r.width - 0.5;
      targetParallaxY = (e.clientY - r.top) / r.height - 0.5;
    });
    hero.addEventListener("mouseleave", () => {
      targetParallaxX = 0;
      targetParallaxY = 0;
      setTimeout(() => {
        if (
          Math.abs(currentParallaxX) < 0.01 &&
          Math.abs(currentParallaxY) < 0.01
        )
          heroImageContainer.style.transform = "";
      }, 500);
    });
  }

  /* ------------------------------------------------------------------------
       6) Animated progress bars
       ------------------------------------------------------------------------ */
  const animateProgress = (el) => {
    const target = parseFloat(el.dataset.progress || el.style.width || 0);
    el.style.width = "0%";
    requestAnimationFrame(() => {
      el.style.width = `${target}%`;
      el.dataset.animated = "true";
    });
  };
  Array.from(document.querySelectorAll(".progress")).forEach((p) => {
    const inlineWidth = p.style.width && parseFloat(p.style.width);
    if (inlineWidth) {
      p.dataset.progress = inlineWidth;
      p.style.width = "0%";
    } else {
      const computed =
        p.getAttribute("style")?.match(/width:\s*(\d+)%/) || null;
      if (!p.dataset.progress && computed) p.dataset.progress = computed[1];
    }
  });

  if ("IntersectionObserver" in window) {
    const progressObserver = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach((en) => {
          if (en.isIntersecting) {
            const bar = en.target.classList.contains("progress")
              ? en.target
              : en.target.querySelector(".progress");
            if (bar && bar.dataset.progress) animateProgress(bar);
            obs.unobserve(en.target);
          }
        });
      },
      { threshold: 0.15 }
    );
    document
      .querySelectorAll(".progress-bar-container, .progress")
      .forEach((node) => progressObserver.observe(node));
  }

  /* ------------------------------------------------------------------------
       7) Lazy-load images
       ------------------------------------------------------------------------ */
  const lazyImages = Array.from(document.querySelectorAll("img[data-src]"));
  if ("IntersectionObserver" in window) {
    const imgObserver = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.setAttribute("loading", "lazy");
            img.src = img.dataset.src;
            img.removeAttribute("data-src");
            img.style.transition = "opacity 500ms";
            img.style.opacity = 0;
            img.onload = () => (img.style.opacity = 1);
            obs.unobserve(img);
          }
        });
      },
      { rootMargin: "200px 0px" }
    );
    lazyImages.forEach((img) => imgObserver.observe(img));
  }

  /* ------------------------------------------------------------------------
       8) Hero Particles
       ------------------------------------------------------------------------ */
  const makeParticles = (count = 15) => {
    if (!hero || prefersReducedMotion) return;
    for (let i = 0; i < count; i++) {
      const p = document.createElement("div");
      p.className = "particle";
      p.style.position = "absolute";
      p.style.borderRadius = "50%";
      p.style.zIndex = "1";
      p.style.width = `${6 + Math.round(Math.random() * 10)}px`;
      p.style.height = p.style.width;
      p.style.left = `${Math.random() * 95}%`;
      p.style.top = `${Math.random() * 85}%`;
      p.style.background =
        Math.random() > 0.5 ? "rgba(59,145,255,0.2)" : "rgba(23,138,72,0.2)";
      p.style.boxShadow = `0 0 8px ${p.style.background}`;
      p.style.transform = `translateY(${20 + Math.random() * 40}px)`;
      const dur = 8000 + Math.random() * 9000;
      p.animate(
        [
          { transform: `translateY(-10px)`, opacity: 0.35 },
          {
            transform: `translateY(${10 + Math.random() * 40}px)`,
            opacity: 0.9,
          },
          { transform: `translateY(-6px)`, opacity: 0.2 },
        ],
        {
          duration: dur,
          iterations: Infinity,
          direction: "alternate",
          easing: "ease-in-out",
        }
      );
      hero.appendChild(p);
    }
  };
  try {
    makeParticles(15);
  } catch (err) {}

  /* ------------------------------------------------------------------------
       9) Favorite (Heart) Toggle Effect 💖
       ------------------------------------------------------------------------ */
  const favoriteIcons = Array.from(document.querySelectorAll(".favorite-icon"));

  favoriteIcons.forEach((icon) => {
    icon.addEventListener("click", (e) => {
      e.stopPropagation();
      icon.classList.toggle("active");
      const isActive = icon.classList.contains("active");

      // Hiệu ứng nhịp tim
      icon.animate(
        [
          { transform: "scale(1)" },
          { transform: "scale(1.3)" },
          { transform: "scale(1)" },
        ],
        { duration: 300, easing: "ease-in-out" }
      );

      // Lưu vào localStorage
      const productCard = icon.closest(".product-card");
      const productId = productCard?.getAttribute("data-id");
      if (productId) {
        let favorites = JSON.parse(localStorage.getItem("favorites") || "[]");
        if (isActive) {
          if (!favorites.includes(productId)) favorites.push(productId);
        } else {
          favorites = favorites.filter((id) => id !== productId);
        }
        localStorage.setItem("favorites", JSON.stringify(favorites));
      }
    });
  });

  window.addEventListener("load", () => {
    const favorites = JSON.parse(localStorage.getItem("favorites") || "[]");
    favorites.forEach((id) => {
      const icon = document.querySelector(
        `.product-card[data-id='${id}'] .favorite-icon`
      );
      if (icon) icon.classList.add("active");
    });
  });

  console.log("Super-Enhanced UI init complete. 🚀");
});
