#!/usr/bin/env python3
"""Static safety and link checks for the standalone Cloudflare Pages website."""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit, unquote

ROOT = Path(__file__).resolve().parents[1] / "site"
PAGES = ("index.html", "privacy.html", "404.html")
REQUIRED = (*PAGES, "favicon.svg", "medisyncd-shield.svg", "_headers", "robots.txt", "sitemap.xml")

class Inspector(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.refs = []
        self.tags = []
        self.lang = None
        self.has_viewport = False
        self.has_h1 = False
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        self.tags.append(tag)
        if tag == "html":
            self.lang = attrs.get("lang")
        if tag == "meta" and attrs.get("name") == "viewport":
            self.has_viewport = True
        if tag == "h1":
            self.has_h1 = True
        if attrs.get("id"):
            if attrs["id"] in self.ids:
                raise AssertionError(f"Duplicate id {attrs['id']}")
            self.ids.add(attrs["id"])
        for key in ("href", "src"):
            if attrs.get(key):
                self.refs.append((tag, attrs[key]))

def check():
    errors = []
    for filename in REQUIRED:
        if not (ROOT / filename).is_file():
            errors.append(f"Missing required asset: {filename}")
    if errors:
        raise AssertionError("\n".join(errors))
    for filename in PAGES:
        path = ROOT / filename
        inspect = Inspector()
        inspect.feed(path.read_text(encoding="utf-8"))
        if inspect.lang != "en":
            errors.append(f"{filename}: missing language")
        if not inspect.has_viewport:
            errors.append(f"{filename}: missing viewport metadata")
        if not inspect.has_h1:
            errors.append(f"{filename}: missing h1")
        if "form" in inspect.tags:
            errors.append(f"{filename}: unexpected form (review privacy/handling first)")
        for tag, ref in inspect.refs:
            parsed = urlsplit(ref)
            if ref.startswith("#"):
                if ref[1:] not in inspect.ids:
                    errors.append(f"{filename}: missing anchor {ref}")
            elif ref.startswith("/") and not ref.startswith("//"):
                name = unquote(parsed.path).lstrip("/") or "index.html"
                if not (ROOT / name).is_file():
                    errors.append(f"{filename}: missing local reference {ref}")
            elif parsed.scheme in ("https", "mailto", "tel"):
                pass
            else:
                errors.append(f"{filename}: unsupported reference {ref}")
    headers = (ROOT / "_headers").read_text(encoding="utf-8")
    if "Content-Security-Policy" not in headers or "X-Content-Type-Options: nosniff" not in headers:
        errors.append("Security headers incomplete")
    if errors:
        raise AssertionError("\n".join(errors))
    print("Static checks PASS: all required assets, local links, anchors, semantics, and core security headers")

if __name__ == "__main__":
    check()
