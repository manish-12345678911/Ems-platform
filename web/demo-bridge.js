/**
 * H8 EMS Demo Bridge — Local mock API layer
 * Intercepts fetch() calls and provides working mock responses.
 * Uses BroadcastChannel + localStorage for cross-tab sync.
 * No backend required.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'h8_demo_state';
    var CHANNEL_NAME = 'h8_demo_sync';

    // BroadcastChannel for cross-tab sync
    var channel;
    try { channel = new BroadcastChannel(CHANNEL_NAME); } catch (e) { channel = { postMessage: function () { }, close: function () { }, addEventListener: function () { }, removeEventListener: function () { } }; }

    function makeId() {
        // Fallback UUID for insecure contexts where crypto.randomUUID is unavailable
        try { return crypto.randomUUID(); } catch (e) {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
                var r = Math.random() * 16 | 0;
                return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            });
        }
    }

    function createDefaultState() {
        return {
            fleet: [
                { id: 'aaaaaaaa-1111-1111-1111-111111111111', unitId: 'aaaaaaaa-1111-1111-1111-111111111111', callSign: 'AMB-01', lat: 26.9150, lon: 75.8100, type: 'ALS', status: 'AVAILABLE' },
                { id: 'bbbbbbbb-2222-2222-2222-222222222222', unitId: 'bbbbbbbb-2222-2222-2222-222222222222', callSign: 'AMB-02', lat: 26.9239, lon: 75.8267, type: 'BLS', status: 'AVAILABLE' },
                { id: 'cccccccc-3333-3333-3333-333333333333', unitId: 'cccccccc-3333-3333-3333-333333333333', callSign: 'AMB-03', lat: 26.8988, lon: 75.8164, type: 'ALS', status: 'AVAILABLE' },
                { id: 'dddddddd-4444-4444-4444-444444444444', unitId: 'dddddddd-4444-4444-4444-444444444444', callSign: 'AMB-04', lat: 26.9073, lon: 75.7925, type: 'BLS', status: 'AVAILABLE' },
                { id: '55555555-0005-0005-0005-000000000005', unitId: '55555555-0005-0005-0005-000000000005', callSign: 'AMB-05', lat: 26.8524, lon: 75.8054, type: 'ALS', status: 'AVAILABLE' },
                { id: '66666666-0006-0006-0006-000000000006', unitId: '66666666-0006-0006-0006-000000000006', callSign: 'AMB-06', lat: 26.8512, lon: 75.7892, type: 'BLS', status: 'AVAILABLE' },
                { id: '77777777-0007-0007-0007-000000000007', unitId: '77777777-0007-0007-0007-000000000007', callSign: 'AMB-07', lat: 26.8623, lon: 75.7584, type: 'ALS', status: 'AVAILABLE' },
                { id: '88888888-0008-0008-0008-000000000008', unitId: '88888888-0008-0008-0008-000000000008', callSign: 'AMB-08', lat: 26.9077, lon: 75.7397, type: 'BLS', status: 'AVAILABLE' },
                { id: '99999999-0009-0009-0009-000000000009', unitId: '99999999-0009-0009-0009-000000000009', callSign: 'AMB-09', lat: 26.8973, lon: 75.8260, type: 'ALS', status: 'AVAILABLE' },
                { id: 'aaaaaaaa-0010-0010-0010-000000000010', unitId: 'aaaaaaaa-0010-0010-0010-000000000010', callSign: 'AMB-10', lat: 26.9452, lon: 75.7337, type: 'BLS', status: 'AVAILABLE' },
                { id: 'bbbbbbbb-0011-0011-0011-000000000011', unitId: 'bbbbbbbb-0011-0011-0011-000000000011', callSign: 'AMB-11', lat: 26.9734, lon: 75.7766, type: 'ALS', status: 'AVAILABLE' },
                { id: 'cccccccc-0012-0012-0012-000000000012', unitId: 'cccccccc-0012-0012-0012-000000000012', callSign: 'AMB-12', lat: 26.9050, lon: 75.7780, type: 'BLS', status: 'AVAILABLE' },
                { id: 'dddddddd-0013-0013-0013-000000000013', unitId: 'dddddddd-0013-0013-0013-000000000013', callSign: 'AMB-13', lat: 26.8285, lon: 75.8522, type: 'ALS', status: 'AVAILABLE' },
                { id: 'eeeeeeee-0014-0014-0014-000000000014', unitId: 'eeeeeeee-0014-0014-0014-000000000014', callSign: 'AMB-14', lat: 26.7788, lon: 75.8277, type: 'ALS', status: 'AVAILABLE' }
            ],
            hospitals: [
                { id: 'aaaaaaaa-0001-0001-0001-000000000001', name: 'SMS Hospital & Apex Trauma Center', lat: 26.8988, lon: 75.8164, capabilities: ['Trauma', 'Cardiac', 'PCI', 'Neuro'], edBedsFree: 8, icuBedsFree: 3, ventilatorsFree: 4, diversion: false },
                { id: 'bbbbbbbb-0002-0002-0002-000000000002', name: 'Fortis Escorts Hospital', lat: 26.8524, lon: 75.8054, capabilities: ['Cardiac', 'PCI', 'Trauma'], edBedsFree: 5, icuBedsFree: 2, ventilatorsFree: 2, diversion: false },
                { id: 'cccccccc-0003-0003-0003-000000000003', name: 'Eternal Heart Care Centre (EHCC)', lat: 26.8623, lon: 75.7584, capabilities: ['Cardiac', 'PCI'], edBedsFree: 4, icuBedsFree: 2, ventilatorsFree: 1, diversion: false },
                { id: 'dddddddd-0004-0004-0004-000000000004', name: 'Narayana Multispeciality Hospital', lat: 26.7865, lon: 75.8245, capabilities: ['Trauma', 'Cardiac', 'Ortho'], edBedsFree: 6, icuBedsFree: 3, ventilatorsFree: 3, diversion: false },
                { id: 'eeeeeeee-0005-0005-0005-000000000005', name: 'Manipal Hospital', lat: 26.9734, lon: 75.7766, capabilities: ['Trauma', 'General'], edBedsFree: 4, icuBedsFree: 1, ventilatorsFree: 2, diversion: false }
            ],
            lastIncidentId: null
        };
    }

    function getState() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return createDefaultState();
            var s = JSON.parse(raw);
            if (!s || !s.fleet || !s.hospitals) return createDefaultState();
            return s;
        } catch (e) { return createDefaultState(); }
    }

    function setState(state) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
    }

    // Haversine distance in km
    function haversine(lat1, lon1, lat2, lon2) {
        var R = 6371;
        var dLat = (lat2 - lat1) * Math.PI / 180;
        var dLon = (lon2 - lon1) * Math.PI / 180;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Capability-Aware DispatchScorer (mirrors common/DispatchScorer.java)
    function scoreCandidate(unit, incLat, incLon, severity, need, requiresAls) {
        var dist = haversine(unit.lat, unit.lon, incLat, incLon);
        var etaSeconds = (dist / 40) * 3600; // ~40 km/h city
        var capPenalty = 0;
        if (requiresAls && unit.type === 'BLS') capPenalty = 0.55;
        if (need === 'CARDIAC' && unit.type === 'BLS') capPenalty = 0.65;
        if (need === 'TRAUMA' && unit.type === 'BLS') capPenalty = 0.40;
        var proxScore = Math.max(0, 1 - (etaSeconds / 1800));
        var score = (0.35 * proxScore) + (0.45 * (1 - capPenalty)) + (0.20 * 1.0);

        return {
            unitId: unit.id || unit.unitId,
            callSign: unit.callSign,
            type: unit.type,
            lat: unit.lat,
            lon: unit.lon,
            distanceKm: dist,
            etaSeconds: etaSeconds,
            score: score,
            capabilityComponent: capPenalty > 0 ? 1 : 0,
            status: unit.status
        };
    }

    function jsonResponse(data, status) {
        status = status || 200;
        return new Response(JSON.stringify(data), {
            status: status,
            statusText: status === 200 ? 'OK' : 'Error',
            headers: { 'Content-Type': 'application/json' }
        });
    }

    // API Routes — returns a Response, or null if not matched
    function handleRequest(method, pathname, searchParams, body) {
        var state, unit, hosp, unitId, hospId, lat, lon;

        // GET /dispatch/units
        if (method === 'GET' && pathname === '/dispatch/units') {
            state = getState();
            return jsonResponse(state.fleet);
        }

        // POST /dispatch/units/{id}/location
        if (method === 'POST' && pathname.indexOf('/dispatch/units/') === 0 && pathname.indexOf('/location') > 0) {
            unitId = pathname.split('/')[3];
            lat = parseFloat(searchParams.get('lat'));
            lon = parseFloat(searchParams.get('lon'));
            var status = searchParams.get('status');
            state = getState();
            unit = state.fleet.find(function (u) { return u.id === unitId || u.unitId === unitId; });
            if (unit) {
                if (!isNaN(lat)) unit.lat = lat;
                if (!isNaN(lon)) unit.lon = lon;
                if (status) {
                    unit.status = status;
                    if (status.toUpperCase() === 'AVAILABLE') {
                        delete unit.assignedIncident;
                    }
                }
                setState(state);
                try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
            }
            return jsonResponse({ ok: true });
        }

        // POST /dispatch/units/{id}/status
        if (method === 'POST' && pathname.indexOf('/dispatch/units/') === 0 && pathname.indexOf('/status') > 0) {
            unitId = pathname.split('/')[3];
            var newStatus = searchParams.get('status');
            state = getState();
            unit = state.fleet.find(function (u) { return u.id === unitId || u.unitId === unitId; });
            if (unit && newStatus) {
                unit.status = newStatus;
                if (newStatus.toUpperCase() === 'AVAILABLE') {
                    delete unit.assignedIncident;
                }
                setState(state);
                try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
            }
            return jsonResponse({ ok: true });
        }

        // GET /dispatch/candidates
        if (method === 'GET' && pathname === '/dispatch/candidates') {
            lat = parseFloat(searchParams.get('lat'));
            lon = parseFloat(searchParams.get('lon'));
            var severity = searchParams.get('severity') || 'URGENT';
            var need = searchParams.get('need') || 'GENERAL';
            var requiresAls = searchParams.get('requiresAls') === 'true';
            state = getState();
            var available = state.fleet.filter(function (u) { return u.status === 'AVAILABLE'; });
            var scored = available.map(function (u) { return scoreCandidate(u, lat, lon, severity, need, requiresAls); });
            scored.sort(function (a, b) { return b.score - a.score; });
            return jsonResponse(scored);
        }

        // POST /incidents
        if (method === 'POST' && pathname === '/incidents') {
            var incidentId = makeId();
            state = getState();
            state.lastIncidentId = incidentId;
            setState(state);
            return jsonResponse({ incidentId: incidentId, status: 'CREATED' });
        }

        // POST /dispatch (assign unit to incident - Hard Rule #3: conditional update status = AVAILABLE)
        if (method === 'POST' && pathname === '/dispatch') {
            state = getState();
            if (body && body.unitId) {
                unit = state.fleet.find(function (u) { return u.id === body.unitId || u.unitId === body.unitId; });
                if (!unit) {
                    return new Response(JSON.stringify({ error: 'UNIT_NOT_FOUND', message: 'Ambulance not found in fleet roster.' }), { status: 404, headers: { 'Content-Type': 'application/json' } });
                }

                // Prevent two incidents from claiming the same ambulance simultaneously
                var currentSt = (unit.status || '').toUpperCase();
                if (currentSt !== 'AVAILABLE') {
                    return new Response(JSON.stringify({
                        error: 'UNIT_ALREADY_COMMITTED',
                        message: 'Ambulance ' + unit.callSign + ' is already active (' + currentSt + ') and cannot be dispatched to a second location simultaneously.'
                    }), { status: 409, headers: { 'Content-Type': 'application/json' } });
                }

                // Atomic conditional reservation
                unit.status = 'DISPATCHED';
                var assignedInc = {
                    incidentId: body.incidentId || state.lastIncidentId || ('INC-' + makeId().substring(0, 8).toUpperCase()),
                    lat: (body.incidentLat != null && !isNaN(body.incidentLat)) ? parseFloat(body.incidentLat) : 26.9124,
                    lon: (body.incidentLon != null && !isNaN(body.incidentLon)) ? parseFloat(body.incidentLon) : 75.7873,
                    severity: body.severity || 'CRITICAL',
                    clinicalNeed: body.clinicalNeed || body.need || 'TRAUMA',
                    requiresAls: body.requiresAls === true,
                    targetAddress: body.targetAddress || 'Ashok Nagar, C-Scheme, Jaipur',
                    dispatchedAt: new Date().toISOString()
                };
                unit.assignedIncident = assignedInc;
                setState(state);

                // Broadcast dispatch event specifically identifying which ambulance was claimed
                try {
                    channel.postMessage({
                        type: 'unit_dispatched',
                        unitId: unit.unitId || unit.id,
                        callSign: unit.callSign,
                        incident: assignedInc
                    });
                    channel.postMessage({ type: 'state_changed' });
                    channel.postMessage({
                        type: 'pre_arrival_alert',
                        alert: {
                            alertId: makeId(),
                            incidentId: assignedInc.incidentId,
                            severity: assignedInc.severity,
                            need: assignedInc.clinicalNeed,
                            etaSeconds: 480,
                            sentAt: new Date().toISOString(),
                            unitCallSign: unit.callSign
                        }
                    });
                } catch (e) { }

                return jsonResponse({
                    status: 'DISPATCHED',
                    unitId: unit.unitId || unit.id,
                    callSign: unit.callSign,
                    assignedAt: new Date().toISOString(),
                    assignedIncident: assignedInc
                });
            }
            return new Response(JSON.stringify({ error: 'MISSING_UNIT_ID', message: 'unitId is required' }), { status: 400, headers: { 'Content-Type': 'application/json' } });
        }

        // GET /redeployment/coverage
        if (method === 'GET' && pathname === '/redeployment/coverage') {
            state = getState();
            var avail = state.fleet.filter(function (u) { return u.status === 'AVAILABLE'; }).length;
            var total = state.fleet.length;
            var pct = ((avail / total) * 100).toFixed(1) + '%';
            return jsonResponse({ coveragePercent: pct, availableUnits: avail, totalUnits: total });
        }

        // POST /redeployment/plan
        if (method === 'POST' && pathname === '/redeployment/plan') {
            return jsonResponse({ message: 'MEXCLP greedy solver: coverage optimal. No repositioning needed.', moves: [] });
        }

        // GET /hospitals
        if (method === 'GET' && pathname === '/hospitals') {
            state = getState();
            return jsonResponse(state.hospitals);
        }

        // GET /hospitals/{id}/capacity
        if (method === 'GET' && /^\/hospitals\/[^\/]+\/capacity$/.test(pathname)) {
            hospId = pathname.split('/')[2];
            state = getState();
            hosp = state.hospitals.find(function (h) { return h.id === hospId; });
            if (hosp) {
                return jsonResponse({ edBedsFree: hosp.edBedsFree, icuBedsFree: hosp.icuBedsFree, ventilatorsFree: hosp.ventilatorsFree, updatedAt: new Date().toISOString(), isStale: false });
            }
            return jsonResponse({ error: 'Not found' }, 404);
        }

        // PUT /hospitals/{id}/capacity
        if (method === 'PUT' && /^\/hospitals\/[^\/]+\/capacity$/.test(pathname)) {
            hospId = pathname.split('/')[2];
            state = getState();
            hosp = state.hospitals.find(function (h) { return h.id === hospId; });
            if (hosp && body) {
                if (body.edBedsFree != null) hosp.edBedsFree = body.edBedsFree;
                if (body.icuBedsFree != null) hosp.icuBedsFree = body.icuBedsFree;
                if (body.ventilatorsFree != null) hosp.ventilatorsFree = body.ventilatorsFree;
                setState(state);
            }
            return jsonResponse({ ok: true, updatedAt: new Date().toISOString() });
        }

        // POST /hospitals/{id}/handover
        if (method === 'POST' && /^\/hospitals\/[^\/]+\/handover$/.test(pathname)) {
            if (body && body.unitId) {
                state = getState();
                unit = state.fleet.find(function (u) { return u.id === body.unitId || u.unitId === body.unitId; });
                if (unit) {
                    unit.status = 'AVAILABLE';
                    delete unit.assignedIncident;
                    setState(state);
                    try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
                }
            }
            return jsonResponse({ ok: true, handoverAt: new Date().toISOString() });
        }

        // GET /audit/verify
        if (method === 'GET' && pathname === '/audit/verify') {
            state = getState();
            return jsonResponse({ valid: true, totalEntries: state.fleet.length + 42, details: 'All block signatures mathematically verified. Chain integrity: PASS.', verifiedAt: new Date().toISOString() });
        }

        // GET /actuator/health
        if (method === 'GET' && pathname === '/actuator/health') {
            return jsonResponse({ status: 'UP' });
        }

        // GET /audit (with limit)
        if (method === 'GET' && pathname === '/audit') {
            return jsonResponse([]);
        }

        return null; // Not matched
    }

    // Intercept window.fetch
    var originalFetch = window.fetch.bind(window);

    window.fetch = function (input, init) {
        init = init || {};
        var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
        var method = (init.method || 'GET').toUpperCase();

        var fullUrl;
        try { fullUrl = new URL(url, window.location.origin); } catch (e) { return originalFetch(input, init); }

        // Skip external URLs
        if (fullUrl.origin !== window.location.origin) return originalFetch(input, init);

        // Skip file requests
        if (/\.(html|css|js|png|jpg|gif|svg|ico|woff|woff2|json|map|pbf)$/.test(fullUrl.pathname)) {
            return originalFetch(input, init);
        }

        // Skip root and directory index requests
        if (fullUrl.pathname === '/' || fullUrl.pathname.endsWith('/')) {
            return originalFetch(input, init);
        }

        // Parse body if present
        var body = {};
        if (init.body) {
            try { body = JSON.parse(init.body); } catch (e) { body = {}; }
        }

        var response = handleRequest(method, fullUrl.pathname, fullUrl.searchParams, body);

        if (response) {
            return Promise.resolve(response);
        }

        // Not intercepted — pass through, but catch network errors gracefully
        return originalFetch(input, init).catch(function () {
            return jsonResponse({ error: 'offline' }, 503);
        });
    };

    // Intercept EventSource for SSE alerts
    var OrigES = window.EventSource;

    function MockEventSource(url) {
        var self = this;
        self.url = url;
        self.readyState = 1;
        self.onopen = null;
        self.onmessage = null;
        self.onerror = null;
        self._listeners = {};

        setTimeout(function () { if (self.onopen) self.onopen(new Event('open')); }, 100);

        self._handler = function (event) {
            if (event.data && event.data.type === 'pre_arrival_alert') {
                var alertData = event.data.alert;
                var msgEvent = new MessageEvent('message', { data: JSON.stringify(alertData) });
                if (self.onmessage) self.onmessage(msgEvent);
                var listeners = self._listeners['pre-arrival'] || [];
                for (var i = 0; i < listeners.length; i++) {
                    listeners[i](new MessageEvent('pre-arrival', { data: JSON.stringify(alertData) }));
                }
            }
        };
        channel.addEventListener('message', self._handler);
    }

    MockEventSource.prototype.addEventListener = function (type, fn) {
        if (!this._listeners[type]) this._listeners[type] = [];
        this._listeners[type].push(fn);
    };
    MockEventSource.prototype.removeEventListener = function (type, fn) {
        if (this._listeners[type]) this._listeners[type] = this._listeners[type].filter(function (f) { return f !== fn; });
    };
    MockEventSource.prototype.close = function () {
        this.readyState = 2;
        channel.removeEventListener('message', this._handler);
    };
    MockEventSource.CONNECTING = 0;
    MockEventSource.OPEN = 1;
    MockEventSource.CLOSED = 2;

    window.EventSource = MockEventSource;

    // Clear stale state on load so fresh default fleet is used
    localStorage.removeItem(STORAGE_KEY);

    console.log('%c[H8 Demo Bridge] Active — all API calls mocked locally', 'color: #10b981; font-weight: bold;');
})();
