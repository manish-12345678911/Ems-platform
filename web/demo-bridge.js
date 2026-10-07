/**
 * EMS Demo Bridge — Local mock API layer
 * Intercepts fetch() calls and provides working mock responses.
 * Uses BroadcastChannel + localStorage for cross-tab sync.
 * No backend required.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'h8_demo_state';
    var CHANNEL_NAME = 'h8_demo_sync';
    var CLOUD_TOPIC = 'h8_ems_fleet_sync_manish_2026';
    var CLOUD_URL = 'https://ntfy.sh/' + CLOUD_TOPIC;
    var TUNNEL_SYNC_URL = 'https://stable-apparatus-catalog-virtue.trycloudflare.com/api/fleet/sync';
    var TUNNEL_ACCOUNTS_URL = 'https://stable-apparatus-catalog-virtue.trycloudflare.com/api/accounts/sync';
    var SUPABASE_URL = 'https://yvfonejqdqbronpdlkhy.supabase.co';
    var SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl2Zm9uZWpxZHFicm9ucGRsa2h5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyNTgzOTgsImV4cCI6MjEwNjgzNDM5OH0.8JqKbgXE8Vbms3ye0_TnNSVLpRqpJHPW_GYF2HnCQ2A';

    // BroadcastChannel for cross-tab sync
    var channel;
    try { channel = new BroadcastChannel(CHANNEL_NAME); } catch (e) { channel = { postMessage: function () { }, close: function () { }, addEventListener: function () { }, removeEventListener: function () { } }; }

    // Broadcast new user/ambulance registration across all devices globally & locally
    function broadcastNewAccount(account, unit) {
        if (!account || !account.callSign) return;
        var payload = {
            action: 'account_registered',
            type: 'account_registered',
            account: account,
            unit: unit,
            callSign: account.callSign,
            timestamp: Date.now()
        };

        try { channel.postMessage({ type: 'account_registered', payload: payload }); } catch (e) { }

        // 1. Supabase Cloud Database Sync (Global Multi-Device Auth)
        if (SUPABASE_URL && SUPABASE_ANON_KEY) {
            try {
                originalFetch(SUPABASE_URL + '/rest/v1/crew_accounts?on_conflict=call_sign', {
                    method: 'POST',
                    headers: {
                        'apikey': SUPABASE_ANON_KEY,
                        'Authorization': 'Bearer ' + SUPABASE_ANON_KEY,
                        'Content-Type': 'application/json',
                        'Prefer': 'resolution=merge-duplicates'
                    },
                    body: JSON.stringify({
                        call_sign: account.callSign,
                        username: account.username || account.callSign.toLowerCase(),
                        password: account.password || 'crew123',
                        type: account.type || 'ALS',
                        label: account.label || 'Tactical Unit',
                        lat: account.lat != null ? parseFloat(account.lat) : (unit && unit.lat != null ? parseFloat(unit.lat) : 26.9150),
                        lon: account.lon != null ? parseFloat(account.lon) : (unit && unit.lon != null ? parseFloat(unit.lon) : 75.8100)
                    })
                }).catch(function () { });
            } catch (e) { }
        }

        // 2. Local & Cloudflare Tunnel accounts sync
        try {
            originalFetch('/api/accounts/sync', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(account)
            }).catch(function () { });
        } catch (e) { }

        try {
            originalFetch(TUNNEL_ACCOUNTS_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(account)
            }).catch(function () { });
        } catch (e) { }

        // 3. Global Cloud Pub/Sub relay
        try {
            originalFetch(CLOUD_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).catch(function () { });
        } catch (e) { }

        if (unit) {
            broadcastUnitChange(unit);
        }
    }

    // Broadcast ambulance updates across all devices globally & locally
    function broadcastUnitChange(unit) {
        if (!unit || !unit.callSign) return;
        var payload = {
            action: 'unit_update',
            type: 'unit_update',
            unitId: unit.unitId || unit.id,
            callSign: unit.callSign,
            lat: unit.lat,
            lon: unit.lon,
            capability: unit.type || 'ALS',
            unitType: unit.type || 'ALS',
            status: unit.status,
            loggedIn: unit.loggedIn,
            timestamp: Date.now()
        };

        // 1. Cross-tab sync on same machine
        try { channel.postMessage({ type: 'state_changed', unit: payload }); } catch (e) { }

        // 2. Supabase Cloud Database Realtime GPS Telemetry Update
        if (SUPABASE_URL && SUPABASE_ANON_KEY && unit.callSign) {
            try {
                originalFetch(SUPABASE_URL + '/rest/v1/ambulance_units?on_conflict=call_sign', {
                    method: 'POST',
                    headers: {
                        'apikey': SUPABASE_ANON_KEY,
                        'Authorization': 'Bearer ' + SUPABASE_ANON_KEY,
                        'Content-Type': 'application/json',
                        'Prefer': 'resolution=merge-duplicates'
                    },
                    body: JSON.stringify({
                        call_sign: unit.callSign,
                        type: unit.type || unit.capability || 'ALS',
                        status: unit.status || 'AVAILABLE',
                        lat: unit.lat != null ? parseFloat(unit.lat) : 26.9150,
                        lon: unit.lon != null ? parseFloat(unit.lon) : 75.8100,
                        logged_in: unit.loggedIn !== false
                    })
                }).catch(function () { });
            } catch (e) { }
        }

        // 3. Local Python server sync (if server.py running)
        try {
            originalFetch('/api/fleet/sync', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).catch(function () { });
        } catch (e) { }

        // 4. Live Tunnel Hub (works globally across GitHub Pages and all networks)
        try {
            originalFetch(TUNNEL_SYNC_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).catch(function () { });
        } catch (e) { }

        // 5. Global Cloud Pub/Sub Relay (ntfy.sh)
        try {
            originalFetch(CLOUD_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).catch(function () { });
        } catch (e) { }
    }

    // Apply unit update received from remote phone / desktop
    function applyRemoteAccount(acc) {
        if (!acc || !acc.callSign) return;
        var state = getState();
        var cs = acc.callSign.toUpperCase();
        if (!state.crewAccounts) state.crewAccounts = JSON.parse(JSON.stringify(DEFAULT_CREW_ACCOUNTS));
        var exists = state.crewAccounts.find(function (a) { return a.callSign.toUpperCase() === cs; });
        if (!exists) {
            state.crewAccounts.push(acc);
            setState(state);
            try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
        }
    }

    function applyRemoteUnit(msg) {
        if (!msg || !msg.callSign) return;
        var state = getState();
        var cs = msg.callSign.toUpperCase();
        var local = state.fleet.find(function (f) {
            return (f.callSign && f.callSign.toUpperCase() === cs) || f.unitId === msg.unitId || f.id === msg.unitId;
        });

        var cap = msg.capability || msg.unitType || (msg.type !== 'unit_update' ? msg.type : null);

        if (local) {
            var hasChange = false;
            if (msg.lat != null && !isNaN(msg.lat) && local.lat !== parseFloat(msg.lat)) { local.lat = parseFloat(msg.lat); hasChange = true; }
            if (msg.lon != null && !isNaN(msg.lon) && local.lon !== parseFloat(msg.lon)) { local.lon = parseFloat(msg.lon); hasChange = true; }
            if (msg.status && local.status !== msg.status) { local.status = msg.status; hasChange = true; }
            if (msg.loggedIn != null && local.loggedIn !== msg.loggedIn) { local.loggedIn = msg.loggedIn; hasChange = true; }
            if (cap && local.type !== cap) { local.type = cap; hasChange = true; }
            if (hasChange) {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                try { channel.postMessage({ type: 'state_changed', unit: local }); } catch (e) { }
                try { window.dispatchEvent(new CustomEvent('h8_state_changed', { detail: local })); } catch (e) { }
            }
        } else {
            var newUnit = {
                id: msg.unitId || makeId(),
                unitId: msg.unitId || makeId(),
                callSign: msg.callSign,
                lat: msg.lat != null ? parseFloat(msg.lat) : 26.9150,
                lon: msg.lon != null ? parseFloat(msg.lon) : 75.8100,
                type: cap || 'ALS',
                status: msg.status || 'AVAILABLE',
                loggedIn: msg.loggedIn !== false
            };
            state.fleet.push(newUnit);
            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
            try { channel.postMessage({ type: 'state_changed', unit: newUnit }); } catch (e) { }
            try { window.dispatchEvent(new CustomEvent('h8_state_changed', { detail: newUnit })); } catch (e) { }
        }
    }

    // Initialize Global SSE Cloud Listener + Bulletproof HTTP Poller + Server.py Poller
    function initGlobalSync() {
        var RealES = OrigES || window.EventSource;

        function handleIncomingMessage(raw) {
            try {
                if (!raw) return;
                var data = typeof raw === 'string' ? JSON.parse(raw) : raw;
                var msg = data.message ? (typeof data.message === 'string' ? JSON.parse(data.message) : data.message) : data;
                if (!msg) return;
                if (msg.action === 'account_registered' || msg.account) {
                    if (msg.account) applyRemoteAccount(msg.account);
                    if (msg.unit) applyRemoteUnit(msg.unit);
                } else if (msg.callSign || msg.type === 'unit_update' || msg.action === 'unit_update') {
                    applyRemoteUnit(msg);
                }
            } catch (e) { }
        }

        // 1. Native SSE Stream Listener
        try {
            if (typeof RealES === 'function') {
                var sse = new RealES(CLOUD_URL + '/sse?since=10m');
                sse.onmessage = function (ev) {
                    handleIncomingMessage(ev.data);
                };
            }
        } catch (err) { }

        // 2. High-speed HTTP Long-Polling Fallback (1.5s interval)
        // Works 100% reliably across all mobile browsers, 4G/5G, and proxies
        setInterval(function () {
            try {
                originalFetch(CLOUD_URL + '/json?poll=1&since=20s')
                    .then(function (res) { return res.ok ? res.text() : ''; })
                    .then(function (text) {
                        if (!text) return;
                        var lines = text.trim().split('\n');
                        for (var i = 0; i < lines.length; i++) {
                            handleIncomingMessage(lines[i]);
                        }
                    })
                    .catch(function () { });
            } catch (e) { }
        }, 1500);

        // 3. Supabase Cloud Database Realtime Poller (1.5s interval)
        // Fetches live GPS telemetry & status directly from Supabase Postgres 24/7 globally
        setInterval(function () {
            if (!SUPABASE_URL || !SUPABASE_ANON_KEY) return;
            try {
                originalFetch(SUPABASE_URL + '/rest/v1/ambulance_units?select=*', {
                    headers: {
                        'apikey': SUPABASE_ANON_KEY,
                        'Authorization': 'Bearer ' + SUPABASE_ANON_KEY
                    }
                })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (dbUnits) {
                    if (Array.isArray(dbUnits) && dbUnits.length > 0) {
                        var state = getState();
                        var changed = false;
                        dbUnits.forEach(function (ru) {
                            if (!ru.call_sign) return;
                            var cs = ru.call_sign.toUpperCase();
                            var local = state.fleet.find(function (f) {
                                return f.callSign && f.callSign.toUpperCase() === cs;
                            });
                            if (local) {
                                if (ru.status && local.status !== ru.status) {
                                    local.status = ru.status;
                                    changed = true;
                                }
                                if (ru.logged_in != null && local.loggedIn !== ru.logged_in) {
                                    local.loggedIn = ru.logged_in;
                                    changed = true;
                                }
                                if (ru.lat != null && ru.lon != null && (local.lat !== ru.lat || local.lon !== ru.lon)) {
                                    local.lat = ru.lat;
                                    local.lon = ru.lon;
                                    changed = true;
                                }
                            } else {
                                state.fleet.push({
                                    id: ru.id || makeId(),
                                    unitId: ru.id || makeId(),
                                    callSign: ru.call_sign,
                                    lat: ru.lat,
                                    lon: ru.lon,
                                    type: ru.type || 'ALS',
                                    status: ru.status || 'AVAILABLE',
                                    loggedIn: ru.logged_in !== false
                                });
                                changed = true;
                            }
                        });
                        if (changed) {
                            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                            try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
                            try { window.dispatchEvent(new CustomEvent('h8_state_changed')); } catch (e) { }
                        }
                    }
                })
                .catch(function () { });
            } catch (e) { }
        }, 1500);

        // 4. Supabase Cloud Crew Accounts Sync (3s interval)
        // Allows any registered crew member on any phone in the world to sign in immediately
        setInterval(function () {
            if (!SUPABASE_URL || !SUPABASE_ANON_KEY) return;
            try {
                originalFetch(SUPABASE_URL + '/rest/v1/crew_accounts?select=*', {
                    headers: {
                        'apikey': SUPABASE_ANON_KEY,
                        'Authorization': 'Bearer ' + SUPABASE_ANON_KEY
                    }
                })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (dbAccounts) {
                    if (Array.isArray(dbAccounts) && dbAccounts.length > 0) {
                        var state = getState();
                        if (!state.crewAccounts) state.crewAccounts = JSON.parse(JSON.stringify(DEFAULT_CREW_ACCOUNTS));
                        var changed = false;
                        dbAccounts.forEach(function (dba) {
                            if (!dba.call_sign) return;
                            var cs = dba.call_sign.toUpperCase();
                            var found = state.crewAccounts.find(function (a) {
                                return a.callSign && a.callSign.toUpperCase() === cs;
                            });
                            if (!found) {
                                state.crewAccounts.push({
                                    callSign: dba.call_sign,
                                    username: dba.username || dba.call_sign.toLowerCase(),
                                    password: dba.password || 'crew123',
                                    unitId: dba.unit_id || makeId(),
                                    type: dba.type || 'ALS',
                                    label: dba.label || 'Tactical Unit',
                                    lat: dba.lat,
                                    lon: dba.lon
                                });
                                changed = true;
                            }
                        });
                        if (changed) {
                            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                        }
                    }
                })
                .catch(function () { });
            } catch (e) { }
        }, 3000);

        // Immediate Supabase sync on load
        setTimeout(function () {
            if (!SUPABASE_URL || !SUPABASE_ANON_KEY) return;
            try {
                originalFetch(SUPABASE_URL + '/rest/v1/ambulance_units?select=*', {
                    headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': 'Bearer ' + SUPABASE_ANON_KEY }
                }).then(function (r) { return r.ok ? r.json() : null; })
                .then(function (dbUnits) {
                    if (Array.isArray(dbUnits) && dbUnits.length > 0) {
                        var state = getState();
                        var changed = false;
                        dbUnits.forEach(function (ru) {
                            if (!ru.call_sign) return;
                            var cs = ru.call_sign.toUpperCase();
                            var local = state.fleet.find(function (f) { return f.callSign && f.callSign.toUpperCase() === cs; });
                            if (local) {
                                if (ru.status && local.status !== ru.status) { local.status = ru.status; changed = true; }
                                if (ru.logged_in != null && local.loggedIn !== ru.logged_in) { local.loggedIn = ru.logged_in; changed = true; }
                                if (ru.lat != null && ru.lon != null && (local.lat !== ru.lat || local.lon !== ru.lon)) { local.lat = ru.lat; local.lon = ru.lon; changed = true; }
                            } else {
                                state.fleet.push({
                                    id: ru.id || makeId(),
                                    unitId: ru.id || makeId(),
                                    callSign: ru.call_sign,
                                    lat: ru.lat,
                                    lon: ru.lon,
                                    type: ru.type || 'ALS',
                                    status: ru.status || 'AVAILABLE',
                                    loggedIn: ru.logged_in !== false
                                });
                                changed = true;
                            }
                        });
                        if (changed) {
                            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                            try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
                            try { window.dispatchEvent(new CustomEvent('h8_state_changed')); } catch (e) { }
                        }
                    }
                }).catch(function () { });
            } catch (e) { }
        }, 200);

        // 5. Local server.py & Cloudflare Tunnel poller (Fleet + Accounts fallback)
        setInterval(function () {
            var syncUrls = ['/api/fleet/sync'];
            var accUrls = ['/api/accounts/sync'];
            if (window.location.hostname.indexOf('github.io') !== -1 || window.location.protocol === 'https:') {
                syncUrls.push(TUNNEL_SYNC_URL);
                accUrls.push(TUNNEL_ACCOUNTS_URL);
            }
            syncUrls.forEach(function (syncUrl) {
                try {
                    originalFetch(syncUrl)
                        .then(function (res) { return res.ok ? res.json() : null; })
                        .then(function (remoteFleet) {
                            if (Array.isArray(remoteFleet) && remoteFleet.length > 0) {
                                var state = getState();
                                var changed = false;
                                remoteFleet.forEach(function (ru) {
                                    if (!ru.callSign) return;
                                    var local = state.fleet.find(function (f) {
                                        return f.callSign.toUpperCase() === ru.callSign.toUpperCase();
                                    });
                                    if (local) {
                                        if (ru.loggedIn && local.status !== ru.status) {
                                            local.status = ru.status;
                                            local.loggedIn = ru.loggedIn;
                                            changed = true;
                                        }
                                        if (ru.lat != null && ru.lon != null && (local.lat !== ru.lat || local.lon !== ru.lon)) {
                                            local.lat = ru.lat;
                                            local.lon = ru.lon;
                                            changed = true;
                                        }
                                    } else {
                                        state.fleet.push(ru);
                                        changed = true;
                                    }
                                });
                                if (changed) {
                                    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                                    try { channel.postMessage({ type: 'state_changed' }); } catch (e) { }
                                    try { window.dispatchEvent(new CustomEvent('h8_state_changed')); } catch (e) { }
                                }
                            }
                        })
                        .catch(function () { });
                } catch (e) { }
            });

            accUrls.forEach(function (accUrl) {
                try {
                    originalFetch(accUrl)
                        .then(function (res) { return res.ok ? res.json() : null; })
                        .then(function (remoteAccounts) {
                            if (Array.isArray(remoteAccounts) && remoteAccounts.length > 0) {
                                var state = getState();
                                if (!state.crewAccounts) state.crewAccounts = JSON.parse(JSON.stringify(DEFAULT_CREW_ACCOUNTS));
                                var changed = false;
                                remoteAccounts.forEach(function (ra) {
                                    if (!ra.callSign) return;
                                    var found = state.crewAccounts.find(function (a) { return a.callSign.toUpperCase() === ra.callSign.toUpperCase(); });
                                    if (!found) {
                                        state.crewAccounts.push(ra);
                                        changed = true;
                                    }
                                });
                                if (changed) {
                                    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
                                }
                            }
                        })
                        .catch(function () { });
                } catch (e) { }
            });
        }, 1500);
    }

    function makeId() {
        try { return crypto.randomUUID(); } catch (e) {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
                var r = Math.random() * 16 | 0;
                return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            });
        }
    }

    var DEFAULT_CREW_ACCOUNTS = [
        { username: 'amb-01', callSign: 'AMB-01', password: 'crew123', unitId: 'aaaaaaaa-1111-1111-1111-111111111111', type: 'ALS', label: 'Mobile ICU', lat: 26.9150, lon: 75.8100 },
        { username: 'amb-02', callSign: 'AMB-02', password: 'crew123', unitId: 'bbbbbbbb-2222-2222-2222-222222222222', type: 'BLS', label: 'Basic Tactical', lat: 26.9239, lon: 75.8267 },
        { username: 'amb-03', callSign: 'AMB-03', password: 'crew123', unitId: 'cccccccc-3333-3333-3333-333333333333', type: 'ALS', label: 'Trauma Unit', lat: 26.8988, lon: 75.8164 },
        { username: 'amb-04', callSign: 'AMB-04', password: 'crew123', unitId: 'dddddddd-4444-4444-4444-444444444444', type: 'BLS', label: 'Basic Tactical', lat: 26.9073, lon: 75.7925 },
        { username: 'amb-05', callSign: 'AMB-05', password: 'crew123', unitId: '55555555-0005-0005-0005-000000000005', type: 'ALS', label: 'Paramedic ALS', lat: 26.8524, lon: 75.8054 },
        { username: 'amb-06', callSign: 'AMB-06', password: 'crew123', unitId: '66666666-0006-0006-0006-000000000006', type: 'BLS', label: 'Basic Tactical', lat: 26.8512, lon: 75.7892 },
        { username: 'amb-07', callSign: 'AMB-07', password: 'crew123', unitId: '77777777-0007-0007-0007-000000000007', type: 'ALS', label: 'Paramedic ALS', lat: 26.8623, lon: 75.7584 },
        { username: 'amb-08', callSign: 'AMB-08', password: 'crew123', unitId: '88888888-0008-0008-0008-000000000008', type: 'BLS', label: 'Basic Tactical', lat: 26.9077, lon: 75.7397 },
        { username: 'amb-09', callSign: 'AMB-09', password: 'crew123', unitId: '99999999-0009-0009-0009-000000000009', type: 'ALS', label: 'Paramedic ALS', lat: 26.8973, lon: 75.8260 },
        { username: 'amb-10', callSign: 'AMB-10', password: 'crew123', unitId: 'aaaaaaaa-0010-0010-0010-000000000010', type: 'BLS', label: 'Basic Tactical', lat: 26.9452, lon: 75.7337 },
        { username: 'amb-11', callSign: 'AMB-11', password: 'crew123', unitId: 'bbbbbbbb-0011-0011-0011-000000000011', type: 'ALS', label: 'Paramedic ALS', lat: 26.9734, lon: 75.7766 },
        { username: 'amb-12', callSign: 'AMB-12', password: 'crew123', unitId: 'cccccccc-0012-0012-0012-000000000012', type: 'BLS', label: 'Basic Tactical', lat: 26.9050, lon: 75.7780 },
        { username: 'amb-13', callSign: 'AMB-13', password: 'crew123', unitId: 'dddddddd-0013-0013-0013-000000000013', type: 'ALS', label: 'Paramedic ALS', lat: 26.8285, lon: 75.8522 },
        { username: 'amb-14', callSign: 'AMB-14', password: 'crew123', unitId: 'eeeeeeee-0014-0014-0014-000000000014', type: 'ALS', label: 'Paramedic ALS', lat: 26.7788, lon: 75.8277 }
    ];

    function findCrewAccount(state, query) {
        if (!query) return null;
        var q = query.trim().toLowerCase().replace(/[^a-z0-9]/g, '');
        var accounts = (state && state.crewAccounts && state.crewAccounts.length > 0) ? state.crewAccounts : DEFAULT_CREW_ACCOUNTS;
        for (var i = 0; i < accounts.length; i++) {
            var a = accounts[i];
            var un = a.username.toLowerCase().replace(/[^a-z0-9]/g, '');
            var cs = a.callSign.toLowerCase().replace(/[^a-z0-9]/g, '');
            if (un === q || cs === q || ('amb' + q) === cs || cs === ('amb0' + q) || cs === ('amb' + q.padStart(2, '0'))) {
                return a;
            }
        }
        return null;
    }

    function createDefaultState() {
        return {
            version: 4,
            fleet: [
                { id: 'aaaaaaaa-1111-1111-1111-111111111111', unitId: 'aaaaaaaa-1111-1111-1111-111111111111', callSign: 'AMB-01', lat: 26.9150, lon: 75.8100, type: 'ALS', status: 'OFFLINE', label: 'Mobile ICU', loggedIn: false },
                { id: 'bbbbbbbb-2222-2222-2222-222222222222', unitId: 'bbbbbbbb-2222-2222-2222-222222222222', callSign: 'AMB-02', lat: 26.9239, lon: 75.8267, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: 'cccccccc-3333-3333-3333-333333333333', unitId: 'cccccccc-3333-3333-3333-333333333333', callSign: 'AMB-03', lat: 26.8988, lon: 75.8164, type: 'ALS', status: 'OFFLINE', label: 'Trauma Unit', loggedIn: false },
                { id: 'dddddddd-4444-4444-4444-444444444444', unitId: 'dddddddd-4444-4444-4444-444444444444', callSign: 'AMB-04', lat: 26.9073, lon: 75.7925, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: '55555555-0005-0005-0005-000000000005', unitId: '55555555-0005-0005-0005-000000000005', callSign: 'AMB-05', lat: 26.8524, lon: 75.8054, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false },
                { id: '66666666-0006-0006-0006-000000000006', unitId: '66666666-0006-0006-0006-000000000006', callSign: 'AMB-06', lat: 26.8512, lon: 75.7892, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: '77777777-0007-0007-0007-000000000007', unitId: '77777777-0007-0007-0007-000000000007', callSign: 'AMB-07', lat: 26.8623, lon: 75.7584, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false },
                { id: '88888888-0008-0008-0008-000000000008', unitId: '88888888-0008-0008-0008-000000000008', callSign: 'AMB-08', lat: 26.9077, lon: 75.7397, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: '99999999-0009-0009-0009-000000000009', unitId: '99999999-0009-0009-0009-000000000009', callSign: 'AMB-09', lat: 26.8973, lon: 75.8260, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false },
                { id: 'aaaaaaaa-0010-0010-0010-000000000010', unitId: 'aaaaaaaa-0010-0010-0010-000000000010', callSign: 'AMB-10', lat: 26.9452, lon: 75.7337, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: 'bbbbbbbb-0011-0011-0011-000000000011', unitId: 'bbbbbbbb-0011-0011-0011-000000000011', callSign: 'AMB-11', lat: 26.9734, lon: 75.7766, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false },
                { id: 'cccccccc-0012-0012-0012-000000000012', unitId: 'cccccccc-0012-0012-0012-000000000012', callSign: 'AMB-12', lat: 26.9050, lon: 75.7780, type: 'BLS', status: 'OFFLINE', label: 'Basic Tactical', loggedIn: false },
                { id: 'dddddddd-0013-0013-0013-000000000013', unitId: 'dddddddd-0013-0013-0013-000000000013', callSign: 'AMB-13', lat: 26.8285, lon: 75.8522, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false },
                { id: 'eeeeeeee-0014-0014-0014-000000000014', unitId: 'eeeeeeee-0014-0014-0014-000000000014', callSign: 'AMB-14', lat: 26.7788, lon: 75.8277, type: 'ALS', status: 'OFFLINE', label: 'Paramedic ALS', loggedIn: false }
            ],
            crewAccounts: JSON.parse(JSON.stringify(DEFAULT_CREW_ACCOUNTS)),
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
            if (!raw) {
                var init = createDefaultState();
                localStorage.setItem(STORAGE_KEY, JSON.stringify(init));
                return init;
            }
            var s = JSON.parse(raw);
            if (!s || !s.fleet || !s.hospitals || s.version !== 4) {
                var fresh = createDefaultState();
                localStorage.setItem(STORAGE_KEY, JSON.stringify(fresh));
                return fresh;
            }
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
                broadcastUnitChange(unit);
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
                broadcastUnitChange(unit);
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

        // GET /auth/crew/accounts
        if (method === 'GET' && pathname === '/auth/crew/accounts') {
            state = getState();
            var list = (state.crewAccounts || DEFAULT_CREW_ACCOUNTS).map(function(acc) {
                var u = state.fleet.find(function(fl) { return fl.unitId === acc.unitId || fl.callSign === acc.callSign; });
                return {
                    callSign: acc.callSign,
                    username: acc.username,
                    unitId: acc.unitId,
                    type: acc.type,
                    label: acc.label,
                    status: u ? (u.status || 'OFFLINE') : 'OFFLINE',
                    lat: u ? u.lat : acc.lat,
                    lon: u ? u.lon : acc.lon
                };
            });
            return jsonResponse(list);
        }

        // POST /auth/crew/login
        if (method === 'POST' && pathname === '/auth/crew/login') {
            state = getState();
            var username = (body.username || '').trim();
            var password = body.password || '';
            var account = findCrewAccount(state, username);

            if (!account || account.password !== password) {
                return new Response(JSON.stringify({
                    success: false,
                    message: "Invalid Call Sign or Passcode."
                }), { status: 401, headers: { 'Content-Type': 'application/json' } });
            }

            // Find or restore unit in fleet
            unit = state.fleet.find(function(u) { return u.unitId === account.unitId || u.id === account.unitId || u.callSign === account.callSign; });
            if (!unit) {
                unit = {
                    id: account.unitId,
                    unitId: account.unitId,
                    callSign: account.callSign,
                    type: account.type || 'ALS',
                    lat: account.lat || 26.9150,
                    lon: account.lon || 75.8100,
                    status: 'AVAILABLE',
                    label: account.label || 'Tactical Unit',
                    loggedIn: true
                };
                state.fleet.push(unit);
            }

            // Set coordinates if passed from device GPS
            if (body.lat != null && !isNaN(body.lat)) unit.lat = parseFloat(body.lat);
            if (body.lon != null && !isNaN(body.lon)) unit.lon = parseFloat(body.lon);

            // Set unit to AVAILABLE on duty
            unit.status = 'AVAILABLE';
            unit.loggedIn = true;
            unit.loggedInAt = new Date().toISOString();
            setState(state);
            broadcastUnitChange(unit);

            try {
                channel.postMessage({ type: 'state_changed' });
                channel.postMessage({
                    type: 'unit_online',
                    unitId: unit.unitId || unit.id,
                    callSign: unit.callSign,
                    lat: unit.lat,
                    lon: unit.lon,
                    type: unit.type,
                    status: 'AVAILABLE'
                });
            } catch(e) {}

            return jsonResponse({
                success: true,
                token: 'h8-crew-token-' + btoa(account.callSign + ':' + Date.now()),
                unitId: unit.unitId || unit.id,
                callSign: unit.callSign,
                type: unit.type,
                label: account.label || unit.label,
                lat: unit.lat,
                lon: unit.lon,
                status: 'AVAILABLE',
                message: 'Ambulance ' + unit.callSign + ' authenticated & on-duty.'
            });
        }

        // POST /auth/crew/register
        if (method === 'POST' && pathname === '/auth/crew/register') {
            state = getState();
            var rawCallSign = (body.callSign || '').trim().toUpperCase();
            var type = (body.type || 'ALS').toUpperCase();
            var pwd = body.password || '';
            var label = body.label || (type === 'ALS' ? 'Paramedic ALS' : 'Basic Tactical');
            var lat = (body.lat != null && !isNaN(body.lat)) ? parseFloat(body.lat) : 26.9124;
            var lon = (body.lon != null && !isNaN(body.lon)) ? parseFloat(body.lon) : 75.7873;

            if (!rawCallSign) {
                return new Response(JSON.stringify({ success: false, message: 'Call Sign is required (e.g. AMB-15)' }), { status: 400, headers: { 'Content-Type': 'application/json' } });
            }
            if (!pwd || pwd.length < 4) {
                return new Response(JSON.stringify({ success: false, message: 'Passcode must be at least 4 characters.' }), { status: 400, headers: { 'Content-Type': 'application/json' } });
            }

            var callSign = rawCallSign.startsWith('AMB-') ? rawCallSign : ('AMB-' + rawCallSign.replace(/[^0-9A-Z]/g, ''));
            var existingAcc = findCrewAccount(state, callSign);
            if (existingAcc) {
                return new Response(JSON.stringify({ success: false, message: 'Unit ' + callSign + ' is already registered. Please sign in.' }), { status: 409, headers: { 'Content-Type': 'application/json' } });
            }

            var newUnitId = makeId();
            var newAccount = {
                username: callSign.toLowerCase(),
                callSign: callSign,
                password: pwd,
                unitId: newUnitId,
                type: type,
                label: label,
                lat: lat,
                lon: lon
            };
            if (!state.crewAccounts) state.crewAccounts = JSON.parse(JSON.stringify(DEFAULT_CREW_ACCOUNTS));
            state.crewAccounts.push(newAccount);

            var newUnit = {
                id: newUnitId,
                unitId: newUnitId,
                callSign: callSign,
                type: type,
                lat: lat,
                lon: lon,
                status: 'AVAILABLE',
                label: label,
                loggedIn: true,
                loggedInAt: new Date().toISOString()
            };
            state.fleet.push(newUnit);
            setState(state);
            broadcastNewAccount(newAccount, newUnit);

            try {
                channel.postMessage({ type: 'state_changed' });
                channel.postMessage({
                    type: 'unit_online',
                    unitId: newUnitId,
                    callSign: callSign,
                    lat: lat,
                    lon: lon,
                    type: type,
                    status: 'AVAILABLE'
                });
            } catch(e) {}

            return jsonResponse({
                success: true,
                token: 'h8-crew-token-' + btoa(callSign + ':' + Date.now()),
                unitId: newUnitId,
                callSign: callSign,
                type: type,
                label: label,
                lat: lat,
                lon: lon,
                status: 'AVAILABLE',
                message: 'Unit ' + callSign + ' successfully registered and on-duty.'
            });
        }

        // POST /auth/crew/logout
        if (method === 'POST' && (pathname === '/auth/crew/logout' || (pathname.indexOf('/dispatch/units/') === 0 && pathname.indexOf('/logout') > 0))) {
            state = getState();
            unitId = (body && body.unitId) || pathname.split('/')[3];
            unit = state.fleet.find(function(u) {
                return (unitId && (u.id === unitId || u.unitId === unitId)) ||
                       (body && body.callSign && u.callSign && u.callSign.toUpperCase() === body.callSign.toUpperCase());
            });
            if (unit) {
                unit.status = 'OFFLINE';
                unit.loggedIn = false;
                delete unit.assignedIncident;
                setState(state);
                broadcastUnitChange(unit);
                try {
                    channel.postMessage({ type: 'state_changed' });
                    channel.postMessage({ type: 'unit_offline', unitId: unit.unitId || unit.id, callSign: unit.callSign });
                } catch(e) {}
            }
            return jsonResponse({ success: true, message: 'Unit taken off-duty successfully.' });
        }

        // POST /auth/login
        if (method === 'POST' && pathname === '/auth/login') {
            var rawUser = (body.username || '').trim();
            var username = rawUser.toLowerCase();
            var password = body.password || '';

            if ((username === 'admin' && password === 'admin123') ||
                (username === 'dispatcher1' && password === 'test123') ||
                (username === 'supervisor1' && password === 'test123')) {
                var dispName = username === 'admin' ? 'Tactical Chief Administrator' : (username === 'dispatcher1' ? 'Senior Dispatch Controller' : 'Tactical Operations Supervisor');
                var role = username === 'admin' ? 'ADMIN' : (username === 'dispatcher1' ? 'DISPATCHER' : 'SUPERVISOR');
                return jsonResponse({
                    success: true,
                    token: 'h8-tactical-admin-token-' + btoa(username + ':' + Date.now()),
                    username: username,
                    displayName: dispName,
                    roles: [role, 'ADMIN'],
                    primaryRole: 'ADMIN',
                    expiresIn: 86400,
                    message: 'Authentication successful'
                });
            } else if ((username === 'crew1' || username === 'ednurse1' || username === 'auditor1') && password === 'test123') {
                return new Response(JSON.stringify({
                    success: false,
                    message: "Access Denied: Account '" + username + "' does not possess administrator clearance. The Dispatcher Command Center requires Administrator clearance."
                }), { status: 403, headers: { 'Content-Type': 'application/json' } });
            } else {
                // Check if it's a crew account attempting login
                state = getState();
                var crewAcc = findCrewAccount(state, rawUser);
                if (crewAcc && crewAcc.password === password) {
                    unit = state.fleet.find(function(u) { return u.unitId === crewAcc.unitId || u.id === crewAcc.unitId || u.callSign === crewAcc.callSign; });
                    if (unit) {
                        unit.status = 'AVAILABLE';
                        unit.loggedIn = true;
                        if (body.lat != null && !isNaN(body.lat)) unit.lat = parseFloat(body.lat);
                        if (body.lon != null && !isNaN(body.lon)) unit.lon = parseFloat(body.lon);
                        setState(state);
                        broadcastUnitChange(unit);
                        try {
                            channel.postMessage({ type: 'state_changed' });
                        } catch(e) {}
                    }
                    return jsonResponse({
                        success: true,
                        token: 'h8-crew-token-' + btoa(crewAcc.callSign + ':' + Date.now()),
                        username: crewAcc.username,
                        unitId: crewAcc.unitId,
                        callSign: crewAcc.callSign,
                        type: crewAcc.type,
                        displayName: crewAcc.callSign + ' (' + crewAcc.type + ') Crew',
                        roles: ['CREW'],
                        primaryRole: 'CREW',
                        expiresIn: 86400,
                        message: 'Crew authentication successful'
                    });
                }

                return new Response(JSON.stringify({
                    success: false,
                    message: "Invalid username or password."
                }), { status: 401, headers: { 'Content-Type': 'application/json' } });
            }
        }

        // GET /auth/verify
        if (method === 'GET' && pathname === '/auth/verify') {
            return jsonResponse({
                success: true,
                username: 'admin',
                displayName: 'Tactical Chief Administrator',
                roles: ['ADMIN', 'DISPATCHER'],
                primaryRole: 'ADMIN',
                expiresIn: 86400,
                message: 'Token verified'
            });
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
        // If url is external HTTP/HTTPS (like ntfy.sh cloud relay), delegate directly to native EventSource!
        if (url && (url.indexOf('http://') === 0 || url.indexOf('https://') === 0) && OrigES) {
            return new OrigES(url);
        }
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

    // Start real-time global cloud relay and local server sync
    initGlobalSync();

    console.log('%c[EMS Demo Bridge] Active — real-time global fleet sync running', 'color: #10b981; font-weight: bold;');
})();
