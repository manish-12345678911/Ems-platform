import http.server
import socketserver
import urllib.request
import urllib.error
import sys

import json
import threading

PORT = 8088
DIRECTORY = r"C:\ambulance\web"
BACKEND_HOST = "http://localhost:8080"

# All API path prefixes that should be proxied to the Spring Cloud Gateway
API_PREFIXES = (
    "/dispatch", "/incidents", "/hospitals", "/redeployment",
    "/audit", "/coverage", "/redeploy", "/metrics/summary",
    "/tracking", "/units", "/eta", "/actuator", "/api/fleet"
)

SHARED_FLEET_LOCK = threading.Lock()
DEFAULT_FLEET = [
    {"id": "aaaaaaaa-1111-1111-1111-111111111111", "unitId": "aaaaaaaa-1111-1111-1111-111111111111", "callSign": "AMB-01", "lat": 26.9150, "lon": 75.8100, "type": "ALS", "status": "OFFLINE", "label": "Mobile ICU", "loggedIn": False},
    {"id": "bbbbbbbb-2222-2222-2222-222222222222", "unitId": "bbbbbbbb-2222-2222-2222-222222222222", "callSign": "AMB-02", "lat": 26.9239, "lon": 75.8267, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "cccccccc-3333-3333-3333-333333333333", "unitId": "cccccccc-3333-3333-3333-333333333333", "callSign": "AMB-03", "lat": 26.8988, "lon": 75.8164, "type": "ALS", "status": "OFFLINE", "label": "Trauma Unit", "loggedIn": False},
    {"id": "dddddddd-4444-4444-4444-444444444444", "unitId": "dddddddd-4444-4444-4444-444444444444", "callSign": "AMB-04", "lat": 26.9073, "lon": 75.7925, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "55555555-0005-0005-0005-000000000005", "unitId": "55555555-0005-0005-0005-000000000005", "callSign": "AMB-05", "lat": 26.8524, "lon": 75.8054, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False},
    {"id": "66666666-0006-0006-0006-000000000006", "unitId": "66666666-0006-0006-0006-000000000006", "callSign": "AMB-06", "lat": 26.8512, "lon": 75.7892, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "77777777-0007-0007-0007-000000000007", "unitId": "77777777-0007-0007-0007-000000000007", "callSign": "AMB-07", "lat": 26.8623, "lon": 75.7584, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False},
    {"id": "88888888-0008-0008-0008-000000000008", "unitId": "88888888-0008-0008-0008-000000000008", "callSign": "AMB-08", "lat": 26.9077, "lon": 75.7397, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "99999999-0009-0009-0009-000000000009", "unitId": "99999999-0009-0009-0009-000000000009", "callSign": "AMB-09", "lat": 26.8973, "lon": 75.8260, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False},
    {"id": "aaaaaaaa-0010-0010-0010-000000000010", "unitId": "aaaaaaaa-0010-0010-0010-000000000010", "callSign": "AMB-10", "lat": 26.9452, "lon": 75.7337, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "bbbbbbbb-0011-0011-0011-000000000011", "unitId": "bbbbbbbb-0011-0011-0011-000000000011", "callSign": "AMB-11", "lat": 26.9734, "lon": 75.7766, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False},
    {"id": "cccccccc-0012-0012-0012-000000000012", "unitId": "cccccccc-0012-0012-0012-000000000012", "callSign": "AMB-12", "lat": 26.9050, "lon": 75.7780, "type": "BLS", "status": "OFFLINE", "label": "Basic Tactical", "loggedIn": False},
    {"id": "dddddddd-0013-0013-0013-000000000013", "unitId": "dddddddd-0013-0013-0013-000000000013", "callSign": "AMB-13", "lat": 26.8285, "lon": 75.8522, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False},
    {"id": "eeeeeeee-0014-0014-0014-000000000014", "unitId": "eeeeeeee-0014-0014-0014-000000000014", "callSign": "AMB-14", "lat": 26.7788, "lon": 75.8277, "type": "ALS", "status": "OFFLINE", "label": "Paramedic ALS", "loggedIn": False}
]
SHARED_FLEET = {u["callSign"].upper(): dict(u) for u in DEFAULT_FLEET}

class ProxyHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Enable CORS and disable aggressive caching so updates appear immediately
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        super().end_headers()

    def _is_api_path(self):
        clean_path = self.path.split('?')[0]
        # UI pages must never be treated as backend API
        if clean_path in ('/', '/index.html') or any(clean_path.startswith(ui) for ui in ('/dispatcher', '/crew', '/ed')):
            return False
        for prefix in API_PREFIXES:
            if clean_path == prefix or self.path.startswith(prefix + '/') or self.path.startswith(prefix + '?'):
                return True
        return False

    def do_proxy(self):
        target_url = BACKEND_HOST + self.path
        # Clean up hop-by-hop headers
        headers = {}
        for k, v in self.headers.items():
            if k.lower() not in ('host', 'content-length', 'connection', 'transfer-encoding'):
                headers[k] = v
        headers['Host'] = 'localhost:8080'

        data = None
        content_length = int(self.headers.get('Content-Length', 0))
        if content_length > 0:
            data = self.rfile.read(content_length)

        req = urllib.request.Request(target_url, data=data, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                self.send_response(resp.status)
                body = resp.read()
                # Forward content type from backend
                ct = resp.headers.get("Content-Type", "application/json")
                self.send_header("Content-Type", ct)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            body = e.read()
            self.send_header("Content-Type", e.headers.get("Content-Type", "application/json"))
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except Exception as e:
            # Fallback to in-memory shared fleet when Spring Boot backend is offline
            if self.command == 'GET' and (self.path.startswith('/dispatch/units') or self.path.startswith('/units')):
                with SHARED_FLEET_LOCK:
                    data = json.dumps(list(SHARED_FLEET.values())).encode('utf-8')
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return

            if self.command == 'POST' and '/dispatch/units/' in self.path and '/location' in self.path:
                try:
                    parts = self.path.split('?')[0].split('/')
                    u_id = parts[3]
                    q = urllib.parse.urlparse(self.path).query
                    params = urllib.parse.parse_qs(q)
                    with SHARED_FLEET_LOCK:
                        for u in SHARED_FLEET.values():
                            if u.get('unitId') == u_id or u.get('id') == u_id or u.get('callSign', '').upper() == u_id.upper():
                                if 'lat' in params: u['lat'] = float(params['lat'][0])
                                if 'lon' in params: u['lon'] = float(params['lon'][0])
                                if 'status' in params: u['status'] = params['status'][0]
                                u['loggedIn'] = True
                except Exception:
                    pass
                resp = b'{"ok": true}'
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(resp)))
                self.end_headers()
                self.wfile.write(resp)
                return

            self.send_response(502)
            msg = f'{{"error": "Backend gateway unreachable", "details": "{str(e)}"}}'.encode('utf-8')
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg)

    def do_GET(self):
        if self.path.startswith('/api/fleet/sync'):
            with SHARED_FLEET_LOCK:
                data = json.dumps(list(SHARED_FLEET.values())).encode('utf-8')
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return

        if self._is_api_path():
            # SSE endpoints need special handling (streaming)
            if '/alerts' in self.path and 'text/event-stream' in self.headers.get('Accept', ''):
                self.do_proxy_sse()
            else:
                self.do_proxy()
        else:
            super().do_GET()

    def do_POST(self):
        if self.path.startswith('/api/fleet/sync'):
            length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(length) if length > 0 else b'{}'
            try:
                payload = json.loads(body.decode('utf-8'))
                cs = (payload.get('callSign') or '').upper()
                with SHARED_FLEET_LOCK:
                    if cs in SHARED_FLEET:
                        unit = SHARED_FLEET[cs]
                        if 'lat' in payload and payload['lat'] is not None:
                            unit['lat'] = float(payload['lat'])
                        if 'lon' in payload and payload['lon'] is not None:
                            unit['lon'] = float(payload['lon'])
                        if 'status' in payload:
                            unit['status'] = payload['status']
                        if 'loggedIn' in payload:
                            unit['loggedIn'] = bool(payload['loggedIn'])
                        if 'type' in payload:
                            unit['type'] = payload['type']
                    elif cs:
                        SHARED_FLEET[cs] = payload
                resp_data = json.dumps({"ok": True}).encode('utf-8')
            except Exception as e:
                resp_data = json.dumps({"error": str(e)}).encode('utf-8')
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(resp_data)))
            self.end_headers()
            self.wfile.write(resp_data)
            return

        if self._is_api_path():
            self.do_proxy()
        else:
            self.send_response(405)
            self.end_headers()

    def do_PUT(self):
        if self._is_api_path():
            self.do_proxy()
        else:
            self.send_response(405)
            self.end_headers()

    def do_DELETE(self):
        if self._is_api_path():
            self.do_proxy()
        else:
            self.send_response(405)
            self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_proxy_sse(self):
        """Proxy Server-Sent Events (SSE) streams from the backend."""
        target_url = BACKEND_HOST + self.path
        headers = {}
        for k, v in self.headers.items():
            if k.lower() not in ('host', 'content-length', 'connection', 'transfer-encoding'):
                headers[k] = v
        headers['Host'] = 'localhost:8080'
        headers['Accept'] = 'text/event-stream'

        req = urllib.request.Request(target_url, headers=headers, method='GET')
        try:
            resp = urllib.request.urlopen(req, timeout=300)
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()

            # Stream data chunks
            while True:
                chunk = resp.read(4096)
                if not chunk:
                    break
                self.wfile.write(chunk)
                self.wfile.flush()
        except Exception as e:
            try:
                self.send_response(502)
                msg = f'{{"error": "SSE proxy failed: {str(e)}"}}'.encode('utf-8')
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(msg)))
                self.end_headers()
                self.wfile.write(msg)
            except Exception:
                pass

    def log_message(self, format, *args):
        # Color-code API vs static requests
        msg = format % args
        if any(p in msg for p in API_PREFIXES):
            sys.stderr.write(f"\033[36m[API] {msg}\033[0m\n")
        else:
            sys.stderr.write(f"[WEB] {msg}\n")

class ThreadingServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

if __name__ == '__main__':
    print(f"""
====================================================================
  H8 EMS Unified Server
  Static Web:  http://localhost:{PORT}/
  Dispatcher:  http://localhost:{PORT}/dispatcher/
  Crew PWA:    http://localhost:{PORT}/crew/
  Hospital ED: http://localhost:{PORT}/ed/
  API Proxy:   -> http://localhost:8080 (Spring Cloud Gateway)
====================================================================
""")
    sys.stdout.flush()
    with ThreadingServer(("0.0.0.0", PORT), ProxyHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down H8 server...")
            httpd.shutdown()
