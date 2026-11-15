#!/usr/bin/env python3
"""
Local openHAB dummy server for testing the app without a real openHAB server.

Emulates the endpoints used by OpenHabClient.kt:
  - POST /rest/voice/interpreters?conversation=<id>   (openHAB 5 chat, plain-text answer)
  - GET  /rest/voice/conversations/<id>               (history as JSON with messages[])
  - DELETE /rest/voice/conversations/<id>             (delete history)
  - POST /rest/habot/chat                             (plain text in -> {"answer": ...})
  - POST /rest/voice/interpret?text=...               (voice interpreter, no answer)
  - POST /rest/chat (custom mode)                     ({"message":...} -> {"answer":...})

Start:  python3 tools/dummy_server.py [port]
App setting "Server URL" in the emulator: http://10.0.2.2:8080
(10.0.2.2 = the host machine's localhost as seen from the Android emulator)
"""
import json
import re
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

# --------------------------------------------------------------------------
# "Intelligence" of the dummy: simple patterns mapped to plausible openHAB answers
# --------------------------------------------------------------------------
RULES = [
    (r"(?i)\bhallo\b|guten (tag|morgen|abend)", "Hallo! Wie kann ich helfen? 🤖"),
    (r"(?i)\blicht.*an\b|\bmach.*an\b", "✅ Wohnzimmer-Licht wurde eingeschaltet."),
    (r"(?i)\blicht.*aus\b|\bmach.*aus\b", "✅ Wohnzimmer-Licht wurde ausgeschaltet."),
    (r"(?i)\btemperatur\b|\bwie warm\b", "Die Temperatur im Wohnzimmer beträgt **21,5 °C**."),
    (r"(?i)\bstatus\b", "Alle Geräte arbeiten normal. Keine Alarme."),
    (r"(?i)\brolle|jalousie|rolladen", "✅ Die Rolläden wurden gefahren."),
]


def generate_answer(text):
    for pattern, answer in RULES:
        if re.search(pattern, text):
            return answer
    return (
        f"Dummy-Antwort zu: „{text}“\n\n"
        "Dies ist ein **lokal simulierter openHAB-Server**. "
        "In der echten Umgebung würde hier die LLM-Antwort stehen – "
        "inklusive *Markdown*, `Code` und Listen:"
        "\n\n- Licht schalten\n- Temperatur abfragen\n- Szenen starten"
    )

# --------------------------------------------------------------------------
# HTTP-Handler
# --------------------------------------------------------------------------
class OpenHabDummyHandler(BaseHTTPRequestHandler):
    # conversations: {conversation_id: [ {id, role, content, timestamp} ]}
    conversations = {}
    next_msg_id = 1

    def log_message(self, fmt, *args):
        print(f"[dummy] {self.address_string()} {fmt % args}")

    # ---------- Helper functions ----------
    def _send(self, code: int, body: str, ctype: str = "text/plain; charset=utf-8"):
        data = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _send_json(self, code: int, obj):
        self._send(code, json.dumps(obj, ensure_ascii=False, indent=2),
                   "application/json; charset=utf-8")

    def _read_body(self) -> str:
        length = int(self.headers.get("Content-Length", "0") or 0)
        if length:
            return self.rfile.read(length).decode("utf-8", errors="replace")
        return ""

    def _add_message(self, conv_id: str, role: str, content: str) -> int:
        cls = OpenHabDummyHandler
        msg = {
            "id": cls.next_msg_id,
            "role": role,
            "content": content,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        }
        cls.next_msg_id += 1
        cls.conversations.setdefault(conv_id, []).append(msg)
        return msg["id"]

    # ---------- Routes ----------
    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        qs = parse_qs(parsed.query)
        body = self._read_body()

        if path == "/rest/voice/interpreters":
            conv_id = (qs.get("conversation", ["default"])[0]).strip() or "default"
            text = body.strip()
            self._add_message(conv_id, "USER", text)
            answer = generate_answer(text)
            self._add_message(conv_id, "OPENHAB", answer)
            # Plain-text answer (as OpenHabClient expects it for OPENHAB5_CHAT)
            self._send(200, answer)

        elif path == "/rest/habot/chat":
            answer = generate_answer(body.strip())
            self._send_json(200, {"answer": answer, "language": "de", "skillName": "dummy"})

        elif path == "/rest/voice/interpret":
            text = qs.get("text", [""])[0]
            print(f"[dummy] Voice-Interpreter-Befehl: {text!r}")
            # Voice mode: no verbal answer expected
            self._send(200, "")

        elif path == "/rest/chat":
            # Custom mode: {"message": "..."} -> {"answer": "..."}
            try:
                payload = json.loads(body)
            except json.JSONDecodeError:
                payload = {}
            message = payload.get("message", "")
            self._send_json(200, {"answer": generate_answer(message)})

        else:
            self._send(404, f"Dummy-Server: unbekannter Pfad {path}")

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        match = re.fullmatch(r"/rest/voice/conversations/([^/]+)", path)
        if match:
            conv_id = match.group(1)
            messages = OpenHabDummyHandler.conversations.get(conv_id, [])
            self._send_json(200, {
                "conversationId": conv_id,
                "messages": messages,
                "lastModified": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            })
        elif path == "/rest":
            # Info endpoint as known from real openHAB
            self._send_json(200, {"version": "5.0.0-dummy", "locale": "de-DE"})
        elif path == "/":
            self._send(200, "openHAB-Dummy läuft. Endpoints:\n"
                            "  POST /rest/voice/interpreters?conversation=<id>\n"
                            "  GET  /rest/voice/conversations/<id>\n"
                            "  DELETE /rest/voice/conversations/<id>\n"
                            "  POST /rest/habot/chat\n"
                            "  POST /rest/voice/interpret?text=...\n"
                            "  POST /rest/chat  (Custom-Modus)")
        else:
            self._send(404, f"Dummy-Server: unbekannter Pfad {path}")

    def do_DELETE(self):
        parsed = urlparse(self.path)
        match = re.fullmatch(r"/rest/voice/conversations/([^/]+)", parsed.path)
        if match:
            conv_id = match.group(1)
            OpenHabDummyHandler.conversations.pop(conv_id, None)
            print(f"[dummy] Verlauf {conv_id} gelöscht.")
            self._send(200, "")
        else:
            self._send(404, "Dummy-Server: unbekannter Pfad")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    server = ThreadingHTTPServer(("0.0.0.0", port), OpenHabDummyHandler)
    print(f"openHAB-Dummy lauscht auf http://0.0.0.0:{port}")
    print(f"  → aus dem Android-Emulator: http://10.0.2.2:{port}")
    print("  Strg+C beendet.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBeendet.")

if __name__ == "__main__":
    main()

