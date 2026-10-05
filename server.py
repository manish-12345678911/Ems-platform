import http.server
import socketserver
import urllib.request
import urllib.error
import sys

PORT = 8088
DIRECTORY = r"C:\ambulance\web"
BACKEND_HOST = "http://localhost:8080"

# All API path prefixes that should be proxied to the Spring Cloud Gateway
API_PREFIXES = (
    "/dispatch", "/incidents", "/hospitals", "/redeployment",
    "/audit", "/coverage", "/redeploy", "/metrics/summary",
    "/tracking", "/units", "/eta", "/actuator"
)

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
            self.send_response(502)
            msg = f'{{"error": "Backend gateway unreachable", "details": "{str(e)}"}}'.encode('utf-8')
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg)

    def do_GET(self):
        if self._is_api_path():
            # SSE endpoints need special handling (streaming)
            if '/alerts' in self.path and 'text/event-stream' in self.headers.get('Accept', ''):
                self.do_proxy_sse()
            else:
                self.do_proxy()
        else:
            super().do_GET()

    def do_POST(self):
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
