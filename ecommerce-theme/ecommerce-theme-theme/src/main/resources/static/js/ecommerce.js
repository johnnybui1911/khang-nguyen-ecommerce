/*
 * Khang Nguyen storefront theme script.
 * Every page is a server-rendered template shell; this script fills it from
 * the same-origin storefront API. Data is rendered with textContent only.
 * The bag lives in localStorage; checkout needs a signed-in user.
 */
(function () {
    'use strict';

    var d = document;
    var b = d.documentElement;
    var PARAM = b.dataset.param && b.dataset.param !== 'null' ? b.dataset.param : '';
    var BAG_KEY = 'ecm.bag';
    var CAT_KEY = 'ecm.categories';
    var CAT_TTL = 5 * 60 * 1000;
    var FALLBACK_CATEGORIES = [
        ['vong-ngoc', 'Vòng ngọc'], ['nhan-ngoc', 'Nhẫn ngọc'],
        ['chuoi-hat', 'Chuỗi hạt'], ['set-nhan-vong', 'Set nhẫn vòng']
    ].map(function (c, i) { return {name: c[0], displayName: c[1], displayOrder: i + 1}; });

    function q(sel, root) { return (root || d).querySelector(sel); }
    function qa(sel, root) { return Array.prototype.slice.call((root || d).querySelectorAll(sel)); }

    function el(tag, attrs, children) {
        var node = d.createElement(tag);
        Object.keys(attrs || {}).forEach(function (k) {
            if (k === 'text') { node.textContent = attrs[k]; }
            else if (k === 'class') { node.className = attrs[k]; }
            else { node.setAttribute(k, attrs[k]); }
        });
        (children || []).forEach(function (c) { if (c) { node.appendChild(c); } });
        return node;
    }

    function clear(node) { while (node.firstChild) { node.removeChild(node.firstChild); } }

    /* ---------- API ---------- */
    function api(path, opts) {
        opts = opts || {};
        var headers = {'Accept': 'application/json, text/plain, */*'};
        var init = {method: opts.method || 'GET', credentials: 'same-origin', headers: headers};
        if (init.method !== 'GET') {
            headers['Content-Type'] = 'application/json';
            headers['X-Requested-With'] = 'ecm';
            init.body = JSON.stringify(opts.body || {});
        }
        return fetch(path, init).then(function (res) {
            return res.text().then(function (text) {
                var data = text;
                try { data = text ? JSON.parse(text) : null; } catch (e) { /* plain text answer */ }
                if (!res.ok) {
                    var err = new Error('HTTP ' + res.status);
                    err.status = res.status;
                    err.data = data;
                    throw err;
                }
                return data;
            });
        });
    }

    function qs(params) {
        var out = [];
        Object.keys(params).forEach(function (k) {
            var v = params[k];
            if (v !== undefined && v !== null && v !== '') {
                out.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
            }
        });
        return out.join('&');
    }

    /* The access token cookie is HttpOnly, so login state is learned from the API: 200 JSON = signed in. */
    var loginProbe;
    /* The platform sets a readable accessTokenExpiredAt cookie next to the HttpOnly token; without it the visitor
       is certainly a guest, so the probe (a 401 the browser logs to the console) is skipped. */
    function hasSessionCookie() {
        return /(?:^|;\s*)accessTokenExpiredAt=/.test(d.cookie);
    }
    function loggedIn() {
        if (!loginProbe && !hasSessionCookie()) { loginProbe = Promise.resolve(false); }
        if (!loginProbe) {
            loginProbe = fetch('/api/v1/shopping-cart/product-count', {credentials: 'same-origin', redirect: 'manual'})
                .then(function (r) {
                    return r.status === 200 && r.type === 'basic' &&
                        (r.headers.get('content-type') || '').indexOf('json') >= 0;
                }, function () { return false; });
        }
        return loginProbe;
    }

    /* Run a page initialiser only for a signed-in visitor, otherwise send them to the login page. */
    function gated(path, init) {
        return function () {
            loggedIn().then(function (ok) {
                if (!ok) { goLogin(typeof path === 'function' ? path() : path); return; }
                init();
            });
        };
    }

    function goLogin(next) {
        location.href = '/login?next=' + encodeURIComponent(next || location.pathname);
    }

    function safeNext() {
        var n = new URLSearchParams(location.search).get('next');
        // internal path only: one leading slash, no backslash, no control chars, no scheme
        return n && /^\/(?![\/\\])[^\\\u0000-\u001f\u007f]*$/.test(n) && n.indexOf('://') < 0 ? n : '';
    }

    /* ---------- money ---------- */
    function isoOf(p) {
        var m = /([A-Z]{3})\s*$/.exec(p.formattedPriceIncludeIsoCode || '');
        return m ? m[1] : '';
    }
    function money(amount, iso, fallback) {
        if (iso === 'VND') {
            return Math.round(Number(amount)).toLocaleString('vi-VN') + ' ₫';
        }
        return fallback || (Number(amount).toLocaleString('en-US') + (iso ? ' ' + iso : ''));
    }
    /* Order endpoints send ready-made money text ("2,700,000₫"); shown the same way as cart and checkout instead. */
    function orderMoney(text) {
        var m = /^([^\d.,\s-]*)\s*([\d.,]+)\s*([^\d.,\s]*)$/.exec(String(text || '').trim());
        if (!m) { return text || ''; }
        var sym = m[1] || m[3];
        var digits = m[2];
        if (sym === '₫' || sym === 'VND') {
            return money(digits.replace(/[.,]/g, ''), 'VND');
        }
        var n = Number(digits.replace(/,/g, ''));
        return isFinite(n) ? sym + n.toLocaleString('en-US') : text;
    }
    function priceText(p) {
        return money(p.price, isoOf(p), p.formattedPriceIncludeSymbol);
    }
    function originalText(p) {
        if (!p.originalPrice) { return ''; }
        return money(p.originalPrice, isoOf(p), p.formattedOriginalPriceIncludeSymbol);
    }
    function imageUrl(media) {
        if (!media) { return ''; }
        if (media.url) { return media.url; }
        return media.name ? '/api/v1/media/' + encodeURIComponent(media.name) : '';
    }

    var STATUS_VI = {
        pending: 'đang chờ xử lý', unpaid: 'chưa thanh toán', paid: 'đã thanh toán', processing: 'đang xử lý',
        confirmed: 'đã xác nhận', shipping: 'đang giao hàng', shipped: 'đã giao cho đơn vị vận chuyển',
        delivered: 'đã giao hàng', completed: 'hoàn tất', cancelled: 'đã hủy', canceled: 'đã hủy',
        refunded: 'đã hoàn tiền', failed: 'thất bại', waiting_payment: 'chờ thanh toán', created: 'đã tạo đơn'
    };
    function statusText(v) {
        var key = String(v || '').toLowerCase();
        return STATUS_VI[key] || key.replace(/_/g, ' ');
    }
    function dateText(ms, withTime) {
        if (!ms) { return ''; }
        var dt = new Date(ms);
        return withTime ? dt.toLocaleString('vi-VN') : dt.toLocaleDateString('vi-VN');
    }

    function priceNode(p) {
        var node = el('p', {'class': 'kn-price'});
        node.appendChild(d.createTextNode(priceText(p)));
        var was = originalText(p);
        if (was && p.discountPercent > 0) {
            node.appendChild(el('s', {text: was}));
        }
        return node;
    }

    /* ---------- state boxes ---------- */
    function setState(root, kind, message, retry) {
        var box = q('[data-kn-state]', root || d);
        if (!box) { return; }
        clear(box);
        if (!kind) { return; }
        var s = el('div', {'class': 'kn-state', 'data-kind': kind, role: kind === 'error' ? 'alert' : 'status'});
        if (kind === 'loading') { s.appendChild(el('span', {'class': 'kn-spin', 'aria-hidden': 'true'})); }
        s.appendChild(d.createTextNode(message));
        if (retry) {
            var btn = el('button', {type: 'button', 'class': 'kn-btn kn-btn--ghost kn-btn--inline', text: 'Thử lại'});
            btn.addEventListener('click', retry);
            s.appendChild(el('div', {}, [btn]));
        }
        box.appendChild(s);
    }

    /* ---------- bag ---------- */
    function readBag() {
        try {
            var v = JSON.parse(localStorage.getItem(BAG_KEY) || '[]');
            return Array.isArray(v) ? v.filter(function (l) { return l && l.id > 0 && l.qty > 0; }) : [];
        } catch (e) { return []; }
    }
    function writeBag(lines) {
        localStorage.setItem(BAG_KEY, JSON.stringify(lines));
        renderBagCount();
    }
    function addToBag(id, qty) {
        var lines = readBag();
        var line = lines.filter(function (l) { return l.id === id; })[0];
        if (line) { line.qty += qty; } else { lines.push({id: id, qty: qty}); }
        writeBag(lines);
    }
    function renderBagCount() {
        var n = readBag().reduce(function (s, l) { return s + l.qty; }, 0);
        var badge = q('#swBagCount');
        if (!badge) { return; }
        badge.textContent = String(n);
        badge.hidden = n === 0;
    }

    /* ---------- categories ---------- */
    var catPromise;
    var catsReal = false;
    function loadCategories() {
        if (catPromise) { return catPromise; }
        try {
            var cached = JSON.parse(sessionStorage.getItem(CAT_KEY) || 'null');
            if (cached && Date.now() - cached.at < CAT_TTL && cached.items.length) {
                catsReal = true;
                catPromise = Promise.resolve(cached.items);
                return catPromise;
            }
        } catch (e) { /* ignore */ }
        catPromise = api('/api/v1/product-categories').then(function (r) {
            var items = (r && r.items || []).slice().sort(function (a, c) {
                return (a.displayOrder || 0) - (c.displayOrder || 0);
            });
            if (!items.length) { return FALLBACK_CATEGORIES; }
            catsReal = true;
            try { sessionStorage.setItem(CAT_KEY, JSON.stringify({at: Date.now(), items: items})); } catch (e) { /* quota */ }
            return items;
        }).catch(function () { return FALLBACK_CATEGORIES; });
        return catPromise;
    }
    function catLink(c) {
        return el('a', {href: '/categories/' + encodeURIComponent(c.name), text: c.displayName || c.name});
    }
    /* A list keeps its [data-kn-keep] items (fixed links such as the jade knowledge page) after the categories. */
    function fillCategoryList(ul, cats) {
        var keep = qa('[data-kn-keep]', ul);
        clear(ul);
        var chips = ul.hasAttribute('data-kn-chips');
        cats.forEach(function (c) {
            var a = catLink(c);
            if (chips) { a.className = 'kn-chip'; }
            ul.appendChild(el('li', {}, [a]));
        });
        keep.forEach(function (n) { ul.appendChild(n); });
    }
    function renderCategoryLists(cats) {
        ['#swCats', '#swDrawerCats', '#swFootCats', '#swHomeCats'].forEach(function (sel) {
            var ul = q(sel);
            if (ul) { fillCategoryList(ul, cats); }
        });
    }

    /* ---------- product grid ---------- */
    /* ---------- wish list ---------- */
    var wishSet = null;
    var wishPromise = null;
    var HEART_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M12 20.500S3.500 15 3.500 9.200A4.700 4.700 0 0112 6.700a4.700 4.700 0 018.500 2.500C20.500 15 12 20.500 12 20.500z"/></svg>';

    /* The ids the signed-in visitor has saved; a guest has none and costs no request. */
    function loadWish() {
        if (!wishPromise) {
            wishPromise = loggedIn().then(function (ok) {
                if (!ok) { return {}; }
                return api('/api/v1/ecm/wishlist/ids').then(function (r) {
                    var m = {};
                    (r && r.ids || []).forEach(function (id) { m[id] = true; });
                    return m;
                }).catch(function () { return {}; });
            }).then(function (m) { wishSet = m; return m; });
        }
        return wishPromise;
    }

    function paintHeart(btn) {
        var on = !!(wishSet && wishSet[btn.getAttribute('data-pid')]);
        btn.setAttribute('aria-pressed', on ? 'true' : 'false');
        btn.setAttribute('aria-label', on ? 'Bỏ khỏi danh sách yêu thích' : 'Thêm vào danh sách yêu thích');
    }

    function paintHearts() {
        qa('[data-pid]').forEach(function (btn) {
            if (btn.hasAttribute('aria-pressed')) { paintHeart(btn); }
        });
        qa('[data-wish-label]').forEach(function (btn) {
            btn.textContent = btn.getAttribute('aria-pressed') === 'true' ? 'Đã lưu vào yêu thích' : 'Lưu vào yêu thích';
        });
    }

    function notify(message) {
        var box = q('#swToast');
        if (!box) {
            box = el('div', {id: 'swToast', 'class': 'kn-toast', role: 'status'});
            d.body.appendChild(box);
        }
        box.textContent = message;
        box.hidden = false;
        clearTimeout(notify.timer);
        notify.timer = setTimeout(function () { box.hidden = true; }, 4000);
    }

    /* Flip one product. A guest is sent to sign in and brought back; a failed save is undone. */
    function toggleWish(id) {
        loggedIn().then(function (ok) {
            if (!ok) { goLogin(location.pathname + location.search); return; }
            loadWish().then(function (set) {
                var turnOn = !set[id];
                if (turnOn) { set[id] = true; } else { delete set[id]; }
                paintHearts();
                api('/api/v1/ecm/wishlist/' + encodeURIComponent(id), {method: turnOn ? 'PUT' : 'DELETE'})
                    .then(function () {
                        d.dispatchEvent(new CustomEvent('ecm:wish', {detail: {id: id, on: turnOn}}));
                    })
                    .catch(function (e) {
                        if (turnOn) { delete set[id]; } else { set[id] = true; }
                        paintHearts();
                        if (e.status === 401) { goLogin(location.pathname + location.search); return; }
                        notify('Chưa cập nhật được danh sách yêu thích. Vui lòng thử lại.');
                    });
            });
        });
    }

    function heartButton(id, extraClass) {
        var btn = el('button', {type: 'button', 'class': 'kn-heart' + (extraClass ? ' ' + extraClass : ''),
            'data-pid': String(id), 'aria-pressed': 'false', 'aria-label': 'Thêm vào danh sách yêu thích'});
        btn.innerHTML = HEART_SVG;
        btn.addEventListener('click', function () { toggleWish(id); });
        loadWish().then(function () { paintHeart(btn); });
        return btn;
    }

    function productCard(p) {
        var imgBox = el('figure', {});
        var src = imageUrl(p.iconImage || p.bannerImage);
        if (src) {
            imgBox.appendChild(el('img', {src: src, alt: p.name, loading: 'lazy'}));
        }
        if (p.discountPercent > 0) {
            imgBox.appendChild(el('span', {'class': 'kn-tag', text: '-' + Math.round(p.discountPercent) + '%'}));
        }
        var price = priceNode(p);
        price.className = 'kn-cell__price';
        var a = el('a', {href: '/products/' + encodeURIComponent(p.code)}, [
            imgBox,
            el('div', {}, [
                el('h3', {text: p.name}),
                el('code', {'class': 'kn-cell__code', text: p.code}),
                price
            ])
        ]);
        return el('li', {'class': 'kn-cell', 'data-pid-card': String(p.id)}, [a, heartButton(p.id)]);
    }

    function productQuery(extra) {
        return qs(Object.assign({productTypes: 'SINGLE'}, extra));
    }

    // Colour, material and product family per product code (static file built from the product names).
    var attrsPromise;
    function loadAttributes() {
        if (!attrsPromise) {
            attrsPromise = api('/data/product-attributes.json').catch(function () { return {}; });
        }
        return attrsPromise;
    }

    function pageList(grid, root, state) {
        var limit = Number(grid.getAttribute('data-limit')) || 12;
        var nextToken = null;
        var loadBtn = q('#swLoadMore');
        var countEl = q('#swCount');
        var params = state.params;
        var loaded = 0;

        function fetchPage(append) {
            if (!append) { clear(grid); loaded = 0; nextToken = null; setState(root, 'loading', 'Đang tải sản phẩm…'); }
            if (loadBtn) { loadBtn.disabled = true; }
            var p = Object.assign({}, params, {limit: limit, nextPageToken: append ? nextToken : ''});
            return api('/api/v1/products?' + productQuery(p)).then(function (r) {
                setState(root, null);
                var items = r.items || [];
                items.forEach(function (it) { grid.appendChild(productCard(it)); });
                loaded += items.length;
                nextToken = r.continuation && r.continuation.hasNext ? r.pageToken.next : null;
                if (countEl) { countEl.textContent = r.total ? loaded + ' / ' + r.total + ' sản phẩm' : ''; }
                if (!loaded) {
                    setState(root, 'empty', 'Không có sản phẩm nào phù hợp với lựa chọn của bạn.');
                }
                if (loadBtn) { loadBtn.hidden = !nextToken; loadBtn.disabled = false; }
            }).catch(function (e) {
                if (e.status === 404 || (e.status === 400 && e.data && e.data.category)) {
                    setState(root, 'empty', 'Danh mục này chưa có sản phẩm.');
                    if (loadBtn) { loadBtn.hidden = true; }
                    if (countEl) { countEl.textContent = ''; }
                    return;
                }
                setState(root, 'error', 'Chưa tải được danh sách sản phẩm.', function () { fetchPage(append); });
                if (loadBtn) { loadBtn.disabled = false; }
            });
        }
        if (loadBtn) { loadBtn.addEventListener('click', function () { fetchPage(true); }); }
        return fetchPage;
    }

    /* ---------- pages ---------- */
    var pages = {};

    pages.home = function () {
        var grid = q('#swGrid');
        var root = grid.closest('section');
        pageList(grid, root, {params: {sortOrder: ''}})(false);
    };

    pages.products = function () {
        var root = q('#swMain');
        var grid = q('#swGrid');
        var url = new URLSearchParams(location.search);
        var slug = PARAM || url.get('category') || '';
        var sel = q('#swFilterCat');
        var sort = q('#swSort');
        var kw = q('#swKeyword');
        var priceMin = q('#swPriceMin');
        var priceMax = q('#swPriceMax');
        var discount = q('#swDiscount');
        var colorSel = q('#swColor');
        var materialSel = q('#swMaterial');
        var titleEl = q('#swListTitle');
        var bc = q('#swBcCur');
        kw.value = url.get('keyword') || '';
        sort.value = url.get('sort') || '';
        priceMin.value = nonNegative(url.get('priceMin'));
        priceMax.value = nonNegative(url.get('priceMax'));
        discount.value = url.get('discount') || '';
        if (discount.value !== (url.get('discount') || '')) { discount.value = ''; }
        var wantColor = url.get('color') || '';
        var wantMaterial = url.get('material') || '';
        var attrs = {};

        function nonNegative(v) {
            var n = Number(v);
            return v !== null && v !== '' && isFinite(n) && n >= 0 ? String(n) : '';
        }
        function apply() {
            var cat = sel.value;
            var keyword = kw.value.trim();
            var chosen = sel.options[sel.selectedIndex];
            var title = keyword ? 'Tìm kiếm: ' + keyword : (cat ? chosen.textContent : 'Tất cả sản phẩm');
            titleEl.textContent = title;
            bc.textContent = title;
            d.title = title + ' | Khang Nguyên · Ngọc Bảo';
            var params = {categoryName: cat, keyword: keyword, sortOrder: sort.value};
            if (priceMin.value !== '') { params.priceGte = priceMin.value; }
            // the server compares "greater than" strictly, so "10% or more" is "> 9"
            if (discount.value) { params.discountPercentGt = Number(discount.value) - 1; }
            pageList2({params: params, attr: {color: colorSel.value, material: materialSel.value, maxPrice: priceMax.value}});
        }
        var loadBtn = q('#swLoadMore');
        var countEl0 = q('#swCount');
        var current;
        function pageList2(pState) {
            if (location.pathname === '/search' && !pState.params.keyword) {
                clear(grid);
                countEl0.textContent = '';
                if (loadBtn) { loadBtn.hidden = true; }
                setState(root, 'empty', 'Nhập từ khóa để tìm trong bộ sưu tập.');
                return;
            }
            if (pState.params.categoryName && !catsReal) {
                clear(grid);
                countEl0.textContent = '';
                if (loadBtn) { loadBtn.hidden = true; }
                setState(root, 'empty', 'Danh mục này chưa có sản phẩm.');
                return;
            }
            if (!current) {
                current = pState;
                current.fetch = pageListFetch(current);
            } else {
                current.params = pState.params;
                current.attr = pState.attr;
            }
            current.fetch(false);
            return current.fetch;
        }
        function pageListFetch(state) {
            var limit = Number(grid.getAttribute('data-limit')) || 12;
            var nextToken = null;
            var loaded = 0;
            var countEl = q('#swCount');
            var seq = 0;
            var buffer = null;
            var shown = 0;
            function isPriceSort() {
                return state.params.sortOrder === 'PRICE_ASC' || state.params.sortOrder === 'PRICE_DESC';
            }
            // colour and material only exist in the static file, and the products API does not apply
            // priceLte to the displayed price, so these are filtered here on the full list
            function hasAttrFilter() {
                return !!(state.attr && (state.attr.color || state.attr.material || state.attr.maxPrice !== ''));
            }
            function matchesAttr(it) {
                var a = attrs[it.code] || {};
                if (state.attr.maxPrice !== '' && Number(it.price) > Number(state.attr.maxPrice)) { return false; }
                if (state.attr.color && (a.colors || []).indexOf(state.attr.color) < 0) { return false; }
                if (state.attr.material && (a.materials || []).indexOf(state.attr.material) < 0) { return false; }
                return true;
            }
            function finish(total) {
                countEl.textContent = total ? loaded + ' / ' + total + ' sản phẩm' : '';
                if (!loaded) { setState(root, 'empty', 'Không có sản phẩm nào phù hợp với lựa chọn của bạn.'); }
            }
            function fetchAll(my, base, token, acc) {
                var p = Object.assign({}, base, {limit: 100, nextPageToken: token || ''});
                return api('/api/v1/products?' + productQuery(p)).then(function (r) {
                    acc = acc.concat(r.items || []);
                    if (r.continuation && r.continuation.hasNext && my === seq) {
                        return fetchAll(my, base, r.pageToken.next, acc);
                    }
                    return acc;
                });
            }
            function showSlice() {
                var slice = buffer.slice(shown, shown + limit);
                slice.forEach(function (it) { grid.appendChild(productCard(it)); });
                shown += slice.length;
                loaded = shown;
                finish(buffer.length);
                if (loadBtn) { loadBtn.hidden = shown >= buffer.length; loadBtn.disabled = false; }
            }
            function fail(my, e, retry) {
                if (my !== seq) { return; }
                countEl.textContent = '';
                if (loadBtn) { loadBtn.hidden = true; loadBtn.disabled = false; }
                if (e.status === 404 || (e.status === 400 && e.data && e.data.category)) {
                    setState(root, 'empty', 'Danh mục này chưa có sản phẩm.');
                    return;
                }
                setState(root, 'error', 'Chưa tải được danh sách sản phẩm.', retry);
            }
            function fetchPage(append) {
                var my = ++seq;
                if (!append) { clear(grid); loaded = 0; nextToken = null; buffer = null; shown = 0; if (loadBtn) { loadBtn.hidden = true; } setState(root, 'loading', 'Đang tải sản phẩm…'); }
                if (loadBtn) { loadBtn.disabled = true; }
                if (isPriceSort() || hasAttrFilter()) {
                    if (append && buffer) { showSlice(); return; }
                    var base = Object.assign({}, state.params);
                    delete base.sortOrder;
                    fetchAll(my, base, '', []).then(function (all) {
                        if (my !== seq) { return; }
                        if (hasAttrFilter()) { all = all.filter(matchesAttr); }
                        if (isPriceSort()) {
                            var dir = state.params.sortOrder === 'PRICE_ASC' ? 1 : -1;
                            all = all.sort(function (x, y) { return dir * (Number(x.price) - Number(y.price)); });
                        }
                        buffer = all;
                        setState(root, null);
                        showSlice();
                    }).catch(function (e) { fail(my, e, function () { fetchPage(false); }); });
                    return;
                }
                var p = Object.assign({}, state.params, {limit: limit, nextPageToken: append ? nextToken : ''});
                api('/api/v1/products?' + productQuery(p)).then(function (r) {
                    if (my !== seq) { return; }
                    setState(root, null);
                    var items = r.items || [];
                    items.forEach(function (it) { grid.appendChild(productCard(it)); });
                    loaded += items.length;
                    nextToken = r.continuation && r.continuation.hasNext ? r.pageToken.next : null;
                    finish(r.total);
                    if (loadBtn) { loadBtn.hidden = !nextToken; loadBtn.disabled = false; }
                }).catch(function (e) { fail(my, e, function () { fetchPage(append); }); });
            }
            if (loadBtn && !loadBtn.dataset.bound) {
                loadBtn.dataset.bound = '1';
                loadBtn.addEventListener('click', function () { fetchPage(true); });
            }
            return fetchPage;
        }

        var dropped = [];
        function showDroppedFilters() {
            var note = q('#swFilterNote');
            if (!note) {
                note = el('p', {'class': 'kn-note', id: 'swFilterNote', role: 'status'});
                q('#swTools').insertAdjacentElement('afterend', note);
            }
            note.textContent = dropped.length
                ? 'Chưa có ' + dropped.join(' hoặc ') + ', nên bộ lọc đó chưa được áp dụng.'
                : '';
        }
        function fillAttrSelect(select, wrap, key, wanted, label) {
            var seen = {};
            Object.keys(attrs).forEach(function (code) {
                (attrs[code][key] || []).forEach(function (v) { seen[v] = true; });
            });
            var values = Object.keys(seen).sort();
            if (!values.length) { return; }
            values.forEach(function (v) { select.appendChild(el('option', {value: v, text: v})); });
            select.value = wanted;
            if (select.value !== wanted) {
                select.value = '';
                if (wanted) { dropped.push(label + ' "' + wanted + '"'); }
            }
            wrap.hidden = false;
        }
        Promise.all([loadCategories(), loadAttributes()]).then(function (res) {
            var cats = res[0];
            attrs = res[1] || {};
            cats.forEach(function (c) {
                sel.appendChild(el('option', {value: c.name, text: c.displayName || c.name}));
            });
            sel.value = slug;
            if (sel.value !== slug) { sel.value = ''; }
            fillAttrSelect(colorSel, q('#swColorWrap'), 'colors', wantColor, 'màu sắc');
            fillAttrSelect(materialSel, q('#swMaterialWrap'), 'materials', wantMaterial, 'chất liệu');
            showDroppedFilters();
            apply();
        });
        q('#swTools').addEventListener('submit', function (e) {
            e.preventDefault();
            var u = new URLSearchParams();
            if (sel.value) { u.set('category', sel.value); }
            if (kw.value.trim()) { u.set('keyword', kw.value.trim()); }
            if (sort.value) { u.set('sort', sort.value); }
            if (priceMin.value !== '') { u.set('priceMin', priceMin.value); }
            if (priceMax.value !== '') { u.set('priceMax', priceMax.value); }
            if (discount.value) { u.set('discount', discount.value); }
            if (colorSel.value) { u.set('color', colorSel.value); }
            if (materialSel.value) { u.set('material', materialSel.value); }
            history.replaceState(null, '', location.pathname + (u.toString() ? '?' + u.toString() : ''));
            apply();
        });
        sort.addEventListener('change', function () { q('#swTools').requestSubmit(); });
        sel.addEventListener('change', function () { q('#swTools').requestSubmit(); });
        discount.addEventListener('change', function () { q('#swTools').requestSubmit(); });
        colorSel.addEventListener('change', function () { q('#swTools').requestSubmit(); });
        materialSel.addEventListener('change', function () { q('#swTools').requestSubmit(); });
    };

    function stockText(p) {
        if (p.amount > 5) { return 'Còn hàng'; }
        if (p.amount === 1) { return 'Chỉ còn 1 sản phẩm, mỗi món là duy nhất'; }
        if (p.amount > 0) { return 'Chỉ còn ' + p.amount + ' sản phẩm'; }
        return 'Hết hàng';
    }

    // Other products of the same family are shown as colour choices; picking one opens that product.
    function renderSwatches(p) {
        loadAttributes().then(function (attrs) {
            var mine = attrs[p.code];
            if (!mine || !mine.family || !(mine.colors || []).length) { return; }
            var family = Object.keys(attrs).filter(function (code) {
                return attrs[code].family === mine.family && (attrs[code].colors || []).length;
            }).sort();
            if (family.length < 2) { return; }
            var list = q('#swPdpSwatchList');
            clear(list);
            family.forEach(function (code) {
                var color = attrs[code].colors[0];
                var current = code === p.code;
                var link = el('a', {
                    href: '/products/' + encodeURIComponent(code),
                    'class': 'kn-chip' + (current ? ' is-active' : ''),
                    text: color
                });
                if (current) { link.setAttribute('aria-current', 'true'); }
                list.appendChild(el('li', {}, [link]));
            });
            q('#swPdpColor').textContent = mine.colors[0];
            q('#swPdpSwatches').hidden = false;
        });
    }

    pages.product = function () {
        var root = q('#swMain');
        var code = PARAM;
        var qty = q('#swPdpQty');
        var product;
        var wishBtn = q('#swWishBtn');
        function load() {
            setState(root, 'loading', 'Đang tải sản phẩm…');
            api('/api/v1/products/' + encodeURIComponent(code) + '/details').then(function (p) {
                product = p;
                setState(root, null);
                d.title = p.name + ' | Khang Nguyên · Ngọc Bảo';
                q('#swBcCur').textContent = p.name;
                q('#swPdpCode').textContent = 'Mã ' + p.code;
                q('#swPdpName').textContent = p.name;
                var priceBox = q('#swPdpPrice');
                priceBox.textContent = priceText(p);
                var was = originalText(p);
                if (was && p.discountPercent > 0) { priceBox.appendChild(el('s', {text: was})); }
                q('#swPdpStock').textContent = stockText(p);
                var box = q('#swPdpImg');
                clear(box);
                var src = imageUrl(p.bannerImage || p.iconImage);
                if (src) { box.appendChild(el('img', {src: src, alt: p.name})); }
                q('#swPdpDescText').textContent = p.description || p.name;
                var inStock = p.amount > 0;
                var addBtn = q('#swAddBtn');
                addBtn.disabled = !inStock;
                addBtn.textContent = inStock ? 'Thêm vào giỏ hàng' : 'Hết hàng';
                q('#swPdpQty').disabled = !inStock;
                qa('[data-qty-dec],[data-qty-inc]').forEach(function (n) { n.disabled = !inStock; });
                // a single piece cannot be ordered twice, so the quantity picker is not offered
                var qtyRow = q('#swPdpQtyRow');
                if (qtyRow) { qtyRow.hidden = p.amount === 1; }
                if (p.amount === 1) { qty.value = '1'; }
                wishBtn.setAttribute('data-pid', String(p.id));
                wishBtn.setAttribute('aria-pressed', 'false');
                loadWish().then(paintHearts);
                q('#swPdp').hidden = false;
                renderSwatches(p);
            }).catch(function (e) {
                if (e.status === 404 || e.status === 400) {
                    setState(root, 'empty', 'Không tìm thấy sản phẩm này.');
                } else {
                    setState(root, 'error', 'Chưa tải được sản phẩm này.', load);
                }
            });
        }
        q('[data-qty-dec]').addEventListener('click', function () { qty.value = Math.max(1, (Number(qty.value) || 1) - 1); });
        q('[data-qty-inc]').addEventListener('click', function () { qty.value = (Number(qty.value) || 1) + 1; });
        wishBtn.setAttribute('data-wish-label', '');
        wishBtn.addEventListener('click', function () { if (product) { toggleWish(product.id); } });
        q('#swAddBtn').addEventListener('click', function () {
            var n = Math.max(1, Math.floor(Number(qty.value) || 1));
            if (product.amount && n > product.amount) { n = product.amount; qty.value = n; }
            addToBag(product.id, n);
            var note = q('#swAddNote');
            note.textContent = '';
            var link = el('a', {href: '/shopping-cart', 'class': 'kn-link', text: 'Xem giỏ hàng'});
            note.appendChild(d.createTextNode('Đã thêm ' + n + ' sản phẩm vào giỏ hàng. '));
            note.appendChild(link);
        });
        load();
    };

    function loadLines() {
        var bag = readBag();
        return Promise.all(bag.map(function (l) {
            return api('/api/v1/products/' + l.id).then(function (p) { return {line: l, product: p}; })
                .catch(function (e) {
                    if (e.status === 404 || e.status === 400) { return null; }
                    throw e;
                });
        })).then(function (rows) {
            var good = rows.filter(Boolean);
            if (good.length !== bag.length) {
                writeBag(good.map(function (r) { return r.line; }));
            }
            return good;
        });
    }

    function subtotal(rows) {
        var total = 0;
        var sample = rows[0] && rows[0].product;
        rows.forEach(function (r) { total += (Number(r.product.price) || 0) * r.line.qty; });
        var iso = sample ? isoOf(sample) : '';
        if (iso === 'VND') { return money(total, iso); }
        var shown = sample && sample.formattedPriceIncludeSymbol;
        var symbol = shown ? shown.replace(/[\d.,\s]+/g, '') : '';
        return symbol ? symbol + total.toLocaleString('en-US') : money(total, iso);
    }

    pages.cart = function () {
        var root = q('#swMain');
        var wrap = q('#swCart');
        var list = q('#swLines');
        function render() {
            setState(root, 'loading', 'Đang tải giỏ hàng…');
            loadLines().then(function (rows) {
                setState(root, null);
                clear(list);
                if (!rows.length) {
                    wrap.hidden = true;
                    var empty = el('div', {'class': 'kn-state', role: 'status'}, [
                        el('p', {text: 'Giỏ hàng của bạn đang trống.'}),
                        el('a', {'class': 'kn-btn kn-btn--inline', href: '/products', text: 'Tiếp tục xem sản phẩm'})
                    ]);
                    q('[data-kn-state]', root).appendChild(empty);
                    return;
                }
                rows.forEach(function (r) { list.appendChild(lineNode(r, render)); });
                q('#swSubtotal').textContent = subtotal(rows);
                wrap.hidden = false;
            }).catch(function () {
                wrap.hidden = true;
                setState(root, 'error', 'Chưa tải được giỏ hàng.', render);
            });
        }
        render();
    };

    function lineNode(r, rerender) {
        var p = r.product;
        var imgBox = el('a', {'class': 'kn-line__img', href: '/products/' + encodeURIComponent(p.code), tabindex: '-1', 'aria-hidden': 'true'});
        var src = imageUrl(p.iconImage || p.bannerImage);
        if (src) { imgBox.appendChild(el('img', {src: src, alt: ''})); }
        var minus = el('button', {type: 'button', 'aria-label': 'Giảm số lượng ' + p.name, text: '−'});
        var plus = el('button', {type: 'button', 'aria-label': 'Tăng số lượng ' + p.name, text: '+'});
        var input = el('input', {type: 'number', min: '1', value: String(r.line.qty), inputmode: 'numeric', 'aria-label': 'Số lượng ' + p.name});
        function setQty(n) {
            n = Math.max(1, Math.floor(n) || 1);
            if (p.amount && n > p.amount) { n = p.amount; }
            var lines = readBag();
            lines.forEach(function (l) { if (l.id === p.id) { l.qty = n; } });
            writeBag(lines);
            rerender();
        }
        minus.addEventListener('click', function () { setQty(r.line.qty - 1); });
        plus.addEventListener('click', function () { setQty(r.line.qty + 1); });
        input.addEventListener('change', function () { setQty(Number(input.value)); });
        var remove = el('button', {type: 'button', 'class': 'kn-rm', text: 'Xóa khỏi giỏ'});
        remove.addEventListener('click', function () {
            writeBag(readBag().filter(function (l) { return l.id !== p.id; }));
            rerender();
        });
        var info = el('div', {}, [
            el('h3', {}, [el('a', {href: '/products/' + encodeURIComponent(p.code), text: p.name})]),
            el('p', {'class': 'kn-line__meta', text: 'Mã ' + p.code}),
            p.amount === 1 ? null : el('div', {'class': 'kn-qty'}, [minus, input, plus])
        ]);
        var end = el('div', {'class': 'kn-line__end'}, [
            el('p', {'class': 'kn-line__price', text: lineTotalText(p, r.line.qty)}),
            remove
        ]);
        return el('li', {'class': 'kn-line'}, [imgBox, info, end]);
    }

    function lineTotalText(p, qty) {
        var iso = isoOf(p);
        if (iso === 'VND') { return money(Number(p.price) * qty, iso); }
        var sym = (p.formattedPriceIncludeSymbol || '').replace(/[\d.,\s]/g, '');
        return (sym || '') + (Number(p.price) * qty).toLocaleString('en-US') + (sym ? '' : ' ' + iso);
    }

    pages.checkout = gated('/checkout', function () {
        var root = q('#swMain');
        var form = q('#swCheckoutForm');
        var wrap = q('#swCheckout');
        var errBox = q('#swFormErr');
        var rowsCache = [];
        var currency;

        function showErr(msg, field) {
            errBox.textContent = msg;
            errBox.hidden = !msg;
            qa('[aria-invalid]', form).forEach(function (n) { n.removeAttribute('aria-invalid'); });
            if (field) { field.setAttribute('aria-invalid', 'true'); field.focus(); }
        }

        function boot() {
            setState(root, 'loading', 'Đang chuẩn bị thanh toán…');
            Promise.all([
                loadLines(),
                api('/api/v1/currencies/default'),
                api('/api/v1/countries'),
                api('/api/v1/available-payment-services')
            ]).then(function (res) {
                rowsCache = res[0];
                currency = res[1];
                setState(root, null);
                if (!rowsCache.length) {
                    setState(root, 'empty', 'Giỏ hàng của bạn đang trống.');
                    q('[data-kn-state]', root).firstChild.appendChild(el('div', {}, [el('a', {'class': 'kn-btn kn-btn--inline', href: '/products', text: 'Tiếp tục xem sản phẩm'})]));
                    return;
                }
                var countries = Array.isArray(res[2]) ? res[2] : (res[2].items || []);
                var cSel = q('#swCountry');
                clear(cSel);
                var vietnam = '';
                countries.forEach(function (c) {
                    var label = c.localName || c.internationalName || c.code;
                    if (/^(vn|vnm)$/i.test(c.code || '') || /viet\s?nam/i.test(label + ' ' + (c.internationalName || ''))) { vietnam = String(c.id); }
                    cSel.appendChild(el('option', {value: String(c.id), text: label}));
                });
                if (vietnam) { cSel.value = vietnam; }
                var services = Array.isArray(res[3]) ? res[3] : (res[3].items || res[3].paymentServices || []);
                var pSel = q('#swPay');
                clear(pSel);
                services.forEach(function (s) {
                    var code = s.code || s.paymentServiceCode || s.name;
                    pSel.appendChild(el('option', {value: code, text: s.displayName || s.name || code}));
                });
                var mini = q('#swMini');
                clear(mini);
                rowsCache.forEach(function (r) {
                    mini.appendChild(el('li', {}, [
                        el('span', {text: r.line.qty + ' × ' + r.product.name}),
                        el('span', {text: lineTotalText(r.product, r.line.qty)})
                    ]));
                });
                q('#swTotal').textContent = subtotal(rowsCache);
                api('/ezyaccount/api/v1/account').then(function (a) {
                    if (a && a.displayName && !form.deliveryFullName.value) { form.deliveryFullName.value = a.displayName; }
                    if (a && a.email && !form.deliveryEmail.value) { form.deliveryEmail.value = a.email; }
                }).catch(function () { /* prefill is optional */ });
                wrap.hidden = false;
            }).catch(function (e) {
                if (e.status === 401) { goLogin('/checkout'); return; }
                setState(root, 'error', 'Chưa chuẩn bị được trang thanh toán.', boot);
            });
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            showErr('');
            var f = form.elements;
            var required = ['deliveryFullName', 'deliveryEmail', 'deliveryPhoneNumber', 'deliveryAddress', 'deliveryCityName'];
            for (var i = 0; i < required.length; i++) {
                if (!f[required[i]].value.trim()) { showErr('Vui lòng điền đủ thông tin giao hàng.', f[required[i]]); return; }
            }
            if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.deliveryEmail.value.trim())) { showErr('Nhập địa chỉ email hợp lệ.', f.deliveryEmail); return; }
            if (!f.paymentServiceCode.value) { showErr('Hiện chưa có phương thức thanh toán nào.', f.paymentServiceCode); return; }
            var btn = q('#swPlace');
            btn.disabled = true;
            btn.textContent = 'Đang đặt hàng…';
            var amountByProductId = {};
            rowsCache.forEach(function (r) { amountByProductId[r.product.id] = r.line.qty; });
            api('/api/v1/checkout', {method: 'POST', body: {
                productIds: rowsCache.map(function (r) { return r.product.id; }),
                amountByProductId: amountByProductId
            }}).then(function (cart) {
                return api('/api/v1/orders/create-payment-url', {method: 'POST', body: {
                    paymentServiceCode: f.paymentServiceCode.value,
                    fromShoppingCartId: cart.shoppingCartId,
                    currencyId: currency.id,
                    products: rowsCache.map(function (r) {
                        return {productId: r.product.id, amount: r.line.qty, price: Number(r.product.price)};
                    }),
                    deliveryInformation: {
                        deliveryFullName: f.deliveryFullName.value.trim(),
                        deliveryEmail: f.deliveryEmail.value.trim(),
                        deliveryPhoneNumber: f.deliveryPhoneNumber.value.replace(/[\s().-]/g, ''),
                        deliveryAddress: f.deliveryAddress.value.trim(),
                        deliveryCountryId: Number(f.deliveryCountryId.value),
                        deliveryCityName: f.deliveryCityName.value.trim(),
                        deliveryPostalCode: f.deliveryPostalCode.value.trim()
                    }
                }});
            }).then(function (url) {
                var target = typeof url === 'string' ? url : (url && (url.url || url.paymentUrl));
                if (!target || target.charAt(0) !== '/') { throw new Error('bad payment url'); }
                writeBag([]);
                location.href = target;
            }).catch(function (err) {
                btn.disabled = false;
                btn.textContent = 'Đặt hàng';
                if (err.status === 401) { goLogin('/checkout'); return; }
                var bad = err.status === 400 && err.data && typeof err.data === 'object' ? Object.keys(err.data)[0] : '';
                var labels = {deliveryPhoneNumber: 'số điện thoại', deliveryEmail: 'địa chỉ email', deliveryPostalCode: 'mã bưu chính', deliveryAddress: 'địa chỉ', deliveryFullName: 'họ tên', deliveryCityName: 'tỉnh/thành phố'};
                if (bad && labels[bad] && f[bad]) { showErr('Vui lòng kiểm tra lại ' + labels[bad] + '.', f[bad]); return; }
                showErr('Chưa đặt được đơn hàng. Vui lòng kiểm tra thông tin và thử lại.');
            });
        });
        boot();
    });

    function authError(data, status) {
        if (status === 401 || status === 400) {
            return 'Kiểm tra lại tên đăng nhập và mật khẩu rồi thử lại.';
        }
        return 'Đã có lỗi xảy ra. Vui lòng thử lại.';
    }

    pages.login = pages.register = function () {
        var form = q('#swAuthForm');
        var mode = form.getAttribute('data-mode');
        var errBox = q('#swFormErr');
        var btn = q('#swAuthBtn');
        var next = safeNext();
        var alt = q('[data-kn-next-link]');
        if (alt && next) { alt.href = alt.getAttribute('href') + '?next=' + encodeURIComponent(next); }
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var f = form.elements;
            errBox.hidden = true;
            qa('[aria-invalid]', form).forEach(function (n) { n.removeAttribute('aria-invalid'); });
            function bad(msg, field) { errBox.textContent = msg; errBox.hidden = false; if (field) { field.setAttribute('aria-invalid', 'true'); field.focus(); } }
            if (!f.username.value.trim()) { bad('Nhập tên đăng nhập.', f.username); return; }
            if (mode === 'register' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email.value.trim())) { bad('Nhập địa chỉ email hợp lệ.', f.email); return; }
            if (!f.password.value) { bad('Nhập mật khẩu.', f.password); return; }
            if (mode === 'register' && f.password.value.length < 8) { bad('Mật khẩu cần có ít nhất 8 ký tự.', f.password); return; }
            var body = {username: f.username.value.trim(), password: f.password.value};
            if (mode === 'register') {
                body.email = f.email.value.trim();
                body.displayName = f.displayName.value.trim() || body.username;
            }
            btn.disabled = true;
            api('/ezylogin/api/v1/' + mode, {method: 'POST', body: body}).then(function () {
                location.href = next || (mode === 'register' ? '/account' : '/account');
            }).catch(function (err) {
                btn.disabled = false;
                var msg = authError(err.data, err.status);
                if (mode === 'register' && err.data) {
                    msg = Object.keys(err.data).length ? 'Chưa tạo được tài khoản: ' + Object.keys(err.data).map(function (k) { return k + ' ' + err.data[k]; }).join(', ') + '.' : msg;
                }
                bad(msg, f.password);
            });
        });
    };

    pages.account = gated('/account', function () {
        var root = q('#swMain');
        var wrap = q('#swAccount');
        var ordersBox = q('[data-kn-orders-state]');
        var table = q('#swOrders');

        function profileRow(dl, label, value) {
            if (!value) { return; }
            dl.appendChild(el('dt', {text: label}));
            dl.appendChild(el('dd', {text: value}));
        }
        var account = null;
        var form = q('#swProfileForm');
        function isoDate(ms) {
            return ms > 0 ? new Date(ms).toISOString().slice(0, 10) : '';
        }
        function fillForm(a) {
            form.elements.displayName.value = a.displayName || '';
            form.elements.phone.value = a.phoneNumber || '';
            form.elements.birthDate.value = isoDate(a.dateOfBirth);
            form.elements.address.value = a.addressNumber || a.address || '';
        }
        function showProfile(a) {
            account = a;
            var dl = q('#swProfile');
            clear(dl);
            profileRow(dl, 'Họ tên', a.displayName);
            profileRow(dl, 'Tên đăng nhập', a.username);
            profileRow(dl, 'Email', a.email);
            profileRow(dl, 'Số điện thoại', a.phoneNumber);
            profileRow(dl, 'Ngày sinh', a.dateOfBirth > 0 ? dateText(a.dateOfBirth) : '');
            profileRow(dl, 'Địa chỉ', a.address || a.addressNumber);
            fillForm(a);
        }
        function clearErrors() {
            qa('[data-err-for]', form).forEach(function (n) { n.hidden = true; n.textContent = ''; });
            qa('input', form).forEach(function (n) { n.removeAttribute('aria-invalid'); });
            q('#swProfileErr').hidden = true;
            q('#swProfileOk').hidden = true;
        }
        function fieldError(name, msg) {
            var box = q('[data-err-for="' + name + '"]', form);
            var key = name === 'dateOfBirth' ? 'birthDate' : (name === 'addressNumber' ? 'address' : name);
            var input = form.elements[key];
            if (input) { input.setAttribute('aria-invalid', 'true'); }
            if (box) {
                box.textContent = msg === 'required' ? 'Vui lòng điền thông tin này.'
                    : (msg === 'duplicated' ? 'Giá trị này đã được tài khoản khác sử dụng.'
                        : (name === 'phone' ? 'Chỉ nhập chữ số, có thể bắt đầu bằng + (ví dụ +84912345678). Khoảng trắng và dấu gạch ngang sẽ được tự bỏ.'
                            : 'Vui lòng nhập giá trị hợp lệ.'));
                box.hidden = false;
                return true;
            }
            return false;
        }
        q('#swEditProfile').addEventListener('click', function () {
            var open = form.hidden;
            form.hidden = !open;
            this.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (open) { clearErrors(); fillForm(account || {}); form.elements.displayName.focus(); }
        });
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (!account) { return; }
            clearErrors();
            var save = q('#swProfileSave');
            var born = form.elements.birthDate.value;
            var body = {
                updateUser: true,
                updateCustomer: true,
                displayName: form.elements.displayName.value.trim(),
                email: account.email,
                phone: form.elements.phone.value.replace(/[\s\-.()]/g, ''),
                dateOfBirth: born ? Date.parse(born + 'T00:00:00Z') : 0,
                addressNumber: form.elements.address.value.trim(),
                addressCountryId: account.addressCountryId || 0,
                addressProvinceId: account.addressProvinceId || 0,
                addressDistrictId: account.addressDistrictId || 0,
                addressWardId: account.addressWardId || 0
            };
            save.disabled = true;
            api('/ezyaccount/api/v1/account/update', {method: 'PUT', body: body}).then(function () {
                return api('/ezyaccount/api/v1/account');
            }).then(function (a) {
                save.disabled = false;
                showProfile(a);
                q('#swProfileOk').hidden = false;
            }).catch(function (err) {
                save.disabled = false;
                if (err.status === 401) { goLogin('/account'); return; }
                var shown = false;
                if (err.status === 400 && err.data && typeof err.data === 'object') {
                    Object.keys(err.data).forEach(function (k) {
                        shown = fieldError(k, String(err.data[k])) || shown;
                    });
                }
                var box = q('#swProfileErr');
                box.textContent = shown
                    ? 'Chưa lưu được hồ sơ. Vui lòng kiểm tra các ô được đánh dấu.'
                    : 'Chưa lưu được hồ sơ. Vui lòng thử lại.';
                box.hidden = false;
            });
        });
        function load() {
            setState(root, 'loading', 'Đang tải tài khoản…');
            api('/ezyaccount/api/v1/account').then(function (a) {
                setState(root, null);
                showProfile(a);
                wrap.hidden = false;
                loadOrders();
            }).catch(function (e) {
                if (e.status === 401) { goLogin('/account'); return; }
                setState(root, 'error', 'Chưa tải được tài khoản.', load);
            });
        }
        function loadOrders() {
            clear(ordersBox);
            ordersBox.appendChild(el('div', {'class': 'kn-state', role: 'status'}, [el('span', {'class': 'kn-spin', 'aria-hidden': 'true'}), d.createTextNode('Đang tải đơn hàng…')]));
            api('/api/v1/orders?limit=20').then(function (r) {
                clear(ordersBox);
                var items = Array.isArray(r) ? r : (r.items || []);
                var body = q('tbody', table);
                clear(body);
                if (!items.length) {
                    ordersBox.appendChild(el('p', {'class': 'kn-note', text: 'Bạn chưa có đơn hàng nào.'}));
                    table.hidden = true;
                    return;
                }
                items.forEach(function (o) {
                    var when = o.createdAt ? dateText(o.createdAt) : '';
                    var total = orderMoney(o.formattedTotalAmountMoneyIncludeSymbol || o.formattedTotalAmountIncludeSymbol || o.formattedAmountIncludeSymbol || o.totalAmount || '');
                    body.appendChild(el('tr', {}, [
                        el('td', {}, [
                            el('a', {'class': 'kn-link', href: '/orders/' + o.id, text: '#' + (o.code || o.id)}),
                            d.createTextNode(' '),
                            el('a', {'class': 'kn-link', href: '/order-status?order=' + encodeURIComponent(o.id), text: 'Tra cứu đơn hàng'})
                        ]),
                        el('td', {text: when}),
                        el('td', {text: statusText(o.status || o.paymentStatus)}),
                        el('td', {text: String(total)})
                    ]));
                });
                table.hidden = false;
            }).catch(function () {
                clear(ordersBox);
                var retry = el('button', {type: 'button', 'class': 'kn-btn kn-btn--ghost kn-btn--inline', text: 'Thử lại'});
                retry.addEventListener('click', loadOrders);
                ordersBox.appendChild(el('div', {'class': 'kn-state', 'data-kind': 'error', role: 'alert'}, [d.createTextNode('Chưa tải được đơn hàng.'), el('div', {}, [retry])]));
            });
        }
        q('#swLogout').addEventListener('click', function () { signOut(); });
        load();
    });

    pages.stores = function () {
        var root = q('#swMain');
        var list = q('#swStores');
        function load() {
            setState(root, 'loading', 'Đang tải danh sách cửa hàng…');
            api('/data/stores.json').then(function (data) {
                var stores = (data && data.stores) || [];
                setState(root, null);
                clear(list);
                if (!stores.length) { setState(root, 'empty', 'Chưa có cửa hàng để hiển thị.'); return; }
                stores.forEach(function (s) {
                    list.appendChild(el('li', {}, [
                        el('h2', {text: s.name}),
                        el('p', {text: s.address}),
                        el('p', {'class': 'kn-muted', text: 'Giờ mở cửa: ' + s.hours}),
                        el('p', {}, [el('a', {
                            'class': 'kn-link',
                            href: '/book-an-appointment?store=' + encodeURIComponent(s.id),
                            text: 'Đặt lịch hẹn'
                        })])
                    ]));
                });
                q('#swStoresNote').textContent = data.note || '';
            }).catch(function () {
                setState(root, 'error', 'Chưa tải được danh sách cửa hàng.', load);
            });
        }
        load();
    };

    pages.orderstatus = function () {
        var form = q('#swStatusForm');
        var msg = q('#swStatusMsg');
        var result = q('#swStatusResult');
        var btn = q('#swStatusBtn');
        var summary = q('#swStatusSummary');
        var lines = q('#swStatusLines');
        var presetId = new URLSearchParams(location.search).get('order') || '';
        // only the order number may come from the link; the email is always typed by the customer
        if (/^[0-9]+$/.test(presetId)) {
            q('#swStatusId').value = presetId;
            q('#swStatusEmail').focus();
        }
        function resetResult() {
            result.hidden = true;
            summary.hidden = true;
            clear(lines);
            qa('dd', result).forEach(function (n) { n.textContent = ''; });
        }
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var id = q('#swStatusId').value.trim();
            var email = q('#swStatusEmail').value.trim();
            resetResult();
            if (!/^[0-9]+$/.test(id) || email.indexOf('@') < 1) {
                msg.textContent = 'Nhập mã đơn hàng và địa chỉ email hợp lệ.';
                return;
            }
            msg.textContent = 'Đang kiểm tra…';
            btn.disabled = true;
            api('/api/v1/ecm/order-status?' + qs({orderId: id, email: email})).then(function (r) {
                msg.textContent = '';
                q('#swStatusOrder').textContent = '#' + r.orderId;
                q('#swStatusValue').textContent = statusText(r.status);
                q('#swStatusPlaced').textContent = r.createdAt ? dateText(r.createdAt, true) : '-';
                q('#swStatusUpdated').textContent = r.updatedAt ? dateText(r.updatedAt, true) : '-';
                result.hidden = false;
                if (Array.isArray(r.items) && r.items.length) {
                    r.items.forEach(function (it) {
                        lines.appendChild(el('li', {}, [
                            el('span', {text: it.quantity + ' \u00d7 ' + (it.name || '') + ' (' + orderMoney(it.unitPrice) + ')'}),
                            el('span', {text: orderMoney(it.lineTotal)})
                        ]));
                    });
                    q('#swStatusTotal').textContent = orderMoney(r.total);
                    summary.hidden = false;
                }
            }).catch(function (err) {
                resetResult();
                msg.textContent = err.status === 429
                    ? 'Bạn thử quá nhiều lần. Vui lòng đợi một phút rồi thử lại.'
                    : err.status === 404 || err.status === 400
                        ? 'Không tìm thấy đơn hàng với thông tin này.'
                        : 'Chưa kiểm tra được đơn hàng. Vui lòng thử lại.';
            }).then(function () { btn.disabled = false; });
        });
    };

    pages.giftcard = function () {
        // no gift-card service exists in this demo: nothing is sent, every code gets the same answer
        var form = q('#swGiftForm');
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var err = q('#swGiftErr');
            var out = q('#swGiftResult');
            var code = form.elements.code;
            out.textContent = '';
            if (!code.value.trim()) {
                err.textContent = 'Nhập số thẻ.';
                err.hidden = false;
                code.setAttribute('aria-invalid', 'true');
                code.focus();
                return;
            }
            err.hidden = true;
            code.removeAttribute('aria-invalid');
            out.textContent = 'Tính năng tra cứu số dư thẻ quà tặng hiện chưa mở.';
        });
    };

    /* ---------- password reset, contact, appointment ---------- */
    var EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
    var PHONE_RE = /^\+?[0-9]{8,15}$/;

    function formMessages(form) {
        var err = q('#swFormErr', form);
        var ok = q('#swFormOk', form);
        return {
            reset: function () {
                err.hidden = true; ok.hidden = true;
                qa('[data-err-for]', form).forEach(function (n) { n.hidden = true; n.textContent = ''; });
                qa('[aria-invalid]', form).forEach(function (n) { n.removeAttribute('aria-invalid'); });
            },
            error: function (msg, field) {
                err.textContent = msg; err.hidden = false;
                if (field) { field.setAttribute('aria-invalid', 'true'); field.focus(); }
            },
            field: function (name, msg) {
                var box = q('[data-err-for="' + name + '"]', form);
                var input = form.elements[name];
                if (input) { input.setAttribute('aria-invalid', 'true'); }
                if (box) { box.textContent = msg; box.hidden = false; return true; }
                return false;
            },
            ok: function (msg) { ok.textContent = msg; ok.hidden = false; }
        };
    }

    // Shows the first invalid field's message in place and focuses it; returns true when all pass.
    function checkFields(form, msgs, rules) {
        var first = null;
        rules.forEach(function (r) {
            var input = form.elements[r.name];
            var problem = r.test(input.value.trim());
            if (problem) {
                msgs.field(r.name, problem);
                if (!first) { first = input; }
            }
        });
        if (first) { first.focus(); }
        return !first;
    }

    function showServerErrors(msgs, data, labels) {
        var shown = false;
        Object.keys(data || {}).forEach(function (k) {
            var what = labels[k] || 'Thông tin này';
            var code = data[k];
            var text = code === 'required' ? what + ' là thông tin bắt buộc.'
                : code === 'too-long' ? what + ' quá dài.'
                    : what + ' chưa hợp lệ.';
            if (msgs.field(k, text)) { shown = true; }
        });
        return shown;
    }

    function sendRequest(form, path, body, labels, done) {
        var msgs = formMessages(form);
        var btn = q('#swFormBtn', form);
        btn.disabled = true;
        api(path, {method: 'POST', body: body}).then(function () {
            done(msgs);
        }).catch(function (err) {
            if (err.status === 429) {
                msgs.error('Bạn gửi quá nhiều yêu cầu. Vui lòng đợi một phút rồi thử lại.');
            } else if (err.status === 400 && err.data && typeof err.data === 'object'
                && showServerErrors(msgs, err.data, labels)) {
                var bad = form.querySelector('[aria-invalid]');
                if (bad) { bad.focus(); }
            } else {
                msgs.error('Chưa gửi được yêu cầu. Vui lòng thử lại.');
            }
        }).then(function () { btn.disabled = false; });
    }

    pages.forgotpassword = function () {
        var form = q('#swForgotForm');
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var msgs = formMessages(form);
            msgs.reset();
            var email = form.elements.email.value.trim();
            if (!EMAIL_RE.test(email)) { msgs.error('Nhập địa chỉ email hợp lệ.', form.elements.email); return; }
            var btn = q('#swFormBtn', form);
            btn.disabled = true;
            // the answer is the same whether or not the address is registered
            api('/ezylogin/api/v1/forgot-password', {method: 'POST', body: {email: email}}).then(function () {
                msgs.ok('Nếu email này đã đăng ký, đường dẫn đặt lại mật khẩu sẽ được gửi đến hộp thư của bạn.');
            }).catch(function (err) {
                msgs.error(err.status === 429
                    ? 'Bạn gửi quá nhiều yêu cầu. Vui lòng đợi một phút rồi thử lại.'
                    : 'Chưa gửi được đường dẫn. Vui lòng thử lại.');
            }).then(function () { btn.disabled = false; });
        });
    };

    pages.resetpassword = function () {
        var form = q('#swResetForm');
        var params = new URLSearchParams(location.search);
        var uuid = params.get('uuid') || '';
        var token = params.get('token') || '';
        var msgs = formMessages(form);
        // keep the one-time token out of the address bar, history and Referer headers
        if (history.replaceState) { history.replaceState(null, '', location.pathname); }
        if (!uuid || !token) {
            form.hidden = true;
            var box = el('p', {'class': 'kn-error', role: 'alert',
                text: 'Đường dẫn đặt lại mật khẩu không hợp lệ. Vui lòng yêu cầu đường dẫn mới.'});
            form.parentNode.insertBefore(box, form);
            return;
        }
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            msgs.reset();
            var f = form.elements;
            if (f.password.value.length < 8) { msgs.error('Mật khẩu cần có ít nhất 8 ký tự.', f.password); return; }
            if (f.password.value !== f.confirm.value) { msgs.error('Hai mật khẩu chưa khớp nhau.', f.confirm); return; }
            var btn = q('#swFormBtn', form);
            btn.disabled = true;
            api('/ezylogin/api/v1/reset-password', {
                method: 'POST',
                body: {userUuid: uuid, forgotPasswordToken: token, newPassword: f.password.value}
            }).then(function () {
                form.reset();
                msgs.ok('Mật khẩu đã được đổi. Bạn có thể đăng nhập ngay.');
                btn.hidden = true;
                form.appendChild(el('a', {'class': 'kn-btn kn-btn--wide', href: '/login', text: 'Đăng nhập'}));
            }).catch(function (err) {
                btn.disabled = false;
                msgs.error(err.status === 429
                    ? 'Bạn gửi quá nhiều yêu cầu. Vui lòng đợi một phút rồi thử lại.'
                    : err.status === 400 && err.data && typeof err.data === 'object' && err.data.newPassword
                        ? 'Mật khẩu này chưa được chấp nhận. Hãy chọn mật khẩu dài hơn và đa dạng ký tự hơn.'
                        : 'Đường dẫn đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu đường dẫn mới.');
            });
        });
    };

    var CONTACT_LABELS = {name: 'Họ tên', email: 'Email', phone: 'Số điện thoại',
        topic: 'Chủ đề', message: 'Nội dung tin nhắn'};

    pages.wishlist = gated('/wishlist', function () {
        var root = q('#swMain');
        var grid = q('#swGrid');
        var more = q('#swLoadMore');
        var count = q('#swCount');
        var page = 1;
        var shown = 0;
        var total = 0;
        function summary() {
            count.textContent = total + ' sản phẩm';
            more.hidden = shown >= total;
            if (total === 0) {
                setState(root, 'empty', 'Danh sách yêu thích đang trống. Chạm vào biểu tượng trái tim trên sản phẩm để lưu vào đây.');
                count.textContent = '';
            }
        }
        function fetchPage() {
            setState(root, 'loading', 'Đang tải danh sách yêu thích…');
            more.disabled = true;
            api('/api/v1/ecm/wishlist?' + qs({page: page, size: 24})).then(function (r) {
                setState(root, null);
                (r.items || []).forEach(function (it) { grid.appendChild(productCard(it)); });
                shown += (r.productIds || []).length;
                total = r.total || 0;
                summary();
            }).catch(function (e) {
                if (e.status === 401) { goLogin('/wishlist'); return; }
                setState(root, 'error', 'Chưa tải được danh sách yêu thích.', fetchPage);
            }).then(function () { more.disabled = false; });
        }
        more.addEventListener('click', function () { page += 1; fetchPage(); });
        d.addEventListener('ecm:wish', function (e) {
            if (e.detail.on) { return; }
            qa('[data-pid-card="' + e.detail.id + '"]', grid).forEach(function (li) { grid.removeChild(li); });
            shown = Math.max(0, shown - 1);
            total = Math.max(0, total - 1);
            summary();
        });
        fetchPage();
    });

    pages.newsletter = function () {
        var unsub = /\/unsubscribe\/?$/.test(location.pathname);
        var token = new URLSearchParams(location.search).get('token') || '';
        var btn = q('#swNlBtn');
        var err = q('#swNlErr');
        var ok = q('#swNlOk');
        // keep the one-time token out of the address bar, history and Referer headers
        if (history.replaceState) { history.replaceState(null, '', location.pathname); }
        q('#swNlTitle').textContent = unsub ? 'Hủy đăng ký bản tin' : 'Xác nhận đăng ký';
        if (!/^[0-9a-f]{64}$/.test(token)) {
            err.textContent = 'Đường dẫn không hợp lệ. Vui lòng dùng đường dẫn trong email của bạn.';
            err.hidden = false;
            return;
        }
        q('#swNlText').textContent = unsub
            ? 'Nhấn nút bên dưới để ngừng nhận bản tin của chúng tôi.'
            : 'Nhấn nút bên dưới để xác nhận bạn muốn nhận bản tin của chúng tôi.';
        btn.textContent = unsub ? 'Hủy đăng ký' : 'Xác nhận đăng ký';
        btn.hidden = false;
        btn.addEventListener('click', function () {
            err.hidden = true;
            btn.disabled = true;
            api('/api/v1/ecm/newsletter/' + (unsub ? 'unsubscribe' : 'confirm'), {method: 'POST', body: {token: token}})
                .then(function () {
                    btn.hidden = true;
                    q('#swNlText').textContent = '';
                    ok.textContent = unsub
                        ? 'Bạn đã hủy đăng ký. Bạn sẽ không nhận bản tin của chúng tôi nữa.'
                        : 'Cảm ơn bạn. Đăng ký của bạn đã được xác nhận.';
                    ok.hidden = false;
                }).catch(function (e) {
                    err.textContent = e.status === 404
                        ? 'Đường dẫn này không còn hiệu lực. Vui lòng dùng email mới nhất chúng tôi đã gửi.'
                        : e.status === 429
                            ? 'Bạn gửi quá nhiều yêu cầu. Vui lòng đợi một phút rồi thử lại.'
                            : 'Đã có lỗi xảy ra. Vui lòng thử lại.';
                    err.hidden = false;
                    btn.disabled = false;
                });
        });
    };

    pages.contact = function () {
        var form = q('#swContactForm');
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var msgs = formMessages(form);
            msgs.reset();
            var ok = checkFields(form, msgs, [
                {name: 'name', test: function (v) { return v ? '' : 'Nhập họ tên.'; }},
                {name: 'email', test: function (v) { return EMAIL_RE.test(v) ? '' : 'Nhập địa chỉ email hợp lệ.'; }},
                {name: 'phone', test: function (v) {
                    var p = v.replace(/[\s-]/g, '');
                    return !p || PHONE_RE.test(p) ? '' : 'Chỉ nhập chữ số, có thể bắt đầu bằng +.';
                }},
                {name: 'message', test: function (v) { return v ? '' : 'Nhập nội dung tin nhắn.'; }}
            ]);
            if (!ok) { return; }
            var f = form.elements;
            sendRequest(form, '/api/v1/ecm/contact', {
                name: f.name.value.trim(), email: f.email.value.trim(), phone: f.phone.value.trim(),
                topic: f.topic.value, message: f.message.value.trim(), website: f.website.value
            }, CONTACT_LABELS, function (done) {
                form.reset();
                done.ok('Cảm ơn bạn. Chúng tôi đã nhận được tin nhắn và sẽ trả lời qua email.');
            });
        });
    };

    var APPT_LABELS = {storeId: 'Cửa hàng', date: 'Ngày hẹn', time: 'Giờ hẹn', name: 'Họ tên',
        phone: 'Số điện thoại', email: 'Email', note: 'Ghi chú'};

    pages.appointment = function () {
        var root = q('#swMain');
        var form = q('#swApptForm');
        var dateInput = form.elements.date;
        var timeSel = form.elements.time;
        var storeSel = form.elements.storeId;
        function iso(day) {
            return day.getFullYear() + '-' + ('0' + (day.getMonth() + 1)).slice(-2) + '-' + ('0' + day.getDate()).slice(-2);
        }
        var today = new Date();
        var last = new Date(today.getTime() + 90 * 24 * 3600 * 1000);
        dateInput.min = iso(today);
        dateInput.max = iso(last);
        timeSel.appendChild(el('option', {value: '', text: 'Chọn giờ hẹn'}));
        for (var h = 10; h <= 21; h++) {
            ['00', '30'].forEach(function (m) {
                timeSel.appendChild(el('option', {value: h + ':' + m, text: h + ':' + m}));
            });
        }
        function load() {
            setState(root, 'loading', 'Đang tải danh sách cửa hàng…');
            api('/data/stores.json').then(function (data) {
                var stores = (data && data.stores) || [];
                if (!stores.length) { setState(root, 'empty', 'Chưa có cửa hàng nhận lịch hẹn.'); return; }
                setState(root, null);
                storeSel.appendChild(el('option', {value: '', text: 'Chọn cửa hàng'}));
                stores.forEach(function (s) { storeSel.appendChild(el('option', {value: s.id, text: s.name})); });
                var want = new URLSearchParams(location.search).get('store') || '';
                storeSel.value = want;
                if (storeSel.value !== want) { storeSel.value = ''; }
                form.hidden = false;
            }).catch(function () {
                setState(root, 'error', 'Chưa tải được danh sách cửa hàng.', load);
            });
        }
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var msgs = formMessages(form);
            msgs.reset();
            var ok = checkFields(form, msgs, [
                {name: 'storeId', test: function (v) { return v ? '' : 'Chọn cửa hàng.'; }},
                {name: 'date', test: function (v) {
                    return v && v >= dateInput.min && v <= dateInput.max ? '' : 'Chọn ngày trong vòng 90 ngày tới.';
                }},
                {name: 'time', test: function (v) { return v ? '' : 'Chọn giờ hẹn.'; }},
                {name: 'name', test: function (v) { return v ? '' : 'Nhập họ tên.'; }},
                {name: 'phone', test: function (v) {
                    return PHONE_RE.test(v.replace(/[\s-]/g, '')) ? '' : 'Chỉ nhập chữ số, có thể bắt đầu bằng +.';
                }},
                {name: 'email', test: function (v) { return EMAIL_RE.test(v) ? '' : 'Nhập địa chỉ email hợp lệ.'; }}
            ]);
            if (!ok) { return; }
            var f = form.elements;
            sendRequest(form, '/api/v1/ecm/appointments', {
                storeId: f.storeId.value, date: f.date.value, time: f.time.value,
                name: f.name.value.trim(), phone: f.phone.value.trim(), email: f.email.value.trim(),
                note: f.note.value.trim(), website: f.website.value
            }, APPT_LABELS, function (done) {
                form.reset();
                done.ok('Cảm ơn bạn. Yêu cầu đã được gửi đến cửa hàng, cửa hàng sẽ xác nhận lịch hẹn qua email hoặc điện thoại.');
            });
        });
        load();
    };

    function signOut() {
        // the platform logout deletes the access token on the server and clears the cookies
        var done = function () { location.href = '/'; };
        fetch('/logout', {method: 'POST', credentials: 'same-origin'}).then(done, done);
    }

    pages.order = gated(function () { return location.pathname; }, function () {
        var root = q('#swMain');
        var id = PARAM;
        function row(dl, label, value) {
            if (!value) { return; }
            dl.appendChild(el('dt', {text: label}));
            dl.appendChild(el('dd', {text: value}));
        }
        function load() {
            setState(root, 'loading', 'Đang tải đơn hàng…');
            api('/api/v1/orders/' + encodeURIComponent(id)).then(function (o) {
                setState(root, null);
                var paid = o.paymentStatus === 'PAID';
                q('#swOrderTitle').textContent = paid ? 'Cảm ơn bạn đã đặt hàng' : 'Đơn hàng của bạn';
                var st = q('#swOrderStatus');
                st.textContent = paid ? 'Đã nhận thanh toán. Đơn hàng #' + o.id + ' đã được xác nhận.' : 'Đơn hàng #' + o.id + ' ' + statusText(o.status) + '.';
                var meta = q('#swOrderMeta');
                clear(meta);
                row(meta, 'Mã đơn hàng', '#' + o.id);
                row(meta, 'Trạng thái', statusText(o.status));
                row(meta, 'Thanh toán', statusText(o.paymentStatus));
                row(meta, 'Ngày đặt', o.createdAt ? dateText(o.createdAt, true) : '');
                var lines = q('#swOrderLines');
                clear(lines);
                (o.products || []).forEach(function (l) {
                    lines.appendChild(el('li', {}, [
                        el('span', {text: l.amount + ' \u00d7 ' + (l.product && l.product.productName || '')}),
                        el('span', {text: orderMoney(l.formattedTotalAmountMoneyIncludeSymbol)})
                    ]));
                });
                q('#swOrderTotal').textContent = orderMoney(o.formattedTotalAmountMoneyIncludeSymbol);
                var track = q('#swOrderTrack');
                if (track) { track.setAttribute('href', '/order-status?order=' + encodeURIComponent(o.id)); }
                q('#swOrder').hidden = false;
                d.title = 'Đơn hàng #' + o.id + ' | Khang Nguyên · Ngọc Bảo';
            }).catch(function (e) {
                if (e.status === 401) { goLogin(location.pathname); return; }
                if (e.status === 403 || e.status === 404) { setState(root, 'empty', 'Không tìm thấy đơn hàng này.'); return; }
                setState(root, 'error', 'Chưa tải được đơn hàng.', load);
            });
        }
        load();
    });

    pages.service = function () { /* static content */ };

    /* ---------- chrome ---------- */
    function initChrome() {
        b.dataset.knChrome = '1';
        var drawer = q('#swDrawer');
        var menuBtn = q('[data-kn-menu]');
        function openDrawer(open) {
            d.body.classList.toggle('kn-drawer-open', open);
            drawer.setAttribute('aria-hidden', open ? 'false' : 'true');
            menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (open) { q('.kn-drawer__x', drawer).focus(); } else { menuBtn.focus(); }
        }
        if (drawer && menuBtn) {
            menuBtn.addEventListener('click', function () { openDrawer(true); });
            qa('[data-kn-close]').forEach(function (n) { n.addEventListener('click', function () { openDrawer(false); }); });
            d.addEventListener('keydown', function (e) {
                if (e.key === 'Escape' && d.body.classList.contains('kn-drawer-open')) { openDrawer(false); }
            });
        }
        var searchForm = q('#swSearch');
        var searchBtn = q('[data-kn-search-toggle]');
        if (searchForm && searchBtn) {
            searchBtn.addEventListener('click', function () {
                var show = searchForm.hidden;
                searchForm.hidden = !show;
                searchBtn.setAttribute('aria-expanded', show ? 'true' : 'false');
                if (show) { q('#swSearchInput').focus(); }
            });
        }
        qa('[data-kn-auth-link]').forEach(function (a) {
            loggedIn().then(function (ok) {
                if (ok) { a.textContent = 'Tài khoản của tôi'; a.setAttribute('href', '/account'); }
            });
        });
        renderBagCount();
        loadCategories().then(renderCategoryLists);
    }

    function initNewsletter() {
        var form = q('#swNewsForm');
        if (!form) { return; }
        var err = q('#swNewsErr');
        var ok = q('#swNewsOk');
        var btn = q('#swNewsBtn');
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            err.hidden = true;
            ok.hidden = true;
            var input = form.elements.email;
            input.removeAttribute('aria-invalid');
            var email = input.value.trim();
            if (!EMAIL_RE.test(email)) {
                err.textContent = 'Nhập địa chỉ email hợp lệ.';
                err.hidden = false;
                input.setAttribute('aria-invalid', 'true');
                input.focus();
                return;
            }
            btn.disabled = true;
            // the answer is the same whether or not the address is already subscribed
            api('/api/v1/ecm/newsletter', {method: 'POST', body: {email: email, website: form.elements.website.value}})
                .then(function () {
                    form.reset();
                    ok.textContent = 'Cảm ơn bạn đã đăng ký. Vui lòng kiểm tra hộp thư để xác nhận địa chỉ email.';
                    ok.hidden = false;
                }).catch(function (e2) {
                    err.textContent = e2.status === 429
                        ? 'Bạn gửi quá nhiều yêu cầu. Vui lòng đợi một phút rồi thử lại.'
                        : e2.status === 400
                            ? 'Nhập địa chỉ email hợp lệ.'
                            : 'Chưa đăng ký được. Vui lòng thử lại.';
                    err.hidden = false;
                }).then(function () { btn.disabled = false; });
        });
    }

    function init() {
        initChrome();
        initNewsletter();
        var page = b.dataset.page;
        if (pages[page]) { pages[page](); }
    }
    if (d.readyState === 'loading') { d.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
